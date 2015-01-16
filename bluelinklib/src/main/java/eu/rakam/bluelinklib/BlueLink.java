package eu.rakam.bluelinklib;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import eu.rakam.bluelinklib.callbacks.OnOpenServerCallback;
import eu.rakam.bluelinklib.callbacks.OnSearchForServerCallback;
import eu.rakam.bluelinklib.callbacks.OnTurnOnBluetoothCallback;

public class BlueLink {

    private static final String TAG = "BlueLinkDebug";
    private static final int ENABLE_BLUETOOTH = 22000;
    private static final int DISCOVERY_REQUEST = 22001;
    private static final int DISCOVERABLE_DURATION = 60;
    private static final int BUFFER_SIZE = 32768;
    private static final String HEADER_SPAN = "   ";
    private static final int HEADER_SIZE = HEADER_SPAN.length();

    private Activity activity;
    private String name;
    private java.util.UUID UUID;

    private BluetoothAdapter bluetooth;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private List<Server> serverList = new ArrayList<>();
    private ByteBuffer conversionBuffer = ByteBuffer.allocate(2);
    private byte[] buffer = new byte[BUFFER_SIZE];

    private OnTurnOnBluetoothCallback turnOnBluetoothCallback;
    private OnOpenServerCallback openServerCallback;
    private Thread serverThread;

    /**
     * @param name Name of the server seen by other players
     */
    public BlueLink(Activity activity, String name, String UUID) {
        this.activity = activity;
        this.name = name;
        this.UUID = java.util.UUID.fromString(UUID);
        this.bluetooth = BluetoothAdapter.getDefaultAdapter();
        registerBroadcastReceivers();
    }

    /**
     * Opens the server and waits for connections
     *
     * @param callback
     */
    public void openServer(OnOpenServerCallback callback) {
        openServerCallback = callback;
        turnOnBluetooth(new OnTurnOnBluetoothCallback() {
            @Override
            public void onBluetoothOn(IOException e) {
                if (e != null) {
                    openServerCallback.onFinished(e);
                } else {
                    try {
                        startServerSocket();
                        makeDiscoverable(DISCOVERABLE_DURATION);
                    } catch (IOException e1) {
                        openServerCallback.onFinished(e1);
                    }
                }
            }
        });
    }

    public void searchForServer(final OnSearchForServerCallback callback) {
        turnOnBluetooth(new OnTurnOnBluetoothCallback() {
            @Override
            public void onBluetoothOn(IOException e) {
                if (e != null) {
                    callback.onSearchFinished(null);
                } else {
                    startDiscovery(callback);
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_CANCELED && turnOnBluetoothCallback != null) {
                    turnOnBluetoothCallback.onBluetoothOn(new IOException("User cancelled the bluetooth activation"));
                }
                break;
            case DISCOVERY_REQUEST:
                if (resultCode == DISCOVERABLE_DURATION && openServerCallback != null) {
                    openServerCallback.onFinished(null);
                } else if (resultCode == Activity.RESULT_CANCELED && openServerCallback != null) {
                    openServerCallback.onFinished(new IOException("User cancelled the discoverability request"));
                }
                openServerCallback = null;
            default:
                break;
        }
    }

    private void connectToServer(Server server) {
        try {
            socket = server.getDevice().createRfcommSocketToServiceRecord(UUID);
            Thread listenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket.connect();
                        listenForMessages();
                    } catch (IOException e) {
                        Log.e(TAG, "Server connection IO Exception", e);
                    }
                }
            });
            listenThread.start();
        } catch (IOException e) {
            Log.e(TAG, "Bluetooth client IO Exception", e);
        }
    }


    /**
     * Turns ON the bluetooth and calls the callback method.
     *
     * @param callback
     */
    protected void turnOnBluetooth(final OnTurnOnBluetoothCallback callback) {
        if (bluetooth.isEnabled() && callback != null) {
            callback.onBluetoothOn(null);
            turnOnBluetoothCallback = null;
        } else {
            turnOnBluetoothCallback = callback;
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, ENABLE_BLUETOOTH);
        }
    }

    /**
     * Makes the device discoverable
     *
     * @param seconds Discoverability duration (0 for infinite)
     */
    private void makeDiscoverable(int seconds) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
        activity.startActivityForResult(discoverableIntent, DISCOVERY_REQUEST);
    }

    /**
     * Starts the server socket and waits for connections
     *
     * @throws IOException
     */
    protected void startServerSocket() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
        serverSocket = bluetooth.listenUsingRfcommWithServiceRecord(name, UUID);
        if (serverThread != null) {
            serverThread.interrupt();
        }
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = serverSocket.accept();
                    listenForMessages();
                } catch (IOException e) {
                    Log.e(TAG, "Server connection IO Exception", e);
                }
            }
        });
        serverThread.start();
    }


    protected void startDiscovery(final OnSearchForServerCallback callback) {
        BroadcastReceiver discoveryBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                    callback.onSearchStarted();
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                    callback.onSearchFinished(serverList);
                }
            }
        };
        activity.registerReceiver(discoveryBR, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        activity.registerReceiver(discoveryBR, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        BroadcastReceiver newDeviceBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (newDevice.getName().startsWith(name)) {
                    Server server = new Server(name.substring(name.length()), newDevice);
                    serverList.add(server);
                    callback.onNewServer(server);
                }
            }
        };
        activity.registerReceiver(newDeviceBR, new IntentFilter(BluetoothDevice.ACTION_FOUND)); // todo test double registration
        serverList.clear();
        bluetooth.startDiscovery();
    }


    private void listenForMessages() {
        Log.d(TAG, Build.MODEL + " listen message");

        boolean listening = true;

        try {
            InputStream inputStream = socket.getInputStream();
            int bytesRead;
            while (listening) {
                byte firstByte = (byte) inputStream.read();
                byte secondByte = (byte) inputStream.read();
                conversionBuffer.put(0, firstByte);
                conversionBuffer.put(1, secondByte);
                int contentLength = conversionBuffer.getShort(0);
                byte messageType = (byte) inputStream.read();
                bytesRead = 0;
                do {
                    int offset = bytesRead;
                    bytesRead += inputStream.read(buffer, offset, contentLength - bytesRead);
                } while (bytesRead < contentLength);
                String result = new String(buffer, 0, bytesRead); //todo need String?
                Log.d(TAG, "Contenu reçu " + messageType + ":\n" + result + "\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Read message IO Exception", e);
        }
    }


    private void registerBroadcastReceivers() {
        BroadcastReceiver bluetoothStateBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        if (turnOnBluetoothCallback != null) {
                            turnOnBluetoothCallback.onBluetoothOn(null);
                            turnOnBluetoothCallback = null;
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        if (turnOnBluetoothCallback != null) {
                            turnOnBluetoothCallback.onBluetoothOn(new IOException("Bluetooth OFF"));
                            turnOnBluetoothCallback = null;
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        activity.registerReceiver(bluetoothStateBR, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }
}

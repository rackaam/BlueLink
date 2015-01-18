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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import eu.rakam.bluelinklib.callbacks.OnConnectToServerCallback;
import eu.rakam.bluelinklib.callbacks.OnOpenServerCallback;
import eu.rakam.bluelinklib.callbacks.OnSearchForServerCallback;
import eu.rakam.bluelinklib.callbacks.OnTurnOnBluetoothCallback;

public class BlueLink {

    private static final String TAG = "BlueLinkDebug";
    private static final byte PROTOCOL_VERSION = 1;
    private static final int ENABLE_BLUETOOTH = 22000;
    private static final int DISCOVERY_REQUEST = 22001;
    private static final int DISCOVERABLE_DURATION = 60;
    private static final int BUFFER_SIZE = 32768;
    private static final String HEADER_SPAN = "   ";
    private static final int HEADER_SIZE = HEADER_SPAN.length();

    private Activity activity;
    private String serverName;
    private java.util.UUID UUID;

    private BluetoothAdapter bluetooth;
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private List<Client> clientList = new ArrayList<>();
    private List<Server> serverList = new ArrayList<>();
    private ByteBuffer twoBytesConversionBuffer = ByteBuffer.allocate(2);
    private byte[] buffer = new byte[BUFFER_SIZE];

    private OnTurnOnBluetoothCallback turnOnBluetoothCallback;
    private OnOpenServerCallback openServerCallback;
    private OnSearchForServerCallback searchForServerCallback;
    private Thread serverThread;

    /**
     * @param serverName Name of the server seen by other players
     */
    public BlueLink(Activity activity, String serverName, String UUID) {
        this.activity = activity;
        this.serverName = serverName;
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
                    openServerCallback.onOpen(e);
                } else {
                    try {
                        bluetooth.setName(serverName + "" + Build.MODEL);
                        startServerSocket();
                        makeDiscoverable(DISCOVERABLE_DURATION);
                    } catch (IOException e1) {
                        openServerCallback.onOpen(e1);
                    }
                }
            }
        });
    }

    /**
     * Search for available servers.
     *
     * @param callback
     */
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

    /**
     * Connects to the server.
     * <p/>
     * <p>The data sent to the server during the connection (<code>out</code>) can be retrieved by
     * the server in the {@link eu.rakam.bluelinklib.callbacks.OnOpenServerCallback#onNewClient(Client, BlueLinkInputStream)}
     * callback method.
     *
     * @param server   server to connect to
     * @param out      data to send to the server during the connection (can be null)
     * @param callback
     */
    public void connectToServer(Server server, final BlueLinkOutputStream out, final OnConnectToServerCallback callback) {
        try {
            bluetooth.cancelDiscovery();
            socket = server.getDevice().createRfcommSocketToServiceRecord(UUID);
            Thread listenThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket.connect();
                        handshakingClient(out);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onConnect(null);
                            }
                        });
                        listenForMessages();
                    } catch (final IOException e) {
                        Log.e(TAG, "Server connection IO Exception", e);
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onConnect(e);
                            }
                        });
                    }
                }
            });
            listenThread.start();
        } catch (IOException e) {
            Log.e(TAG, "Bluetooth client IO Exception", e);
            callback.onConnect(e);
        }
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
                    openServerCallback.onOpen(null);
                } else if (resultCode == Activity.RESULT_CANCELED && openServerCallback != null) {
                    openServerCallback.onOpen(new IOException("User cancelled the discoverability request"));
                }
            default:
                break;
        }
    }


    /**
     * Turns ON the bluetooth and calls the callback method.
     *
     * @param callback
     */
    private void turnOnBluetooth(final OnTurnOnBluetoothCallback callback) {
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


    private void startDiscovery(final OnSearchForServerCallback callback) {
        searchForServerCallback = callback;
        serverList.clear();
        bluetooth.startDiscovery();
    }

    /**
     * Starts the server socket and waits for connections
     *
     * @throws IOException
     */
    private void startServerSocket() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
        serverSocket = bluetooth.listenUsingRfcommWithServiceRecord(serverName, UUID);
        if (serverThread != null) {
            serverThread.interrupt();
        }
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = serverSocket.accept();
                    final byte[] handshakeData = handshakingServer();
                    final Client client = new Client(socket);
                    clientList.add(client);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            openServerCallback.onNewClient(client,
                                    handshakeData == null ? null : new BlueLinkInputStream(handshakeData));
                        }
                    });
                    listenForMessages();
                } catch (IOException e) {
                    Log.e(TAG, "Server connection IO Exception", e);
                }
            }
        });
        serverThread.start();
    }

    /**
     * Sends the protocol version, two bytes to indicate the size of the frame then the frame.
     *
     * @param out the data to send
     */
    private void handshakingClient(BlueLinkOutputStream out) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(PROTOCOL_VERSION);
            if (out != null) {
                outputStream.write(twoBytesConversionBuffer.putChar(0, (char) out.getSize()).array());
                outputStream.write(out.toByteArray());
            } else {
                outputStream.write(twoBytesConversionBuffer.putChar(0, (char) 0).array());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the protocol version, gets the data sent and returns them in a byte array.
     *
     * @return the data sent by the client. Can be null
     */
    private byte[] handshakingServer() {
        try {
            InputStream inputStream = socket.getInputStream();
            byte clientProtocolVerison = (byte) inputStream.read();
            byte byte0 = (byte) inputStream.read();
            byte byte1 = (byte) inputStream.read();
            twoBytesConversionBuffer.put(0, byte0);
            twoBytesConversionBuffer.put(1, byte1);
            int frameSize = twoBytesConversionBuffer.getShort(0);
            byte[] dataIn = null;
            if (frameSize > 0) {
                dataIn = new byte[frameSize];
                int bytesRead = 0;
                while (bytesRead < frameSize) {
                    int offset = bytesRead;
                    bytesRead += inputStream.read(dataIn, offset, frameSize - bytesRead);
                }
            }
            return dataIn;
        } catch (IOException e) {
            Log.e(TAG, "Handshaking server IO Exception", e);
            return null;
        }
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
                twoBytesConversionBuffer.put(0, firstByte);
                twoBytesConversionBuffer.put(1, secondByte);
                int contentLength = twoBytesConversionBuffer.getShort(0);
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

        BroadcastReceiver discoveryBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                    if (searchForServerCallback != null) {
                        searchForServerCallback.onSearchStarted();
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                    if (searchForServerCallback != null) {
                        searchForServerCallback.onSearchFinished(serverList);
                        searchForServerCallback = null;
                    }
                }
            }
        };
        activity.registerReceiver(discoveryBR, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        activity.registerReceiver(discoveryBR, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        BroadcastReceiver newDeviceBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "New Device");
                if (newDevice.getName().startsWith(serverName)) {
                    Log.d(TAG, "New Device OK");
                    Server server = new Server(newDevice.getName().substring(serverName.length()), newDevice);
                    serverList.add(server);
                    if (searchForServerCallback != null)
                        searchForServerCallback.onNewServer(server);
                }
            }
        };
        activity.registerReceiver(newDeviceBR, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

}

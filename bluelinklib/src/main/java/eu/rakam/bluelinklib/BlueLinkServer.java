package eu.rakam.bluelinklib;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.rakam.bluelinklib.callbacks.OnNewClientCallback;
import eu.rakam.bluelinklib.callbacks.OnNewConnectionCallback;
import eu.rakam.bluelinklib.callbacks.OnNewFrameCallback;
import eu.rakam.bluelinklib.callbacks.OnNewMessageCallback;
import eu.rakam.bluelinklib.callbacks.OnOpenServerCallback;
import eu.rakam.bluelinklib.callbacks.OnTurnOnBluetoothCallback;
import eu.rakam.bluelinklib.threads.ConnectedThread;
import eu.rakam.bluelinklib.threads.ServerHandshakingThread;
import eu.rakam.bluelinklib.threads.ServerThread;

public class BlueLinkServer implements OnNewClientCallback, OnNewFrameCallback {

    private static final int DISCOVERY_REQUEST = 22001;
    private static final int DISCOVERABLE_DURATION = 60;

    private Activity activity;
    private String serverName;
    private java.util.UUID UUID;

    private final BluetoothAdapter bluetooth;
    private final ArrayList<Client> clientList = new ArrayList<>();
    private ServerThread serverThread;

    private OnNewMessageCallback messageCallback;
    private OnTurnOnBluetoothCallback turnOnBluetoothCallback;
    private OnOpenServerCallback openServerCallback;


    /**
     * @param serverName Name of the server seen by other players
     */
    public BlueLinkServer(Activity activity, String serverName, String UUID) {
        this(activity, serverName, UUID, null);
    }

    public BlueLinkServer(Activity activity, String serverName, String UUID, OnNewMessageCallback messageCallback) {
        this.activity = activity;
        this.serverName = serverName;
        this.UUID = java.util.UUID.fromString(UUID);
        this.messageCallback = messageCallback;
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
                        bluetooth.setName(serverName + Build.MODEL);
                        openServer();
                        makeDiscoverable(DISCOVERABLE_DURATION);
                    } catch (IOException e1) {
                        openServerCallback.onOpen(e1);
                    }
                }
            }
        });
    }


    public void sendMessage(Client client, String message) {
        if (message == null)
            return;
        BlueLinkOutputStream outputStream = new BlueLinkOutputStream();
        outputStream.writeString(message);
        sendMessage(client, BlueLink.USER_MESSAGE, outputStream);
    }


    public void sendMessage(Client client, BlueLinkOutputStream message) {
        sendMessage(client, BlueLink.USER_MESSAGE, message);
    }


    private void sendMessage(Client client, byte type, BlueLinkOutputStream message) {
        if (message == null)
            return;
        client.getConnectedThread().sendMessage(type, message);
    }

    /**
     * Closes the server. New clients can't connect any more.
     * The connections already established with the clients stay alive.
     */
    public void closeServer() {
        if (serverThread != null) {
            serverThread.cancel();
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BlueLink.ENABLE_BLUETOOTH:
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
            activity.startActivityForResult(intent, BlueLink.ENABLE_BLUETOOTH);
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
    private void openServer() throws IOException {
        serverThread = new ServerThread(new OnNewConnectionCallback() {
            @Override
            public void onNewConnection(BluetoothSocket socket) {
                ServerHandshakingThread thread = new ServerHandshakingThread(BlueLinkServer.this, socket);
                thread.start();

            }
        }, bluetooth, serverName, UUID);
        serverThread.start();
    }


    @Override
    public void onNewClient(final Client client, final BlueLinkInputStream in) {
        ConnectedThread thread = new ConnectedThread(client, this);
        clientList.add(client);
        client.setConnectedThread(thread);
        if (openServerCallback != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    openServerCallback.onNewClient(client, in);
                }
            });
        }
        thread.start();
    }


    @Override
    public void onNewFrame(final int senderID, byte messageType, final BlueLinkInputStream in) {
        switch (messageType) {
            case BlueLink.USER_MESSAGE:
                if (messageCallback != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messageCallback.onNewMessage(senderID, in);
                        }
                    });
                }
                break;
            case BlueLink.SYNC_MESSAGE:
                break;
            default:
                break;
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

        BroadcastReceiver scanModeChangedBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);

                if (mode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE && serverThread != null) {
                    closeServer();
                }
            }
        };
        activity.registerReceiver(scanModeChangedBR, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
    }


    public OnNewMessageCallback getMessageCallback() {
        return messageCallback;
    }


    public void setMessageCallback(OnNewMessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }


    public List<Client> getClientList() {
        return (List<Client>) clientList.clone();
    }
}

package eu.rakam.bluelinklib;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.rakam.bluelinklib.callbacks.OnNewClientCallback;
import eu.rakam.bluelinklib.callbacks.OnNewConnectionCallback;
import eu.rakam.bluelinklib.callbacks.OnNewMessageCallback;
import eu.rakam.bluelinklib.callbacks.OnNewUserMessageCallback;
import eu.rakam.bluelinklib.callbacks.OnOpenServerCallback;
import eu.rakam.bluelinklib.callbacks.OnTurnOnBluetoothCallback;
import eu.rakam.bluelinklib.sync.BLSyncThread;
import eu.rakam.bluelinklib.sync.BLSynchronizable;
import eu.rakam.bluelinklib.sync.messages.Message;
import eu.rakam.bluelinklib.sync.messages.NewInstanceMessage;
import eu.rakam.bluelinklib.sync.messages.UpdateMessage;
import eu.rakam.bluelinklib.threads.ConnectedServerThread;
import eu.rakam.bluelinklib.threads.ServerHandshakingThread;
import eu.rakam.bluelinklib.threads.ServerThread;

public class BlueLinkServer implements OnNewClientCallback, OnNewUserMessageCallback {

    private static final int DISCOVERY_REQUEST = 22001;
    private static final int DISCOVERABLE_DURATION = 60;

    private Activity activity;
    private Handler handler;
    private String serverName;
    private java.util.UUID UUID;

    private final BluetoothAdapter bluetooth;
    private final ArrayList<Client> clientList = new ArrayList<>();
    private ServerThread serverThread;
    private BLSyncThread syncThread = new BLSyncThread(this);

    private OnNewMessageCallback messageCallback;
    private OnTurnOnBluetoothCallback turnOnBluetoothCallback;
    private OnOpenServerCallback openServerCallback;


    /**
     * @param activity
     * @param handler         the thread associated with this handler will be used to run all the callback methods
     * @param serverName
     * @param UUID            unique ID for your application
     * @param messageCallback callback method called when the client receive a message sent by {@link eu.rakam.bluelinklib.BlueLinkServer#sendMessage(Client, BlueLinkOutputStream)}.
     */
    public BlueLinkServer(Activity activity, Handler handler, String serverName, String UUID,
                          OnNewMessageCallback messageCallback) {
        this.activity = activity;
        this.handler = handler;
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
            public void onBluetoothOn(final IOException e) {
                if (e != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            openServerCallback.onOpen(e);
                        }
                    });
                } else {
                    try {
                        bluetooth.setName(serverName + Build.MODEL);
                        openServer();
                        makeDiscoverable(DISCOVERABLE_DURATION);
                    } catch (final IOException e1) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                openServerCallback.onOpen(e1);
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * Syncs a new instance with all the clients.
     *
     * @param synchronizable the new instance to sync
     * @param out            data to send to the client's factory
     */
    public void syncNewInstance(BLSynchronizable synchronizable, BlueLinkOutputStream out) {
        syncThread.syncNewInstance(synchronizable, out);
    }

    /**
     * Syncs the instance with its equivalents.
     * Calls {@link eu.rakam.bluelinklib.sync.BLSynchronizable#getDataToSync()} to get the data to
     * send to the others instances.
     * The equivalents will get the data to sync in the
     * {@link eu.rakam.bluelinklib.sync.BLSynchronizable#syncData(BlueLinkInputStream)} method.
     *
     * @param synchronizable instance to sync
     */
    public void sync(BLSynchronizable synchronizable) {
        syncThread.sync(synchronizable);
    }

    /**
     * Syncs the instance with its equivalents.
     * The equivalents will get the data to sync in the
     * {@link eu.rakam.bluelinklib.sync.BLSynchronizable#syncData(BlueLinkInputStream)} method.
     *
     * @param synchronizable instance to sync
     * @param out            data sent to its equivalents
     */
    public void sync(BLSynchronizable synchronizable, BlueLinkOutputStream out) {
        syncThread.sync(synchronizable, out);
    }

    /**
     * Convenient method to send a basic string to a client.
     * The message can be retrieved by the {@link eu.rakam.bluelinklib.BlueLinkClient}
     * with the {@link eu.rakam.bluelinklib.callbacks.OnNewMessageCallback} passed to his constructor.
     *
     * @param client  recipient
     * @param message string to send
     */
    public void sendMessage(Client client, String message) {
        if (message == null)
            return;
        BlueLinkOutputStream outputStream = new BlueLinkOutputStream();
        outputStream.writeString(message);
        sendMessage(client, BlueLink.USER_MESSAGE, outputStream);
    }

    /**
     * Sends the message to the client.
     * The message can be retrieved by the {@link eu.rakam.bluelinklib.BlueLinkClient}
     * with the {@link eu.rakam.bluelinklib.callbacks.OnNewMessageCallback} passed to his constructor.
     *
     * @param client  recipient
     * @param message message to send
     */
    public void sendMessage(Client client, BlueLinkOutputStream message) {
        sendMessage(client, BlueLink.USER_MESSAGE, message);
    }

    private void sendMessage(Client client, byte type, BlueLinkOutputStream message) {
        if (message == null)
            return;
        client.getConnectedServerThread().sendMessage(type, message);
    }

    /**
     * Sends the string to all the clients.
     *
     * @param message
     */
    public void broadcastMessage(String message) {
        if (message == null)
            return;
        BlueLinkOutputStream outputStream = new BlueLinkOutputStream();
        outputStream.writeString(message);
        broadcastMessage(BlueLink.USER_MESSAGE, outputStream);
    }

    /**
     * Sends the message to all the clients.
     *
     * @param message
     */
    public void broadcastMessage(BlueLinkOutputStream message) {
        broadcastMessage(BlueLink.USER_MESSAGE, message);
    }


    private void broadcastMessage(Byte type, BlueLinkOutputStream message) {
        if (message == null)
            return;
        for (Client client : clientList) {
            client.getConnectedServerThread().sendMessage(type, message);
        }
    }

    /**
     * Used by BlueLink. Must not be called.
     *
     * @param message
     */
    public void broadcastSyncMessage(Message message) {
        switch (message.getType()) {
            case BlueLink.NEW_INSTANCE_MESSAGE:
                for (Client client : clientList) {
                    client.getConnectedServerThread().sendNewInstanceMessage((NewInstanceMessage) message);
                }
                break;
            case BlueLink.UPDATE_MESSAGE:
                for (Client client : clientList) {
                    client.getConnectedServerThread().sendUpdateMessage((UpdateMessage) message);
                }
                break;
        }

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

    /**
     * This method must be called every time from {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BlueLink.ENABLE_BLUETOOTH:
                if (resultCode == Activity.RESULT_CANCELED && turnOnBluetoothCallback != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            turnOnBluetoothCallback.onBluetoothOn(new IOException("User cancelled the bluetooth activation"));
                        }
                    });
                }
                break;
            case DISCOVERY_REQUEST:
                if (resultCode == DISCOVERABLE_DURATION && openServerCallback != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            openServerCallback.onOpen(null);
                        }
                    });
                } else if (resultCode == Activity.RESULT_CANCELED && openServerCallback != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            openServerCallback.onOpen(new IOException("User cancelled the discoverability request"));
                        }
                    });
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

    private void startSyncThread() {
        if (syncThread == null || !syncThread.isAlive()) {
            syncThread = new BLSyncThread(this);
            syncThread.start();
        }
    }

    @Override
    public void onNewClient(final Client client, final BlueLinkInputStream in) {
        startSyncThread();
        ConnectedServerThread thread = new ConnectedServerThread(client, this);
        clientList.add(client);
        client.setConnectedServerThread(thread);
        if (openServerCallback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    openServerCallback.onNewClient(client, in);
                }
            });
        }
        thread.start();
    }

    @Override
    public void onMessage(final int senderID, final BlueLinkInputStream in) {
        if (messageCallback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    messageCallback.onNewMessage(senderID, in);
                }
            });
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

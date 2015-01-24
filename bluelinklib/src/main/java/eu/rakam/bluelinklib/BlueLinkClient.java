package eu.rakam.bluelinklib;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.rakam.bluelinklib.callbacks.OnConnectToServerCallback;
import eu.rakam.bluelinklib.callbacks.OnNewMessageCallback;
import eu.rakam.bluelinklib.callbacks.OnNewSyncMessageCallback;
import eu.rakam.bluelinklib.callbacks.OnNewUserMessageCallback;
import eu.rakam.bluelinklib.callbacks.OnSearchForServerCallback;
import eu.rakam.bluelinklib.callbacks.OnTurnOnBluetoothCallback;
import eu.rakam.bluelinklib.sync.BLFactory;
import eu.rakam.bluelinklib.sync.BLSynchronizable;
import eu.rakam.bluelinklib.threads.ConnectThread;
import eu.rakam.bluelinklib.threads.ConnectedClientThread;

public class BlueLinkClient implements OnNewUserMessageCallback, OnNewSyncMessageCallback {

    private Activity activity;
    private Handler handler;
    private String serverName;
    private java.util.UUID UUID;

    private BluetoothAdapter bluetooth;
    private final ArrayList<Server> serverList = new ArrayList<>();

    private ConnectedClientThread connectedClientThread;
    private OnNewMessageCallback messageCallback;
    private OnTurnOnBluetoothCallback turnOnBluetoothCallback;
    private OnSearchForServerCallback searchForServerCallback;

    private BLFactory factory;
    private SparseArray<BLSynchronizable> objects = new SparseArray<>();


    public BlueLinkClient(Activity activity, Handler handler, String serverName, String UUID, BLFactory factory,
                          OnNewMessageCallback messageCallback) {
        this.activity = activity;
        this.handler = handler;
        this.serverName = serverName;
        this.UUID = java.util.UUID.fromString(UUID);
        this.factory = factory;
        this.messageCallback = messageCallback;
        this.bluetooth = BluetoothAdapter.getDefaultAdapter();
        registerBroadcastReceivers();
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
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSearchFinished(null);
                        }
                    });
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
            final BluetoothSocket socket = server.getDevice().createRfcommSocketToServiceRecord(UUID);
            final ConnectThread connectThread = new ConnectThread(socket, out, new OnConnectToServerCallback() {
                @Override
                public void onConnect(final IOException e) {
                    if (e == null) {
                        connectedClientThread = new ConnectedClientThread(socket, BlueLinkClient.this, BlueLinkClient.this);
                        connectedClientThread.start();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onConnect(e);
                        }
                    });
                }
            });
            connectThread.start();
        } catch (final IOException e) {
            Log.e(BlueLink.TAG, "Bluetooth client IO Exception", e);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onConnect(e);
                }
            });
        }
    }


    public void sendMessage(String message) {
        if (message == null)
            return;
        BlueLinkOutputStream outputStream = new BlueLinkOutputStream();
        outputStream.writeString(message);
        sendMessage(BlueLink.USER_MESSAGE, outputStream);
    }


    public void sendMessage(BlueLinkOutputStream message) {
        sendMessage(BlueLink.USER_MESSAGE, message);
    }


    private void sendMessage(byte type, BlueLinkOutputStream message) {
        if (message == null)
            return;
        if (connectedClientThread != null) {
            connectedClientThread.sendMessage(type, message);
        }
    }


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
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onBluetoothOn(null);
                    turnOnBluetoothCallback = null;
                }
            });
        } else {
            turnOnBluetoothCallback = callback;
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, BlueLink.ENABLE_BLUETOOTH);
        }
    }


    private void startDiscovery(final OnSearchForServerCallback callback) {
        searchForServerCallback = callback;
        serverList.clear();
        bluetooth.startDiscovery();
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

    @Override
    public void onNewInstanceMessage(final String className, final int ID, final BlueLinkInputStream in) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                BLSynchronizable o = factory.instantiate(className, in);
                objects.put(ID, o);
            }
        });
    }

    @Override
    public void onUpdateMessage(int id, final BlueLinkInputStream in) {
        final BLSynchronizable o = objects.get(id, null);
        if (o != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    o.syncData(in);
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

        BroadcastReceiver discoveryBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                    if (searchForServerCallback != null) {
                        searchForServerCallback.onSearchStarted();
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                    if (searchForServerCallback != null) {
                        searchForServerCallback.onSearchFinished((List<Server>) serverList.clone());
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
                Log.d(BlueLink.TAG, "New Device");
                if (newDevice.getName().startsWith(serverName)) {
                    Log.d(BlueLink.TAG, "New Device OK");
                    Server server = new Server(newDevice.getName().substring(serverName.length()), newDevice);
                    serverList.add(server);
                    if (searchForServerCallback != null)
                        searchForServerCallback.onNewServer(server);
                }
            }
        };
        activity.registerReceiver(newDeviceBR, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    public OnNewMessageCallback getMessageCallback() {
        return messageCallback;
    }

    public void setMessageCallback(OnNewMessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }
}

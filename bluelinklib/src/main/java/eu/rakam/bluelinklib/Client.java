package eu.rakam.bluelinklib;

import android.bluetooth.BluetoothSocket;

import eu.rakam.bluelinklib.threads.ConnectedServerThread;

public class Client {

    private static int idCount = 0; // 0 is reserved for the server

    private final int id;
    private final byte protocol;
    private final BluetoothSocket socket;
    private ConnectedServerThread connectedServerThread;

    public static synchronized int generateClientId() {
        idCount++;
        return idCount;
    }

    public Client(byte protocol, BluetoothSocket socket) {
        this.id = generateClientId();
        this.protocol = protocol;
        this.socket = socket;
    }

    public int getId() {
        return id;
    }

    public byte getProtocol() {
        return protocol;
    }

    public BluetoothSocket getSocket() {
        return socket;
    }

    public ConnectedServerThread getConnectedServerThread() {
        return connectedServerThread;
    }

    public void setConnectedServerThread(ConnectedServerThread connectedServerThread) {
        this.connectedServerThread = connectedServerThread;
    }
}

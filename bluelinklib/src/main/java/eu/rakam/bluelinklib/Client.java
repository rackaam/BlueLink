package eu.rakam.bluelinklib;

import android.bluetooth.BluetoothSocket;

import eu.rakam.bluelinklib.threads.ConnectedThread;

public class Client {

    private static int idCount = 0; // 0 is reserved for the server

    private final int id;
    private final byte protocol;
    private final BluetoothSocket socket;
    private ConnectedThread connectedThread;

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

    public ConnectedThread getConnectedThread() {
        return connectedThread;
    }

    protected void setConnectedThread(ConnectedThread connectedThread) {
        this.connectedThread = connectedThread;
    }
}

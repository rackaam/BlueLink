package eu.rakam.bluelinklib;

import android.bluetooth.BluetoothSocket;

public class Client {

    private final BluetoothSocket socket;

    public Client(BluetoothSocket socket) {
        this.socket = socket;
    }

    public BluetoothSocket getSocket() {
        return socket;
    }
}

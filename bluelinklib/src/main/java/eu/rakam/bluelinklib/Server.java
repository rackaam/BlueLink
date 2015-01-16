package eu.rakam.bluelinklib;

import android.bluetooth.BluetoothDevice;

public class Server {

    private final BluetoothDevice device;
    private final String name;

    public Server(String name, BluetoothDevice device) {
        this.name = name;
        this.device = device;
    }

    protected BluetoothDevice getDevice() {
        return device;
    }

    public String getName() {
        return name;
    }
}

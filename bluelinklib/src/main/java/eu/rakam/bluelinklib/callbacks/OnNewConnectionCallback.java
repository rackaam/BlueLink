package eu.rakam.bluelinklib.callbacks;

import android.bluetooth.BluetoothSocket;

public interface OnNewConnectionCallback {
    public void onNewConnection(BluetoothSocket socket);
}

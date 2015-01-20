package eu.rakam.bluelinklib.threads;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import eu.rakam.bluelinklib.callbacks.OnNewConnectionCallback;

public class ServerThread extends Thread {

    private final static String TAG = "ServerThread";

    private final OnNewConnectionCallback newConnectionCallback;
    private final BluetoothServerSocket serverSocket;
    private boolean loop = true;

    public ServerThread(OnNewConnectionCallback newConnectionCallback, BluetoothAdapter adapter,
                        String name, UUID uuid) {
        this.newConnectionCallback = newConnectionCallback;

        BluetoothServerSocket tmpSocket = null;
        try {
            tmpSocket = adapter.listenUsingRfcommWithServiceRecord(name, uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverSocket = tmpSocket;
    }

    @Override
    public void run() {
        while (loop) {
            try {
                BluetoothSocket socket = serverSocket.accept();
                newConnectionCallback.onNewConnection(socket);
            } catch (IOException e) {
                Log.e(TAG, "Server connection IO Exception", e);
            }
        }
    }

    public void cancel() {
        Log.d(TAG, "Server thread cancelled");
        try {
            loop = false;
            serverSocket.close();
        } catch (IOException e) {
        }
    }


}

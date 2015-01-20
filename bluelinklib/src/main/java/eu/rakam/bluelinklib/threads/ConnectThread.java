package eu.rakam.bluelinklib.threads;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import eu.rakam.bluelinklib.BlueLink;
import eu.rakam.bluelinklib.BlueLinkOutputStream;
import eu.rakam.bluelinklib.callbacks.OnConnectToServerCallback;

public class ConnectThread extends Thread {


    private final BluetoothSocket socket;
    private final BlueLinkOutputStream out;
    private final OnConnectToServerCallback connectToServerCallback;
    private final ByteBuffer twoBytesConversionBuffer = ByteBuffer.allocate(2);

    public ConnectThread(BluetoothSocket socket, BlueLinkOutputStream out, OnConnectToServerCallback connectToServerCallback) {
        this.socket = socket;
        this.out = out;
        this.connectToServerCallback = connectToServerCallback;
    }

    @Override
    public void run() {
        try {
            socket.connect();
            handshakingClient(out);
            if (connectToServerCallback != null) {
                connectToServerCallback.onConnect(null);
            }
        } catch (final IOException e) {
            Log.e(BlueLink.TAG, "Server connection IO Exception", e);
            if (connectToServerCallback != null) {
                connectToServerCallback.onConnect(e);
            }
        }
    }

    /**
     * Sends the protocol version, two bytes to indicate the size of the frame then the frame.
     *
     * @param out the data to send
     */
    private void handshakingClient(BlueLinkOutputStream out) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(BlueLink.PROTOCOL_VERSION);
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
}

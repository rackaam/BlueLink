package eu.rakam.bluelinklib.threads;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.Client;
import eu.rakam.bluelinklib.callbacks.OnNewFrameCallback;

public class ConnectedThread extends Thread {

    private static final String TAG = "ConnectedThread";

    private final OnNewFrameCallback newFrameCallback;
    private final BluetoothSocket socket;
    private final int clientId;
    private ByteBuffer twoBytesConversionBuffer = ByteBuffer.allocate(2);

    /**
     * User by the client to communicate with the server.
     *
     * @param socket
     * @param newFrameCallback
     */
    public ConnectedThread(BluetoothSocket socket, OnNewFrameCallback newFrameCallback) {
        this.newFrameCallback = newFrameCallback;
        this.socket = socket;
        this.clientId = 0;
    }

    /**
     * Used by the server to communicate with the clients.
     *
     * @param client
     * @param newFrameCallback
     */
    public ConnectedThread(Client client, OnNewFrameCallback newFrameCallback) {
        this.newFrameCallback = newFrameCallback;
        this.socket = client.getSocket();
        this.clientId = client.getId();
    }

    @Override
    public void run() {
        boolean listening = true;

        try {
            InputStream inputStream = socket.getInputStream();
            int bytesRead;
            while (listening) {
                byte messageType = (byte) inputStream.read();
                byte byte0 = (byte) inputStream.read();
                byte byte1 = (byte) inputStream.read();
                twoBytesConversionBuffer.put(0, byte0);
                twoBytesConversionBuffer.put(1, byte1);
                int frameSize = twoBytesConversionBuffer.getShort(0);
                final byte[] buffer = new byte[frameSize];
                bytesRead = 0;
                while (bytesRead < frameSize) {
                    int offset = bytesRead;
                    bytesRead += inputStream.read(buffer, offset, frameSize - bytesRead);
                }
                if (newFrameCallback != null) {
                    newFrameCallback.onNewFrame(clientId, messageType, new BlueLinkInputStream(buffer));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Read message IO Exception", e);
        }
    }
}

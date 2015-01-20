package eu.rakam.bluelinklib.threads;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import eu.rakam.bluelinklib.BlueLink;
import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.BlueLinkOutputStream;
import eu.rakam.bluelinklib.Client;
import eu.rakam.bluelinklib.callbacks.OnNewFrameCallback;

public class ConnectedThread extends Thread {

    private static final String TAG = "ConnectedThread";

    private static final int HEADER_SIZE = 3;

    private final OnNewFrameCallback newFrameCallback;
    private final BluetoothSocket socket;
    private final int clientId;
    private InputStream inputStream;
    private OutputStream outputStream;
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

        initStreams();
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

        initStreams();
    }

    private void initStreams() {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "get streams error", e);
        }

        inputStream = tmpIn;
        outputStream = tmpOut;
    }

    @Override
    public void run() {
        boolean listening = true;

        try {
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

    public void sendMessage(byte type, BlueLinkOutputStream message) {
        if (message == null)
            return;
        try {
            byte[] header = new byte[HEADER_SIZE];
            int contentLength = message.getSize();
            byte[] contentLengthTab = twoBytesConversionBuffer.putShort(0, (short) contentLength).array(); // conversion int/byte
            header[0] = type;
            header[1] = contentLengthTab[0];
            header[2] = contentLengthTab[1];
            outputStream.write(header);
            outputStream.write(message.toByteArray());
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "Send message IO Exception", e);
        }
    }
}

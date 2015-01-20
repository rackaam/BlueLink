package eu.rakam.bluelinklib.threads;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.Client;
import eu.rakam.bluelinklib.callbacks.OnNewClientCallback;

public class ServerHandshakingThread extends Thread {

    private final static String TAG = "ServerHandshakingThread";

    private final OnNewClientCallback newClientCallback;
    private final BluetoothSocket socket;
    private Client client;
    private ByteBuffer twoBytesConversionBuffer = ByteBuffer.allocate(2);

    public ServerHandshakingThread(OnNewClientCallback newClientCallback, BluetoothSocket socket) {
        this.newClientCallback = newClientCallback;
        this.socket = socket;
    }

    @Override
    public void run() {
        final byte[] handshakeData = handshakingServer();
        if (newClientCallback != null) {
            newClientCallback.onNewClient(client, handshakeData == null ? null : new BlueLinkInputStream(handshakeData));
        }
    }

    /**
     * Reads the protocol version, gets the data sent and returns them in a byte array.
     *
     * @return the data sent by the client. Can be null
     */
    private byte[] handshakingServer() {
        try {
            InputStream inputStream = socket.getInputStream();
            byte clientProtocolVersion = (byte) inputStream.read();
            this.client = new Client(clientProtocolVersion, socket);
            byte byte0 = (byte) inputStream.read();
            byte byte1 = (byte) inputStream.read();
            twoBytesConversionBuffer.put(0, byte0);
            twoBytesConversionBuffer.put(1, byte1);
            int frameSize = twoBytesConversionBuffer.getShort(0);
            byte[] buffer = null;
            if (frameSize > 0) {
                buffer = new byte[frameSize];
                int bytesRead = 0;
                while (bytesRead < frameSize) {
                    int offset = bytesRead;
                    bytesRead += inputStream.read(buffer, offset, frameSize - bytesRead);
                }
            }
            return buffer;
        } catch (IOException e) {
            Log.e(TAG, "Handshaking server IO Exception", e);
            return null;
        }
    }
}

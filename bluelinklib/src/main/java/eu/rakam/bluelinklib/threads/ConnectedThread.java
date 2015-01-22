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
import eu.rakam.bluelinklib.sync.messages.NewInstanceMessage;
import eu.rakam.bluelinklib.sync.messages.UpdateMessage;

public class ConnectedThread extends Thread {

    private static final String TAG = "ConnectedThread";

    private static final int MESSAGE_HEADER_SIZE = 3;
    private static final int UPDATE_MESSAGE_HEADER_SIZE = 7;
    private static final int NEW_INSTANCE_MESSAGE_HEADER_SIZE = 9;

    private final OnNewFrameCallback newFrameCallback;
    private final BluetoothSocket socket;
    private final int clientId;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ByteBuffer twoBytesConversionBuffer = ByteBuffer.allocate(2);
    private ByteBuffer fourBytesConversionBuffer = ByteBuffer.allocate(2);

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
            while (listening) {
                byte messageType = (byte) inputStream.read();
                switch (messageType) {
                    case BlueLink.USER_MESSAGE:
                        receiveUserMessage();
                        break;
                    case BlueLink.UPDATE_MESSAGE:
                        receiveUpdateMessage();
                        break;
                    default:
                        break;
                }

            }
        } catch (IOException e) {
            Log.e(TAG, "Read message IO Exception", e);
        }
    }

    private void receiveUserMessage() {
        try {
            byte byte0 = (byte) inputStream.read();
            byte byte1 = (byte) inputStream.read();
            twoBytesConversionBuffer.put(0, byte0);
            twoBytesConversionBuffer.put(1, byte1);
            int frameSize = twoBytesConversionBuffer.getChar(0);
            final byte[] buffer = new byte[frameSize];
            int bytesRead = 0;
            while (bytesRead < frameSize) {
                int offset = bytesRead;
                bytesRead += inputStream.read(buffer, offset, frameSize - bytesRead);
            }
            if (newFrameCallback != null) {
                newFrameCallback.onNewMessage(clientId, BlueLink.USER_MESSAGE, new BlueLinkInputStream(buffer));
            }
        } catch (IOException e) {
            Log.e(TAG, "ReceiveUserMessage Exception", e);
        }
    }

    private void receiveNewInstanceMessage() {
        // todo
    }

    private void receiveUpdateMessage() {
        try {
            byte byte0 = (byte) inputStream.read();
            byte byte1 = (byte) inputStream.read();
            byte byte2 = (byte) inputStream.read();
            byte byte3 = (byte) inputStream.read();
            fourBytesConversionBuffer.put(0, byte0);
            fourBytesConversionBuffer.put(1, byte1);
            fourBytesConversionBuffer.put(2, byte2);
            fourBytesConversionBuffer.put(3, byte3);
            int ID = fourBytesConversionBuffer.getInt(0);
            byte0 = (byte) inputStream.read();
            byte1 = (byte) inputStream.read();
            twoBytesConversionBuffer.put(0, byte0);
            twoBytesConversionBuffer.put(1, byte1);
            int frameSize = twoBytesConversionBuffer.getChar(0);
            final byte[] buffer = new byte[frameSize];
            int bytesRead = 0;
            while (bytesRead < frameSize) {
                int offset = bytesRead;
                bytesRead += inputStream.read(buffer, offset, frameSize - bytesRead);
            }
            if (newFrameCallback != null) {
                newFrameCallback.onNewMessage(clientId, BlueLink.UPDATE_MESSAGE, new BlueLinkInputStream(buffer));
            }
        } catch (IOException e) {
            Log.e(TAG, "ReceiveUserMessage Exception", e);
        }
    }

    public void sendMessage(byte type, BlueLinkOutputStream message) {
        if (message == null)
            return;
        try {
            byte[] header = new byte[MESSAGE_HEADER_SIZE];
            int contentLength = message.getSize();
            byte[] contentLengthTab = twoBytesConversionBuffer.putChar(0, (char) contentLength).array(); // conversion int/byte
            header[0] = type;
            header[1] = contentLengthTab[0];
            header[2] = contentLengthTab[1];
            send(header, message.toByteArray());
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "Send message IO Exception", e);
        }
    }

    public void sendNewInstanceMessage(NewInstanceMessage message) {
        try {
            byte[] header = new byte[NEW_INSTANCE_MESSAGE_HEADER_SIZE];
            byte[] idTab = fourBytesConversionBuffer.putInt(0, message.id).array();

            int contentLength = message.out.getSize();
            byte[] contentLengthTab = twoBytesConversionBuffer.putChar(0, (char) contentLength).array(); // conversion int/byte
            header[0] = BlueLink.NEW_INSTANCE_MESSAGE;
            header[1] = idTab[0];
            header[2] = idTab[1];
            header[3] = idTab[2];
            header[4] = idTab[3];
            header[5] = idTab[3];
            header[6] = idTab[3];
            header[7] = contentLengthTab[0];
            header[8] = contentLengthTab[1];
            send(header, message.out.toByteArray());
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "Send New Instance Message IO Exception", e);
        }
    }

    public void sendUpdateMessage(UpdateMessage message) {
        try {
            byte[] header = new byte[UPDATE_MESSAGE_HEADER_SIZE];
            byte[] idTab = fourBytesConversionBuffer.putInt(0, message.id).array();
            int contentLength = message.out.getSize();
            byte[] contentLengthTab = twoBytesConversionBuffer.putChar(0, (char) contentLength).array(); // conversion int/byte
            header[0] = BlueLink.UPDATE_MESSAGE;
            header[1] = idTab[0];
            header[2] = idTab[1];
            header[3] = idTab[2];
            header[4] = idTab[3];
            header[5] = contentLengthTab[0];
            header[6] = contentLengthTab[1];
            send(header, message.out.toByteArray());
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "Send Sync Message IO Exception", e);
        }
    }

    private synchronized void send(byte[]... bytes) throws IOException {
        for (byte[] b : bytes) {
            outputStream.write(b);
        }
    }
}

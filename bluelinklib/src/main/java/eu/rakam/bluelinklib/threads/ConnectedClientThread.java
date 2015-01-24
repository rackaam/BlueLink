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
import eu.rakam.bluelinklib.callbacks.OnNewSyncMessageCallback;
import eu.rakam.bluelinklib.callbacks.OnNewUserMessageCallback;

public class ConnectedClientThread extends Thread {

    private static final int MESSAGE_HEADER_SIZE = 3;

    private final OnNewUserMessageCallback userMessageCallback;
    private final OnNewSyncMessageCallback syncMessageCallback;

    private final BluetoothSocket socket;
    private final int clientId;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ByteBuffer twoBytesConversionBuffer = ByteBuffer.allocate(2);
    private ByteBuffer fourBytesConversionBuffer = ByteBuffer.allocate(4);

    /**
     * User by the client to communicate with the server.
     *
     * @param socket
     * @param userMessageCallback
     * @param syncMessageCallback
     */
    public ConnectedClientThread(BluetoothSocket socket, OnNewUserMessageCallback userMessageCallback,
                                 OnNewSyncMessageCallback syncMessageCallback) {
        this.userMessageCallback = userMessageCallback;
        this.syncMessageCallback = syncMessageCallback;
        this.socket = socket;
        this.clientId = 0;

        initStreams();
    }

    private void initStreams() {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "get streams error", e);
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
                    case BlueLink.NEW_INSTANCE_MESSAGE:
                        receiveNewInstanceMessage();
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "Read message IO Exception", e);
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
            if (userMessageCallback != null) {
                userMessageCallback.onMessage(clientId, new BlueLinkInputStream(buffer));
            }
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "ReceiveUserMessage Exception", e);
        }
    }

    private void receiveNewInstanceMessage() {
        try {
            byte byte0 = (byte) inputStream.read();
            byte byte1 = (byte) inputStream.read();
            byte byte2 = (byte) inputStream.read();
            byte byte3 = (byte) inputStream.read();
            fourBytesConversionBuffer.put(0, byte0);
            fourBytesConversionBuffer.put(1, byte1);
            fourBytesConversionBuffer.put(2, byte2);
            fourBytesConversionBuffer.put(3, byte3);
            int id = fourBytesConversionBuffer.getInt(0);

            byte0 = (byte) inputStream.read();
            byte1 = (byte) inputStream.read();
            twoBytesConversionBuffer.put(0, byte0);
            twoBytesConversionBuffer.put(1, byte1);
            int classNameLength = twoBytesConversionBuffer.getChar(0);

            byte0 = (byte) inputStream.read();
            byte1 = (byte) inputStream.read();
            twoBytesConversionBuffer.put(0, byte0);
            twoBytesConversionBuffer.put(1, byte1);
            int frameSize = twoBytesConversionBuffer.getChar(0);

            final byte[] classNameBuffer = new byte[classNameLength];
            int bytesRead = 0;
            while (bytesRead < classNameLength) {
                int offset = bytesRead;
                bytesRead += inputStream.read(classNameBuffer, offset, classNameLength - bytesRead);
            }
            String className = new String(classNameBuffer);

            final byte[] buffer = new byte[frameSize];
            bytesRead = 0;
            while (bytesRead < frameSize) {
                int offset = bytesRead;
                bytesRead += inputStream.read(buffer, offset, frameSize - bytesRead);
            }
            if (syncMessageCallback != null) {
                syncMessageCallback.onNewInstanceMessage(className, id, new BlueLinkInputStream(buffer));
            }
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "ReceiveUserMessage Exception", e);
        }
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
            int id = fourBytesConversionBuffer.getInt(0);
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
            if (syncMessageCallback != null) {
                syncMessageCallback.onUpdateMessage(id, new BlueLinkInputStream(buffer));
            }
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "ReceiveUserMessage Exception", e);
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

    private synchronized void send(byte[]... bytes) throws IOException {
        for (byte[] b : bytes) {
            outputStream.write(b);
        }
    }
}

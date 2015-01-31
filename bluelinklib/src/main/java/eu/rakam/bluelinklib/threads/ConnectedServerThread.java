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
import eu.rakam.bluelinklib.callbacks.OnNewUserMessageCallback;
import eu.rakam.bluelinklib.sync.messages.NewInstanceMessage;
import eu.rakam.bluelinklib.sync.messages.UpdateMessage;

public class ConnectedServerThread extends Thread {

    private static final int MESSAGE_HEADER_SIZE = 3;
    private static final int UPDATE_MESSAGE_HEADER_SIZE = 7;
    private static final int NEW_INSTANCE_MESSAGE_HEADER_SIZE = 9;

    private final OnNewUserMessageCallback userMessageCallback;
    private final BluetoothSocket socket;
    private final Client client;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ByteBuffer rTwoBytesConversionBuffer = ByteBuffer.allocate(2);
    private ByteBuffer sTwoBytesConversionBuffer = ByteBuffer.allocate(2);
    private ByteBuffer fourBytesConversionBuffer = ByteBuffer.allocate(4);

    /**
     * Used by the server to communicate with the clients.
     *
     * @param client
     * @param userMessageCallback
     */
    public ConnectedServerThread(Client client, OnNewUserMessageCallback userMessageCallback) {
        this.userMessageCallback = userMessageCallback;
        this.socket = client.getSocket();
        this.client = client;

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
            rTwoBytesConversionBuffer.put(0, byte0);
            rTwoBytesConversionBuffer.put(1, byte1);
            int frameSize = rTwoBytesConversionBuffer.getChar(0);
            final byte[] buffer = new byte[frameSize];
            int bytesRead = 0;
            while (bytesRead < frameSize) {
                int offset = bytesRead;
                bytesRead += inputStream.read(buffer, offset, frameSize - bytesRead);
            }
            if (userMessageCallback != null) {
                userMessageCallback.onMessage(client.getId(), new BlueLinkInputStream(buffer));
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
            byte[] contentLengthTab = sTwoBytesConversionBuffer.putChar(0, (char) contentLength).array(); // conversion int/byte
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
            byte[] contentLengthTab = sTwoBytesConversionBuffer.putChar(0, (char) contentLength).array().clone(); // conversion int/byte
            header[0] = BlueLink.NEW_INSTANCE_MESSAGE;
            header[1] = idTab[0];
            header[2] = idTab[1];
            header[3] = idTab[2];
            header[4] = idTab[3];

            byte[] classNameLengthTab = sTwoBytesConversionBuffer.putChar(0, (char) message.className.getBytes().length).array();
            header[5] = classNameLengthTab[0];
            header[6] = classNameLengthTab[1];

            header[7] = contentLengthTab[0];
            header[8] = contentLengthTab[1];
            send(header, message.className.getBytes(), message.out.toByteArray());
        } catch (IOException e) {
            Log.e(BlueLink.TAG, "Send New Instance Message IO Exception", e);
        }
    }

    public void sendUpdateMessage(UpdateMessage message) {
        try {
            byte[] header = new byte[UPDATE_MESSAGE_HEADER_SIZE];
            byte[] idTab = fourBytesConversionBuffer.putInt(0, message.id).array();
            int contentLength = message.out.getSize();
            byte[] contentLengthTab = sTwoBytesConversionBuffer.putChar(0, (char) contentLength).array(); // conversion int/byte
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

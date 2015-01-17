package eu.rakam.bluelinklib;

import java.nio.ByteBuffer;

public class BlueLinkInputStream {

    private ByteBuffer buffer;

    public BlueLinkInputStream(byte[] bytes) {
        this.buffer = ByteBuffer.wrap(bytes);
    }

    public byte readByte() {
        return buffer.get();
    }

    public boolean readBoolean() {
        return buffer.get() == 1;
    }

    public char readChar() {
        return buffer.getChar();
    }

    public short readShort() {
        return buffer.getShort();
    }

    public int readInt() {
        return buffer.getInt();
    }

    public float readFloat() {
        return buffer.getFloat();
    }

    public long readLong() {
        return buffer.getLong();
    }

    public double readDouble() {
        return buffer.getDouble();
    }

    public String readString() {
        char len = readChar();
        byte[] bytes = new byte[len];
        buffer.get(bytes, 0, len);
        return new String(bytes);
    }
}

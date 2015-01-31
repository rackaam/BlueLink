package eu.rakam.bluelinklib;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BlueLinkOutputStream {

    private static final String TAG = "BlueLinkOutputStream";

    private ByteBuffer twoBytesConversionBuffer = ByteBuffer.allocate(2);
    private ByteBuffer fourBytesConversionBuffer = ByteBuffer.allocate(4);
    private ByteBuffer eightBytesConversionBuffer = ByteBuffer.allocate(8);
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public BlueLinkOutputStream writeByte(byte b) {
        outputStream.write(b);
        return this;
    }

    public BlueLinkOutputStream writeBoolean(boolean b) {
        outputStream.write(b ? 1 : 0);
        return this;
    }

    public BlueLinkOutputStream writeChar(char c) {
        try {
            outputStream.write(twoBytesConversionBuffer.putChar(0, c).array());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return this;
    }

    public BlueLinkOutputStream writeShort(short s) {
        try {
            outputStream.write(twoBytesConversionBuffer.putShort(0, s).array());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public BlueLinkOutputStream writeInt(int i) {
        try {
            outputStream.write(fourBytesConversionBuffer.putInt(0, i).array());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return this;
    }

    public BlueLinkOutputStream writeFloat(float f) {
        try {
            outputStream.write(fourBytesConversionBuffer.putFloat(0, f).array());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return this;
    }

    public BlueLinkOutputStream writeLong(long l) {
        try {
            outputStream.write(eightBytesConversionBuffer.putLong(0, l).array());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return this;
    }

    public BlueLinkOutputStream writeDouble(double d) {
        try {
            outputStream.write(eightBytesConversionBuffer.putDouble(0, d).array());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return this;
    }

    public BlueLinkOutputStream writeString(String s) {
        if (s == null)
            return this;
        try {
            // Write a char before the string to indicate the string length
            writeChar((char) s.length());
            outputStream.write(s.getBytes());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return this;
    }

    public byte[] toByteArray() {
        return outputStream.toByteArray();
    }

    public int getSize() {
        return outputStream.size();
    }
}

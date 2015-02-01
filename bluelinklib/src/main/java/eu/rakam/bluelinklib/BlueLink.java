package eu.rakam.bluelinklib;

public class BlueLink {

    public static final String TAG = "BlueLinkDebug";

    public static final byte PROTOCOL_VERSION = 1;
    public static final int ENABLE_BLUETOOTH = 22000;

    /* Message types */
    public static final byte NEW_INSTANCE_MESSAGE = 0;
    public static final byte UPDATE_MESSAGE = 1;
    public static final byte USER_MESSAGE = 2;
    public static final byte KEEP_ALIVE_MESSAGE = 3;

    private static int idCount;

    public static int getID() {
        idCount++;
        return idCount;
    }
}

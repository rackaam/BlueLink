package eu.rakam.bluelinklib.callbacks;

import java.io.IOException;

public interface OnConnectToServerCallback {
    /**
     * Called when connected to the server.
     *
     * @param e null if connected
     */
    public void onConnect(IOException e);
}

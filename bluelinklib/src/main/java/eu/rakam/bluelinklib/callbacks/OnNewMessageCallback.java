package eu.rakam.bluelinklib.callbacks;

import eu.rakam.bluelinklib.BlueLinkInputStream;

public interface OnNewMessageCallback {
    /**
     * Called every time a new message is received.
     *
     * @param senderID ID of the sender (0 for the server, otherwise <code>Client.getID()</code>
     * @param message
     */
    public void onNewMessage(int senderID, BlueLinkInputStream message);
}

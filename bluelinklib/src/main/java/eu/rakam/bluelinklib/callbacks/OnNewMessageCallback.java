package eu.rakam.bluelinklib.callbacks;

import eu.rakam.bluelinklib.BlueLinkInputStream;

public interface OnNewMessageCallback {
    /**
     * Called every time a new message is received.
     *
     * @param message
     */
    public void onNewMessage(BlueLinkInputStream message);
}

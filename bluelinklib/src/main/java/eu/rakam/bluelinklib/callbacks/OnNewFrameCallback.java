package eu.rakam.bluelinklib.callbacks;

import eu.rakam.bluelinklib.BlueLinkInputStream;

public interface OnNewFrameCallback {
    public void onNewMessage(int senderID, byte messageType, BlueLinkInputStream in);

    public void onNewInstanceMessage(String className, int ID, BlueLinkInputStream in);

    public void onNewSyncMessage(int ID, BlueLinkInputStream in);
}

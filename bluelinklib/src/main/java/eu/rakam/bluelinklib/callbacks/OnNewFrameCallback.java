package eu.rakam.bluelinklib.callbacks;

import eu.rakam.bluelinklib.BlueLinkInputStream;

public interface OnNewFrameCallback {
    public void onNewFrame(int senderID, byte messageType, BlueLinkInputStream in);
}

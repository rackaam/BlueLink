package eu.rakam.bluelinklib.callbacks;

import eu.rakam.bluelinklib.BlueLinkInputStream;

public interface OnNewUserMessageCallback {

    public void onMessage(int senderID, BlueLinkInputStream in);
}

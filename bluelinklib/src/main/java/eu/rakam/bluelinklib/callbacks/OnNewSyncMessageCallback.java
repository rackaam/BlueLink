package eu.rakam.bluelinklib.callbacks;

import eu.rakam.bluelinklib.BlueLinkInputStream;

public interface OnNewSyncMessageCallback {

    public void onNewInstanceMessage(String className, int ID, BlueLinkInputStream in);

    public void onUpdateMessage(int ID, BlueLinkInputStream in);
}

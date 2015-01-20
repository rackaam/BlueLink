package eu.rakam.bluelinklib.callbacks;

import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.Client;

public interface OnNewClientCallback {
    public void onNewClient(Client client, BlueLinkInputStream in);
}

package eu.rakam.bluelinklib.callbacks;

import eu.rakam.bluelinklib.Client;

public interface OnOpenServerCallback {

    /**
     * Called when the server finished starting.
     *
     * @param e null if the name is open
     */
    public void onOpen(Exception e);

    /**
     * Called every time a player connects to the server.
     *
     * @param client the new player
     */
    public void onNewClient(Client client);
}

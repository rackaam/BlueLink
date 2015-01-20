package eu.rakam.bluelinklib.callbacks;

import eu.rakam.bluelinklib.BlueLinkInputStream;
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
     * @param in     the data sent by the client in {@link eu.rakam.bluelinklib.BlueLinkClient#connectToServer(eu.rakam.bluelinklib.Server, eu.rakam.bluelinklib.BlueLinkOutputStream, OnConnectToServerCallback)}.
     *               Can be null
     */
    public void onNewClient(Client client, BlueLinkInputStream in);
}

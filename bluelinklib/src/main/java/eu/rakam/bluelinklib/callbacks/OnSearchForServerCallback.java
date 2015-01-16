package eu.rakam.bluelinklib.callbacks;

import java.util.List;

import eu.rakam.bluelinklib.Server;

public interface OnSearchForServerCallback {

    /**
     * Called when the search begins
     */
    public void onSearchStarted();

    /**
     * Called each time a new server is found.
     *
     * @param server The new server
     */
    public void onNewServer(Server server);

    /**
     * Called when the search ended.
     *
     * @param servers List of all the servers found
     */
    public void onSearchFinished(List<Server> servers);
}

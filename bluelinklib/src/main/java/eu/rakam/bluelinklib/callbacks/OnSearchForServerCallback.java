package eu.rakam.bluelinklib.callbacks;

import java.util.List;

import eu.rakam.bluelinklib.Server;

public interface OnSearchForServerCallback {

    public void onSearchStarted();

    public void onNewServer(Server server);

    public void onSearchFinished(List<Server> servers);
}

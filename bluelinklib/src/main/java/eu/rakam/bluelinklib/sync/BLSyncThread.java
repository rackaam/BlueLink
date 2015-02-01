package eu.rakam.bluelinklib.sync;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import eu.rakam.bluelinklib.BlueLinkOutputStream;
import eu.rakam.bluelinklib.BlueLinkServer;
import eu.rakam.bluelinklib.sync.messages.KeepAliveMessage;
import eu.rakam.bluelinklib.sync.messages.Message;
import eu.rakam.bluelinklib.sync.messages.NewInstanceMessage;
import eu.rakam.bluelinklib.sync.messages.UpdateMessage;

public class BLSyncThread extends Thread {

    private BlueLinkServer blueLinkServer;
    private LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private KeepAliveMessage keepAliveMessage = new KeepAliveMessage();

    public BLSyncThread(BlueLinkServer blueLinkServer) {
        this.blueLinkServer = blueLinkServer;
    }

    public void syncNewInstance(BLSynchronizable synchronizable, BlueLinkOutputStream out) {
        NewInstanceMessage message = new NewInstanceMessage(synchronizable.getSynchronizableId(), synchronizable.getClass().getName(), out);
        queue.add(message);
    }

    public void sync(BLSynchronizable synchronizable) {
        UpdateMessage message = new UpdateMessage(synchronizable.getSynchronizableId(), synchronizable.getDataToSync());
        queue.add(message);
    }

    public void sync(BLSynchronizable synchronizable, BlueLinkOutputStream out) {
        UpdateMessage message = new UpdateMessage(synchronizable.getSynchronizableId(), out);
        queue.add(message);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Message m = queue.poll(100, TimeUnit.MILLISECONDS);
                if (m != null)
                    blueLinkServer.broadcastSyncMessage(m);
                else
                    blueLinkServer.broadcastSyncMessage(keepAliveMessage);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

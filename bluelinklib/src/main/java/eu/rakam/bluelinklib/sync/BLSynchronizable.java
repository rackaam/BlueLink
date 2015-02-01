package eu.rakam.bluelinklib.sync;

import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.BlueLinkOutputStream;

public interface BLSynchronizable {

    /**
     * Returns an unique ID (for all the devices connected).
     * If your game doesn't dispose of an unique ID system, you can use the {@link eu.rakam.bluelinklib.BlueLink#getID()} method.
     *
     * @return an unique ID
     */
    public int getSynchronizableId();

    /**
     * Called when {@link eu.rakam.bluelinklib.BlueLinkServer#sync(BLSynchronizable)} is used to get the data to synchronize.
     *
     * @return
     */
    public BlueLinkOutputStream getDataToSync();

    /**
     * Called when a client receive data to synchronize.
     * Reads data from {@code foo} and update the instance state.
     *
     * @param in
     */
    public void syncData(BlueLinkInputStream in);
}

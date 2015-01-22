package eu.rakam.bluelinklib.sync;

import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.BlueLinkOutputStream;

public interface BLSynchronizable {

    public int getSynchronizableId();

    public BlueLinkOutputStream getDataToSync();

    public void syncData(BlueLinkInputStream in);
}

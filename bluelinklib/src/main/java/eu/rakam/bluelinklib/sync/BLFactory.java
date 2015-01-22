package eu.rakam.bluelinklib.sync;

import eu.rakam.bluelinklib.BlueLinkInputStream;

public abstract class BLFactory {

    public abstract BLSynchronizable instantiate(String className, BlueLinkInputStream in);

    public interface Command {
        BLSynchronizable instantiate(BlueLinkInputStream in);
    }
}

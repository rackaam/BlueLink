package eu.rakam.bluelinklib.sync.messages;

import eu.rakam.bluelinklib.BlueLink;
import eu.rakam.bluelinklib.BlueLinkOutputStream;

public class NewInstanceMessage implements Message {
    public final int id;
    public final String className;
    public final BlueLinkOutputStream out;

    public NewInstanceMessage(int id, String className, BlueLinkOutputStream out) {
        this.id = id;
        this.className = className;
        this.out = out;
    }

    @Override
    public byte getType() {
        return BlueLink.NEW_INSTANCE_MESSAGE;
    }
}

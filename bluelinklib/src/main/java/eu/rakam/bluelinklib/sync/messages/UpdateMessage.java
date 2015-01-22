package eu.rakam.bluelinklib.sync.messages;

import eu.rakam.bluelinklib.BlueLink;
import eu.rakam.bluelinklib.BlueLinkOutputStream;

public class UpdateMessage implements Message {
    public final int id;
    public final BlueLinkOutputStream out;

    public UpdateMessage(int id, BlueLinkOutputStream out) {
        this.id = id;
        this.out = out;
    }

    @Override
    public byte getType() {
        return BlueLink.UPDATE_MESSAGE;
    }
}

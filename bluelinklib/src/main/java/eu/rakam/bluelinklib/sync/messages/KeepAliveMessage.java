package eu.rakam.bluelinklib.sync.messages;

import eu.rakam.bluelinklib.BlueLink;

public class KeepAliveMessage implements Message {

    @Override
    public byte getType() {
        return BlueLink.KEEP_ALIVE_MESSAGE;
    }
}

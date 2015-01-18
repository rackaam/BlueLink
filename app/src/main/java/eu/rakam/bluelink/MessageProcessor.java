package eu.rakam.bluelink;

import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.callbacks.OnNewMessageCallback;

public class MessageProcessor implements OnNewMessageCallback {

    MainActivity mainActivity;

    public MessageProcessor(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onNewMessage(BlueLinkInputStream message) {
        String str = message.readString();
        int i = message.readInt();
        float f = message.readFloat();
        mainActivity.log(str + " " + i + " " + f);
    }
}

package eu.rakam.bluelink.pong;

import android.util.Log;

import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.callbacks.OnNewMessageCallback;

public class MessageProcessor implements OnNewMessageCallback {

    public static final byte CONTROLLER_STATE = 0;

    private Model model;

    public MessageProcessor(Model model) {
        this.model = model;
    }

    @Override
    public void onNewMessage(int senderID, BlueLinkInputStream message) {
        Log.d(PongActivity.TAG, "New message");
        byte type = message.readByte();
        switch (type) {
            case CONTROLLER_STATE:
                receiverControllerState(message);
                break;
            default:
                break;
        }
    }

    private void receiverControllerState(BlueLinkInputStream message) {
        Log.d(PongActivity.TAG, "New controler message");
        model.controllerStateB = message.readInt();
    }
}

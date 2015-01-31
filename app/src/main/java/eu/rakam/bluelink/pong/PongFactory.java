package eu.rakam.bluelink.pong;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.sync.BLFactory;
import eu.rakam.bluelinklib.sync.BLSynchronizable;

public class PongFactory extends BLFactory {

    private Model model;
    private final Map<String, Command> commands = new HashMap<>();

    public PongFactory(Model m, final int screenH, final int screenW) {
        this.model = m;
        commands.put(Paddle.class.getName(), new Command() {
            @Override
            public BLSynchronizable instantiate(BlueLinkInputStream in) {
                boolean isLeft = in.readBoolean();
                int pos = in.readInt();
                if (isLeft)
                    model.leftPaddle = new Paddle(screenH, screenW, pos);
                else
                    model.rightPaddle = new Paddle(screenH, screenW, pos);
                return model.rightPaddle;
            }
        });
    }

    @Override
    public BLSynchronizable instantiate(String className, BlueLinkInputStream in) {
        Log.d("ca-bug", className + "");
        Command command = commands.get(className);
        if(command != null)
            return command.instantiate(in);
        return null;
    }
}

package eu.rakam.bluelink;

import java.util.HashMap;
import java.util.Map;

import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.sync.BLFactory;
import eu.rakam.bluelinklib.sync.BLSynchronizable;

public class Factory extends BLFactory {

    private final Map<String, Command> commands = new HashMap<>();

    public Factory() {
        commands.put(Square.class.getName(), new Command() {
            @Override
            public BLSynchronizable instantiate(BlueLinkInputStream in) {
                int x = in.readInt();
                int y = in.readInt();
                int w = in.readInt();
                int h = in.readInt();
                return new Square(x, y, w, h, 0, 0, 0);
            }
        });
    }

    @Override
    public BLSynchronizable instantiate(String className, BlueLinkInputStream in) {
        return commands.get(className).instantiate(in);
    }


}

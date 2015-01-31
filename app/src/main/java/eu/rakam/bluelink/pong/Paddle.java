package eu.rakam.bluelink.pong;

import eu.rakam.bluelinklib.BlueLink;
import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.BlueLinkOutputStream;
import eu.rakam.bluelinklib.BlueLinkServer;
import eu.rakam.bluelinklib.sync.BLSynchronizable;

public class Paddle implements BLSynchronizable {

    public static final float WIDTH_RATIO = 0.02f;
    public static final float HEIGHT_RATIO = 0.2f;
    public static final float VELOCITY = 1f;

    private BlueLinkServer server;
    private int screenHeight;
    private int width;
    private int midHeight;
    private int pos, minPos, maxPos;
    private int y;

    public Paddle(int screenH, int screenW, int pos) {
        init(screenH, screenW, pos);
    }

    public Paddle(BlueLinkServer server, int screenHeight, int screenW, int pos, boolean isLeft) {
        this.server = server;
        init(screenHeight, screenW, pos);
        server.syncNewInstance(this, new BlueLinkOutputStream().writeBoolean(isLeft).writeInt(y));
    }

    private void init(int screenH, int screenW, int pos) {
        this.screenHeight = screenH;
        this.width = (int) (screenW * WIDTH_RATIO);
        int h = (int) (screenH * HEIGHT_RATIO);
        this.midHeight = (int) (h / 2f);
        this.pos = pos;
        this.minPos = (int) (midHeight / (screenHeight / 100f));
        this.maxPos = (int) ((screenHeight - midHeight) / (screenHeight / 100f));
        this.y = (int) (screenHeight * pos / 100f);
    }

    public int getWidth() {
        return width;
    }

    public int getBottom() {
        return y - midHeight;
    }

    public int getTop() {
        return y + midHeight;
    }

    public int getPos() {
        return pos;
    }

    public int getMaxPos() {
        return maxPos;
    }

    public int getMinPos() {
        return minPos;
    }

    public int getY() {
        return y;
    }

    public void setPos(int p) {
        this.pos = p;
        this.y = (int) (screenHeight * p / 100f);
        server.sync(this, new BlueLinkOutputStream().writeInt(p));
    }

    @Override
    public int getSynchronizableId() {
        return BlueLink.getID();
    }

    @Override
    public BlueLinkOutputStream getDataToSync() {
        return null;
    }

    @Override
    public void syncData(BlueLinkInputStream in) {
        y = (int) (screenHeight * in.readInt() / 100f);
    }
}

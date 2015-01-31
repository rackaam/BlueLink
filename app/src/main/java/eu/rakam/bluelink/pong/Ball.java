package eu.rakam.bluelink.pong;

import eu.rakam.bluelinklib.BlueLink;
import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.BlueLinkOutputStream;
import eu.rakam.bluelinklib.BlueLinkServer;
import eu.rakam.bluelinklib.sync.BLSynchronizable;

public class Ball implements BLSynchronizable {

    public static final float SIZE_RATIO = 0.1f;
    public static final float VELOCITY = 1f;

    private BlueLinkServer server;
    private int size;
    private int x, y;
    private int vX, vY;

    public Ball(BlueLinkServer server, int size, int x, int y) {
        this.server = server;
        this.size = size;
        this.x = x;
        this.y = y;
        server.syncNewInstance(this, new BlueLinkOutputStream().writeInt(x).writeInt(y));
    }

    public int getSize() {
        return size;
    }

    public int getvX() {
        return vX;
    }

    public int getvY() {
        return vY;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setvX(int vX) {
        this.vX = vX;
    }

    public void setvY(int vY) {
        this.vY = vY;
    }

    public void setXY(int x, int y) {
        this.x = x;
        this.y = y;
        server.sync(this, new BlueLinkOutputStream().writeInt(x).writeInt(y));
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
        x = in.readInt();
        y = in.readInt();
    }
}

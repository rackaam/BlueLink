package eu.rakam.bluelink;

import eu.rakam.bluelinklib.BlueLink;
import eu.rakam.bluelinklib.BlueLinkInputStream;
import eu.rakam.bluelinklib.BlueLinkOutputStream;
import eu.rakam.bluelinklib.BlueLinkServer;
import eu.rakam.bluelinklib.sync.BLSynchronizable;

public class Square implements BLSynchronizable {

    private BlueLinkServer server;

    private int id;
    private int x, y;
    private int w, h;
    private int r, g, b;

    public Square(BlueLinkServer server, int x, int y, int w, int h, int r, int g, int b) {
        init(x, y, w, h, r, g, b);
        this.id = BlueLink.getID();

        BlueLinkOutputStream out = new BlueLinkOutputStream();
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(w);
        out.writeInt(h);
        server.syncNewInstance(this, out);
    }

    public Square(int x, int y, int w, int h, int r, int g, int b) {
        init(x, y, w, h, r, g, b);
    }

    private void init(int x, int y, int w, int h, int r, int g, int b) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setRGB(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    @Override
    public int getSynchronizableId() {
        return id;
    }

    @Override
    public BlueLinkOutputStream getDataToSync() {
        BlueLinkOutputStream out = new BlueLinkOutputStream();
        out.writeInt(r);
        out.writeInt(g);
        out.writeInt(b);
        return out;
    }

    @Override
    public void syncData(BlueLinkInputStream in) {
        r = in.readInt();
        g = in.readInt();
        b = in.readInt();
    }
}

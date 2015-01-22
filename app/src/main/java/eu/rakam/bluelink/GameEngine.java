package eu.rakam.bluelink;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

import eu.rakam.bluelinklib.BlueLinkClient;
import eu.rakam.bluelinklib.BlueLinkServer;

public class GameEngine {

    private static final int ROW = 10;
    private static final int COLUMN = 10;

    private Activity activity;
    private BlueLinkServer server;
    private BlueLinkClient client;

    private List<Square> squares = new ArrayList<>();

    public GameEngine(BlueLinkServer server) {
        this.server = server;
    }

    public GameEngine(BlueLinkClient client) {
        this.client = client;
    }

    private void initGame() {
        float width = activity.getResources().getDimensionPixelSize(R.dimen.surfaceSize);
        int squareH = (int) (width / ROW);
        int squareW = (int) (width / COLUMN);
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COLUMN; j++) {
                Square square = new Square(server, j * squareW, i * squareH, squareW, squareH, 0, 0, 0);
            }
        }
    }

}

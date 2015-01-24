package eu.rakam.bluelink;

import android.app.Activity;

import eu.rakam.bluelinklib.BlueLinkServer;

public class Engine {

    private static final int ROW = 10;
    private static final int COLUMN = 10;

    private Activity activity;
    private BlueLinkServer server;
    private Model model;

    public Engine(Activity activity, BlueLinkServer server, Model model) {
        this.activity = activity;
        this.server = server;
        this.model = model;
        initGame();
    }

    private void initGame() {
        float width = activity.getResources().getDimensionPixelSize(R.dimen.surfaceSize);
        int squareH = (int) (width / ROW);
        int squareW = (int) (width / COLUMN);
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COLUMN; j++) {
                model.squares.add(new Square(server, j * squareW, i * squareH, squareW, squareH, 0, 0, 0));
            }
        }

    }

    public void update() {
        for (Square s : model.squares) {
            s.setRGB(random255(), random255(), random255());
        }
    }

    private static int random255() {
        return (int) (Math.random() * 255);
    }


//    class EngineThread extends Thread {
//
//        GridSurfaceView surfaceView;
//        private boolean running = false;
//
//        public EngineThread(GridSurfaceView view) {
//            surfaceView = view;
//        }
//
//        public void setRunning(boolean run) {
//            running = run;
//        }
//
//        @Override
//        public void run() {
//            while (running) {
//                Canvas canvas = surfaceView.getHolder().lockCanvas();
//
//                if (canvas != null) {
//                    synchronized (surfaceView.getHolder()) {
//                        surfaceView.drawSquares(canvas);
//                    }
//                    surfaceView.getHolder().unlockCanvasAndPost(canvas);
//                }
//
//                try {
//                    sleep(1500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

}

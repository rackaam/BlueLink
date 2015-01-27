package eu.rakam.bluelink.squares;


import android.app.Activity;
import android.graphics.Canvas;

public class RenderingThread extends Thread {

    private final Activity activity;
    private final GridSurfaceView surfaceView;
    private boolean running = true;

    public RenderingThread(Activity activity, GridSurfaceView surfaceView) {
        this.activity = activity;
        this.surfaceView = surfaceView;
    }

    public void setRunning(boolean run) {
        running = run;
    }

    @Override
    public void run() {
        while (running) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Canvas canvas = surfaceView.getHolder().lockCanvas();
                    if (canvas != null) {
                        surfaceView.drawSquares(canvas);
                        surfaceView.getHolder().unlockCanvasAndPost(canvas);
                    }
                }
            });
            try {
                sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

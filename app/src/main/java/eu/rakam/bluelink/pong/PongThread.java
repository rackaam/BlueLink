package eu.rakam.bluelink.pong;

import android.app.Activity;
import android.graphics.Canvas;

public class PongThread extends Thread {

    private Activity activity;
    private PongEngine engine;
    private PongSurfaceView surfaceView;

    public PongThread(Activity activity, PongSurfaceView surfaceView, PongEngine engine) {
        this.activity = activity;
        this.engine = engine;
        this.surfaceView = surfaceView;
    }

    @Override
    public void run() {
        while (true) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Canvas canvas = surfaceView.getHolder().lockCanvas();
                    if (canvas != null) {
                        surfaceView.render(canvas);
                        surfaceView.getHolder().unlockCanvasAndPost(canvas);
                    }
                }
            });
            if (engine != null)
                engine.update();
            try {
                sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

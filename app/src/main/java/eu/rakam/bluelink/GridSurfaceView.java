package eu.rakam.bluelink;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GridSurfaceView extends SurfaceView implements SurfaceHolder.Callback {


    private GridThread thread;
    private SurfaceHolder surfaceHolder;
    private Paint paint;

    public GridSurfaceView(Context context) {
        super(context);
        init();
    }

    public GridSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        thread = new GridThread(this);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }


    private void drawSquares(Canvas canvas) {
        // todo
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    private class GridThread extends Thread {

        GridSurfaceView surfaceView;
        private boolean running = false;

        public GridThread(GridSurfaceView view) {
            surfaceView = view;
        }

        public void setRunning(boolean run) {
            running = run;
        }

        @Override
        public void run() {
            while (running) {
                Canvas canvas = surfaceView.getHolder().lockCanvas();

                if (canvas != null) {
                    synchronized (surfaceView.getHolder()) {
                        surfaceView.drawSquares(canvas);
                    }
                    surfaceView.getHolder().unlockCanvasAndPost(canvas);
                }

                try {
                    sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

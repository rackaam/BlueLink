package eu.rakam.bluelink;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GridSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder surfaceHolder;
    private Paint paint;
    private Model model;

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

    public void setModel(Model model) {
        this.model = model;
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    public void drawSquares(Canvas canvas) {
        for (Square s : model.squares) {
            paint.setColor(Color.rgb(s.getR(), s.getG(), s.getB()));
            canvas.drawRect(s.getX(), s.getY(), s.getX() + s.getW(), s.getY() + s.getH(), paint);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

}

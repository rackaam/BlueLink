package eu.rakam.bluelink.pong;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;

import eu.rakam.bluelinklib.BlueLinkClient;
import eu.rakam.bluelinklib.BlueLinkOutputStream;

public class PongSurfaceView extends SurfaceView {

    public final static int STATE_RELEASED = 0;
    public final static int STATE_BOTTOM = 1;
    public final static int STATE_UP = 2;

    private int state = STATE_RELEASED;
    private BlueLinkClient blueLinkClient;
    private Model model;
    private Paint paint;

    public PongSurfaceView(Context context) {
        super(context);
        init();
    }

    public PongSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PongSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (event.getY() < getHeight() / 2.0f) {
                state = STATE_BOTTOM;
            } else {
                state = STATE_UP;
            }
            syncStateWithEngine();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            state = STATE_RELEASED;
            syncStateWithEngine();
        }
        return true;
    }

    private void syncStateWithEngine() {
        Log.d(PongActivity.TAG, "SYNC ENGINE");
        if (blueLinkClient != null) {
            blueLinkClient.sendMessage(new BlueLinkOutputStream()
                    .writeByte(MessageProcessor.CONTROLLER_STATE).writeInt(state));
            Log.d(PongActivity.TAG, "SYNC ENGINE OK");
        } else if (model != null) {
            model.controllerStateA = state;
        }
    }

    public void render(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        if (model.leftPaddle != null)
            canvas.drawRect(0, model.leftPaddle.getTop(), model.leftPaddle.getWidth(), model.leftPaddle.getBottom(), paint);
        if (model.rightPaddle != null)
            canvas.drawRect(getWidth() - model.rightPaddle.getWidth(), model.rightPaddle.getTop(), getWidth(), model.rightPaddle.getBottom(), paint);
    }

    public void setBlueLinkClient(BlueLinkClient blueLinkClient) {
        this.blueLinkClient = blueLinkClient;
    }

    public void setModel(Model model) {
        this.model = model;
    }
}

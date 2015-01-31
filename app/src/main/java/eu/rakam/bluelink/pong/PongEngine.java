package eu.rakam.bluelink.pong;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;

import eu.rakam.bluelinklib.BlueLinkServer;

public class PongEngine {

    private Activity activity;
    private BlueLinkServer server;
    private Model model;

    private int w, h;

    public PongEngine(Activity activity, BlueLinkServer server, Model model) {
        this.activity = activity;
        this.server = server;
        this.model = model;
        initGame();
    }

    private void initGame() {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        w = size.x;
        h = size.y;

        model.leftPaddle = new Paddle(server, h, w, 50, true);
        model.rightPaddle = new Paddle(server, h, w, 50, false);
//        model.ball = new Ball(server, (int) (h * Ball.SIZE_RATIO), 50, 50);
    }

    public void update() {
        if (model.controllerStateA == PongSurfaceView.STATE_BOTTOM) {
            if (model.leftPaddle.getPos() > model.leftPaddle.getMinPos()) {
                model.leftPaddle.setPos(model.leftPaddle.getPos() - 1);
            }
        }
        if (model.controllerStateA == PongSurfaceView.STATE_UP) {
            if (model.leftPaddle.getPos() < model.leftPaddle.getMaxPos()) {
                model.leftPaddle.setPos(model.leftPaddle.getPos() + 1);
            }
        }
        if (model.controllerStateB == PongSurfaceView.STATE_BOTTOM) {
            if (model.rightPaddle.getPos() > model.rightPaddle.getMinPos()) {
                model.rightPaddle.setPos(model.rightPaddle.getPos() - 1);
            }
        }
        if (model.controllerStateB == PongSurfaceView.STATE_UP) {
            if (model.rightPaddle.getPos() < model.rightPaddle.getMaxPos()) {
                model.rightPaddle.setPos(model.rightPaddle.getPos() + 1);
            }
        }
        /*
        if (model.ball.getX() < model.leftPaddle.getWidth()
                && model.ball.getY() < model.leftPaddle.getY() + model.leftPaddle.getBottom()
                && model.ball.getY() + model.ball.getSize() > model.leftPaddle.getBottom()) {
            // Collision with the left paddle
            model.ball.setvX(model.ball.getvX() * -1);
        } else if (model.ball.getX() + model.ball.getSize() > w - model.rightPaddle.getWidth()
                && model.ball.getY() < model.rightPaddle.getY() + model.rightPaddle.getBottom()
                && model.ball.getY() + model.ball.getSize() > model.rightPaddle.getBottom()) {
            // Collision with the right paddle
            model.ball.setvX(model.ball.getvX() * -1);
        } else if (model.ball.getY() < 0
                || model.ball.getY() + model.ball.getSize() > h) {
            model.ball.setvY(model.ball.getvY() * -1);
        }

        int newX = (int) (model.ball.getX() + model.ball.getvX() * Ball.VELOCITY);
        int newY = (int) (model.ball.getY() + model.ball.getvY() * Ball.VELOCITY);
        model.ball.setXY(newX, newY);
        */
    }

}

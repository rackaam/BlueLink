package eu.rakam.bluelink;

public class EngineThread extends Thread {

    private final Engine engine;
    private boolean running = true;

    public EngineThread(Engine engine) {
        this.engine = engine;
    }

    public void setRunning(boolean run) {
        running = run;
    }

    @Override
    public void run() {
        while (running) {
            engine.update();
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

package eu.rakam.bluelinklib.callbacks;

public interface OnOpenServerCallback {

    /**
     * Called when the server finished starting
     *
     * @param e null if the name is open
     */
    public void onFinished(Exception e);
}

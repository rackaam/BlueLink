package eu.rakam.bluelinklib.callbacks;

import java.io.IOException;

public interface OnTurnOnBluetoothCallback {

    /**
     * Called when bluetooth starting up is done
     *
     * @param e null if bluetooth is ON
     */
    public void onBluetoothOn(IOException e);
}

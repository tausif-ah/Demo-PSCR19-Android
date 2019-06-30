package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;

public interface DataReceivedHandler {

    /**
     * Called when a piece of data is received from a neighbor.
     *
     * @param src  name of the sender.
     * @param dst  name of the target.
     * @param data data received.
     * @param initiator initiator of the action.
     */
    void dataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, @NonNull String initiator);

}

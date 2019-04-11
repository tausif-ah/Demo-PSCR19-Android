package nist.p_70nanb17h188.demo.pscr19.link;

public interface DataReceivedHandler {

    /**
     * Called when a piece of data is received from a neighbor.
     *
     * @param id id of the sender neighbor.
     * @param data data received.
     */
    void dataReceived(NeighborID id, byte[] data);
}

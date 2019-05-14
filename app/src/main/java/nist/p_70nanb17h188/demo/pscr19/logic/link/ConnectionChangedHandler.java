package nist.p_70nanb17h188.demo.pscr19.logic.link;

public interface ConnectionChangedHandler {

    /**
     * Called when a connection is established or lost.
     *
     * @param id id of the neighbor on the other side of the connection.
     * @param connected if the event is connected or disconnected.
     */
    void connectionChanged(NeighborID id, boolean connected);
}

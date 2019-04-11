package nist.p_70nanb17h188.demo.pscr19.link;

import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.Device;

final class LinkLayer_Impl {

    private final HashSet<ConnectionChangedHandler> connectionHandlers = new HashSet<>();
    private final HashSet<DataReceivedHandler> dataHandlers = new HashSet<>();

    /**
     * Singleton pattern, prevent the class to be instantiated by the others.
     */
    LinkLayer_Impl() {
        System.out.printf("LinkLayer_Impl on %s initialized!%n", Device.getName());
    }

    public boolean addConnectionHandler(ConnectionChangedHandler h) {
        return connectionHandlers.add(h);
    }

    public boolean removeConnectionHandler(ConnectionChangedHandler h) {
        return connectionHandlers.remove(h);
    }

    public boolean addDataReceivedHandler(DataReceivedHandler h) {
        return dataHandlers.add(h);
    }

    public boolean removeDataReceivedHandler(DataReceivedHandler h) {
        return dataHandlers.remove(h);
    }

    public boolean sendData(NeighborID id, byte[] data, int start, int len) {
        // Do not forget to flush stream after each send, when in TCP
        return false;
    }

    /**
     * Example function on forwarding a data to all the handlers.
     *
     * @param id neighbor id.
     * @param data data received.
     */
    private void onDataReceived(NeighborID id, byte[] data) {
        dataHandlers.forEach(h -> {
            byte[] toForward = new byte[data.length];
            // make a copy and send, so that the users have the freedom to update the content.
            System.arraycopy(data, 0, toForward, 0, data.length);
            h.dataReceived(id, toForward);
        });
    }
}

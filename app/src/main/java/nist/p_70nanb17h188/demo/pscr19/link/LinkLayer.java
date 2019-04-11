package nist.p_70nanb17h188.demo.pscr19.link;

public class LinkLayer {

    private static LinkLayer_Impl defaultInstance;

    /**
     * Initiates the link layer.
     *
     * Can perform some actions based on the Device.getName()
     *
     * @see nist.p_70nanb17h188.demo.pscr19.Device
     */
    public static void init() {
        defaultInstance = new LinkLayer_Impl();
    }

    /**
     * Add a connection handler.
     *
     * @param h connection callback.
     * @return true if the handler is added, false if the handler already
     * exists.
     */
    public static boolean addConnectionHandler(ConnectionChangedHandler h) {
        return defaultInstance.addConnectionHandler(h);
    }

    /**
     * Remove a connection handler.
     *
     * @param h connection callback.
     * @return true if the handler is removed, false if the handler does not
     * exist.
     */
    public static boolean removeConnectionHandler(ConnectionChangedHandler h) {
        return defaultInstance.removeConnectionHandler(h);
    }

    /**
     * Add a data handler.
     *
     * @param h data callback.
     * @return true if the handler is added, false if the handler already
     * exists.
     */
    public static boolean addDataReceivedHandler(DataReceivedHandler h) {
        return defaultInstance.addDataReceivedHandler(h);
    }

    /**
     * Remove a data handler.
     *
     * @param h data callback.
     * @return true if the handler is removed, false if the handler does not
     * exist.
     */
    public static boolean removeDataReceivedHandler(DataReceivedHandler h) {
        return defaultInstance.removeDataReceivedHandler(h);
    }

    /**
     * Send a piece of data to a neighbor.
     *
     * @param id neighbor id.
     * @param data data to be sent.
     * @param start start position in the data to be sent.
     * @param len length of the data to be sent.
     * @return true if the data is sent, false otherwise.
     */
    public static boolean sendData(NeighborID id, byte[] data, int start, int len) {
        return defaultInstance.sendData(id, data, start, len);
    }

    /**
     * This class should not be instantiated.
     */
    private LinkLayer() {
    }
}

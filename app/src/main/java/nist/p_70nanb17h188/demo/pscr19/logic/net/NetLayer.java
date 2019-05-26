package nist.p_70nanb17h188.demo.pscr19.logic.net;

public class NetLayer {
    private static NetLayer_Impl defaultInstance;

    public static void init() {
        defaultInstance = new NetLayer_Impl();
    }

    public static NetLayer_Impl getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * Send a piece of data to a dst (either unicast or multicast).
     *
     * @param src   the source name.
     * @param dst   the destination name.
     * @param data  the data.
     * @param start the beginning of the data.
     * @param len   the size of the data.
     * @return true if successfully sent.
     */
    public static boolean sendData(Name src, Name dst, byte[] data, int start, int len) {
        return defaultInstance.sendData(src, dst, data, start, len);
    }

    /**
     * Send a piece of data to a dst (either unicast or multicast).
     *
     * @param src  the source name.
     * @param dst  the destination name.
     * @param data the data.
     * @return true if successfully sent.
     */
    public static boolean sendData(Name src, Name dst, byte[] data) {
        return sendData(src, dst, data, 0, data.length);
    }

    /**
     * Subscribe to a name (either unicast or multicast).
     *
     * @param n the name to subscribe to.
     * @param h the data handler when the name is received.
     * @return true if successfully added, false otherwise.
     */
    public static boolean subscribe(Name n, DataReceivedHandler h) {
        return defaultInstance.subscribe(n, h);
    }

    /**
     * Unsubscribe from a name (either unicast or multicast).
     *
     * @param n the name to unsubscribe from.
     * @param h the data handler to remove.
     * @return true if successfully removed, false otherwise.
     */
    public static boolean unSubscribe(Name n, DataReceivedHandler h) {
        return defaultInstance.unSubscribe(n, h);
    }


    private NetLayer() {
    }
}

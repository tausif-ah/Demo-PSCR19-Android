package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.app.Application;
import android.os.Handler;

import nist.p_70nanb17h188.demo.pscr19.logic.Device;

public class LinkLayer {
    /**
     * Broadcast intent action indicating that a link is either established or disconnected.
     * One extra EXTRA_NEIGHBOR_ID indicates the ID of the neighbor.
     * Another extra EXTRA_CONNECTED indicates if the connection is established or disconnected.
     */
    public static final String ACTION_LINK_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer.linkChanged";

    /**
     * Broadcast intent action indicating that a piece of data is received.
     * One extra EXTRA_NEIGHBOR_ID indicates the ID of the neighbor that sent the data.
     * Another extra EXTRA_DATA contains the data sent.
     */
    public static final String ACTION_DATA_RECEIVED = "nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer.dataReceived";
    public static final String EXTRA_NEIGHBOR_ID = "neighborId";
    public static final String EXTRA_CONNECTED = "connected";
    public static final String EXTRA_DATA = "data";


    private static LinkLayer_Impl defaultInstance;

    /**
     * This class should not be instantiated.
     */
    private LinkLayer() {
    }

    /**
     * Initiates the link layer.
     * <p>
     * Can perform some actions based on the Device.getName()
     *
     * @see Device
     */
    public static void init(Application application, Handler handler) {
        defaultInstance = new LinkLayer_Impl(application, handler);
    }

    /**
     * Send a piece of data to a neighbor.
     *
     * @param id    neighbor id.
     * @param data  data to be sent.
     * @param start start position in the data to be sent.
     * @param len   length of the data to be sent.
     * @return true if the data is sent, false otherwise.
     */
    public static boolean sendData(NeighborID id, byte[] data, int start, int len) {
        return defaultInstance.sendData(id, data, start, len);
    }
}

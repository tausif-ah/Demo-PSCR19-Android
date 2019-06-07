package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.support.annotation.NonNull;

import java.io.IOException;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class LinkLayer {
    private static final String TAG = "LinkLayer";

    /**
     * The context for link layer events.
     */
    public static final String CONTEXT_LINK_LAYER = "nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer";
    /**
     * Broadcast intent action indicating that a link is either established or disconnected.
     * One extra {@link #EXTRA_NEIGHBOR_ID} ({@link NeighborID}) indicates the ID of the neighbor.
     * Another extra {@link #EXTRA_CONNECTED} ({@link Boolean}) indicates if the connection is established or disconnected.
     */
    public static final String ACTION_LINK_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer.linkChanged";

    /**
     * Broadcast intent action indicating that a piece of data is received.
     * One extra {@link #EXTRA_NEIGHBOR_ID} (@link {@link NeighborID}) indicates the ID of the neighbor that sent the data.
     * Another extra {@link #EXTRA_DATA} (byte[]) contains the data sent.
     */
    public static final String ACTION_DATA_RECEIVED = "nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer.dataReceived";

    public static final String EXTRA_NEIGHBOR_ID = "neighborId";
    public static final String EXTRA_CONNECTED = "connected";
    public static final String EXTRA_DATA = "data";


    private static LinkLayer_Impl defaultImplementation;

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
    public static void init() {
        try {
            defaultImplementation = new LinkLayer_Impl();
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in creating defaultImplementation!");
        }
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
    public static boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        return defaultImplementation.sendData(id, data, start, len);
    }

    public static LinkLayer_Impl getDefaultImplementation() {
        return defaultImplementation;
    }
}

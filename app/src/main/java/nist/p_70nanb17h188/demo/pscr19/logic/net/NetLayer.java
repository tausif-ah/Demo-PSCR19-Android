package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;
import nist.p_70nanb17h188.demo.pscr19.logic.Consumer;

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
     * @param store if the message should be in a store-and-forward manner
     */
    public static void sendData(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, int start, int len, boolean store) {
        defaultInstance.sendData(src, dst, data, start, len, store);
    }

    /**
     * Send a piece of data to a dst (either unicast or multicast).
     *
     * @param src   the source name.
     * @param dst   the destination name.
     * @param data  the data.
     * @param store if the message should be in a store-and-forward manner
     */
    public static void sendData(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, boolean store) {
        sendData(src, dst, data, 0, data.length, store);
    }

    /**
     * Subscribe to a name (either unicast or multicast).
     *
     * @param n the name to subscribe to.
     * @param h the data handler when the name is received.
     * @return true if successfully added, false otherwise.
     */
    public static boolean subscribe(@NonNull Name n, @NonNull DataReceivedHandler h) {
        return defaultInstance.subscribe(n, h);
    }

    /**
     * Unsubscribe from a name (either unicast or multicast).
     *
     * @param n the name to unsubscribe from.
     * @param h the data handler to remove.
     * @return true if successfully removed, false otherwise.
     */
    public static boolean unSubscribe(@NonNull Name n, @NonNull DataReceivedHandler h) {
        return defaultInstance.unSubscribe(n, h);
    }

    public static Name registerRandomName(@NonNull String initiator) {
        return defaultInstance.registerRandomName(initiator);
    }

    public static boolean registerName(@NonNull Name n, boolean add, @NonNull String initiator) {
        return defaultInstance.registerName(n, add, initiator);
    }

    public static boolean hasName(@NonNull Name n) {
        return defaultInstance.hasName(n);
    }

    public static boolean registerRelationship(@NonNull Name parent, @NonNull Name child, boolean add, @NonNull String initiator) {
        return defaultInstance.registerRelationship(parent, child, add, initiator);
    }

    public static void forEachAncestor(@NonNull Name leaf, @NonNull Consumer<Name> consumer) {
        defaultInstance.forEachAncestor(leaf, consumer);
    }

    public static void forEachParent(@NonNull Name child, @NonNull Consumer<Name> consumer) {
        defaultInstance.forEachParent(child, consumer);
    }

    public static void forEachChild(@NonNull Name parent, @NonNull Consumer<Name> consumer) {
        defaultInstance.forEachChild(parent, consumer);
    }

    public static void forEachDescendant(@NonNull Name root, @NonNull Consumer<Name> consumer) {
        defaultInstance.forEachDescendant(root, consumer);
    }

    private NetLayer() {
    }
}

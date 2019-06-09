package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class NetLayer_Impl {
    private static final int MAGIC = 0x12345678;
    public static final int MAX_SEND_SIZE = 2000000;
    public static final int MAX_SHOW_SIZE = 40;

    /**
     * The context for wifi NetLayer_Impl events.
     */
    public static final String CONTEXT_NET_LAYER_IMPL = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl";

    /**
     * Broadcast intent action indicating that a neighbor list has changed.
     * One extra {@link #EXTRA_NEIGHBORS} ({@link NeighborID}[]) indicates the neighbors that ar connected.
     * <p>
     * The values are also available with function {@link #getConnectNeighbors()} .
     */
    public static final String ACTION_NEIGHBOR_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl.neighborChanged";
    public static final String EXTRA_NEIGHBORS = "neighbors";
    private static final String TAG = "NetLayer_Impl";
    private static final byte TYPE_DATA = 1;

    private final HashSet<NeighborID> connectedNeighbors = new HashSet<>();
    private final HashMap<Name, HashSet<DataReceivedHandler>> dataHandlers = new HashMap<>();
    private final GossipModule gossipModule;

    static int getWritePrefixSize() {
        return Helper.INTEGER_SIZE + 1; // magic + type
    }

    static void writePrefix(@NonNull ByteBuffer buf, byte type) {
        buf.putInt(MAGIC);
        buf.put(type);
    }

    NetLayer_Impl() {
        gossipModule = new GossipModule();
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).registerReceiver((context, intent) -> {
                    switch (intent.getAction()) {
                        case LinkLayer.ACTION_LINK_CHANGED:
                            onLinkChanged(intent);
                            break;
                        case LinkLayer.ACTION_DATA_RECEIVED:
                            onDataReceived(intent);
                            break;
                    }
                },
                new IntentFilter()
                        .addAction(LinkLayer.ACTION_LINK_CHANGED)
                        .addAction(LinkLayer.ACTION_DATA_RECEIVED)
        );
    }

    public GossipModule getGossipModule() {
        return gossipModule;
    }

    private void onLinkChanged(@NonNull Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        Boolean connected = intent.getExtra(LinkLayer.EXTRA_CONNECTED);
        assert neighborID != null && connected != null;
        boolean changed;
        if (connected) changed = connectedNeighbors.add(neighborID);
        else changed = connectedNeighbors.remove(neighborID);

        if (changed) {
            Context.getContext(CONTEXT_NET_LAYER_IMPL).sendBroadcast(new Intent(ACTION_NEIGHBOR_CHANGED).putExtra(EXTRA_NEIGHBORS, connectedNeighbors.toArray(new NeighborID[0])));
            if (connected) {
                getGossipModule().onNeighborConnected(neighborID);
            } else {
                getGossipModule().onNeighborDisconnected(neighborID);
            }
        }

    }

    public NeighborID[] getConnectNeighbors() {
        return connectedNeighbors.toArray(new NeighborID[0]);
    }

    private void onDataReceived(@NonNull Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        byte[] data = intent.getExtra(LinkLayer.EXTRA_DATA);
        assert data != null;
        Log.d(TAG, "received %d bytes from %s", data.length, neighborID);

        ByteBuffer buffer = ByteBuffer.wrap(data);
        // read magic
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            return;
        }
        int magic = buffer.getInt();
        if (magic != MAGIC) return;
        // read type
        byte type = buffer.get();
        switch (type) {
            case TYPE_DATA:
                readData(neighborID, buffer);
                break;
            case GossipModule.TYPE_SV_POSSESS:
                getGossipModule().readSVPossess(neighborID, buffer);
                break;
            case GossipModule.TYPE_SV_REQUEST:
                getGossipModule().readSVRequest(neighborID, buffer);
                break;
            case GossipModule.TYPE_MESSAGE:
                getGossipModule().readMessage(neighborID, buffer);
                break;
            default:
                Log.e(TAG, "Unknown type: 0x%02X", type & 0xFF);
                break;
        }
    }


    private void readData(NeighborID from, @NonNull ByteBuffer buffer) {
        Name src = Name.read(buffer);
        if (src == null) return;

        Name dst = Name.read(buffer);
        if (dst == null) return;

        if (buffer.remaining() < Helper.INTEGER_SIZE) return;
        int len = buffer.getInt();

        if (buffer.remaining() != len) return;
        byte[] data = new byte[len];
        buffer.get(data);

        synchronized (dataHandlers) {
            HashSet<DataReceivedHandler> handlers = dataHandlers.get(dst);
            if (handlers != null) {
                for (DataReceivedHandler h : handlers) {
                    h.dataReceived(src, dst, data);
                }
            }
        }

        // Dummy implementation, send it to all neighbors except the "from" interface
        // but forward only valid net layer packets
        for (NeighborID neighbor : connectedNeighbors) {
            if (!neighbor.equals(from))
                LinkLayer.sendData(neighbor, data, 0, data.length);
        }
    }

    boolean sendData(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, int start, int len) {
        if (len < 0 || start < 0 || start + len > data.length)
            throw new IllegalArgumentException("Error start, len value");
        int size = getWritePrefixSize() // prefix
                + Name.WRITE_SIZE * 2   // src, dst
                + Helper.INTEGER_SIZE   // len
                + len;                  // data
        ByteBuffer buffer = ByteBuffer.allocate(size);
        writePrefix(buffer, TYPE_DATA);
        boolean result = src.write(buffer);
        if (!result) return false;
        result = dst.write(buffer);
        if (!result) return false;
        buffer.putInt(len);
        buffer.put(data, start, len);

        // also send the data to local subscribers
        onDataReceived(new Intent(LinkLayer.ACTION_DATA_RECEIVED).putExtra(LinkLayer.EXTRA_DATA, buffer.array()));

        return true;
    }


    boolean subscribe(Name n, DataReceivedHandler h) {
        synchronized (dataHandlers) {
            HashSet<DataReceivedHandler> handlers = dataHandlers.get(n);
            if (handlers == null) {
                dataHandlers.put(n, handlers = new HashSet<>());
            }
            return handlers.add(h);
        }
    }

    boolean unSubscribe(Name n, DataReceivedHandler h) {
        synchronized (dataHandlers) {
            HashSet<DataReceivedHandler> handlers = dataHandlers.get(n);
            if (handlers == null) return false;
            if (handlers.remove(h)) {
                if (handlers.isEmpty()) {
                    dataHandlers.remove(n);
                }
                return true;
            }
            return false;
        }
    }
}

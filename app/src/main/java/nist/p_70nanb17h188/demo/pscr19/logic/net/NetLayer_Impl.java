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
    public static final String TAG = "NetLayer_Impl";
    private static final byte TYPE_DATA = 1;

    private final HashSet<NeighborID> connectedNeighbors = new HashSet<>();
    private final HashMap<Name, HashSet<DataReceivedHandler>> dataHandlers = new HashMap<>();

    NetLayer_Impl() {
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

    private void onLinkChanged(@NonNull Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        Boolean connected = intent.getExtra(LinkLayer.EXTRA_CONNECTED);
        assert neighborID != null && connected != null;
        boolean changed;
        if (connected) changed = connectedNeighbors.add(neighborID);
        else changed = connectedNeighbors.remove(neighborID);

        if (changed)
            Context.getContext(CONTEXT_NET_LAYER_IMPL).sendBroadcast(new Intent(ACTION_NEIGHBOR_CHANGED).putExtra(EXTRA_NEIGHBORS, connectedNeighbors.toArray(new NeighborID[0])));
    }

    public NeighborID[] getConnectNeighbors() {
        return connectedNeighbors.toArray(new NeighborID[0]);
    }

    private void onDataReceived(@NonNull Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        byte[] data = intent.getExtra(LinkLayer.EXTRA_DATA);
        assert data != null;

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
                if (!readData(buffer))
                    return;
                break;
            // for other types
            default:
                return;
        }

        // Dummy implementation, send it to all neighbors except the "from" interface
        // but forward only valid net layer packets
        for (NeighborID neighbor : connectedNeighbors) {
            if (!neighbor.equals(neighborID))
                LinkLayer.sendData(neighbor, data, 0, data.length);
        }

    }

    private boolean readData(@NonNull ByteBuffer buffer) {
        Name src = Name.read(buffer);
        if (src == null) return false;

        Name dst = Name.read(buffer);
        if (dst == null) return false;

        if (buffer.remaining() < Helper.INTEGER_SIZE) return false;
        int len = buffer.getInt();

        if (buffer.remaining() != len) return false;
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
        return true;
    }

    boolean sendData(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, int start, int len) {
        if (len < 0 || start < 0 || start + len > data.length)
            throw new IllegalArgumentException("Error start, len value");
        int size = Helper.INTEGER_SIZE  // magic
                + 1                     // type
                + Name.WRITE_SIZE * 2   // src, dst
                + Helper.INTEGER_SIZE   // len
                + len;                  // data
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(MAGIC);
        buffer.put(TYPE_DATA);
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

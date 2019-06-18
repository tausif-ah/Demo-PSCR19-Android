package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class NetLayer_Impl {
    private static final String TAG = "NetLayer_Impl";

    private final HashMap<Name, HashSet<DataReceivedHandler>> dataHandlers = new HashMap<>();
    private final GossipModule gossipModule;

    private static final byte TYPE_DATA = 1;
    private static final byte TYPE_NAME_CHANGE = 2;
    private static final byte TYPE_LINK_CHANGE = 3;
    private static final int MAGIC = 0x87654321;

    NetLayer_Impl() {
        gossipModule = new GossipModule();
        Context.getContext(GossipModule.CONTEXT_GOSSIP_MODULE).registerReceiver((context, intent) -> {
            if (!intent.getAction().equals(GossipModule.ACTION_DATA_RECEIVED)) return;
            onDataReceivedFromGossip(intent.getExtra(GossipModule.EXTRA_DATA));
        }, new IntentFilter().addAction(GossipModule.ACTION_DATA_RECEIVED));
    }

    public GossipModule getGossipModule() {
        return gossipModule;
    }


    void sendData(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, int start, int len, boolean store) {
        byte[] tmp = new byte[len];
        System.arraycopy(data, start, tmp, 0, len);
        // notify the applications
        onDataReceived(src, dst, tmp);

        int size = getWritePrefixSize() +
                Name.WRITE_SIZE * 2 +   // src, dst
                Helper.INTEGER_SIZE +   // len
                len;                    // data
        ByteBuffer buf = ByteBuffer.allocate(size);
        writePrefix(buf, TYPE_DATA);
        src.write(buf);
        dst.write(buf);
        buf.putInt(len);
        buf.put(tmp);
        gossipModule.addMessage(buf.array(), store);
    }

    boolean registerName(Name n, boolean add) {
        return false;
    }

    boolean registerRelationship(Name parent, Name child, boolean add) {
        return false;
    }

    private void onDataReceivedFromGossip(byte[] data) {
        if (data == null) return;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "Receive size (%d) < INTEGER_SIZE (%d)", buffer.remaining(), Helper.INTEGER_SIZE);
            return;
        }
        int magic = buffer.getInt();
        if (magic != MAGIC) {
            Log.e(TAG, "MAGIC (0x%08x) != required (0x%08x)", magic, MAGIC);
            return;
        }
        // read type
        byte type = buffer.get();
        switch (type) {
            case TYPE_DATA: {
                Name src = Name.read(buffer);
                if (src == null) {
                    Log.e(TAG, "TYPE_DATA, Failed in reading src");
                    break;
                }
                Name dst = Name.read(buffer);
                if (dst == null) {
                    Log.e(TAG, "TYPE_DATA, Failed in reading src");
                    break;
                }
                if (buffer.remaining() < Helper.INTEGER_SIZE) {
                    Log.e(TAG, "TYPE_DATA, buffer size (%d) < INTEGER_SIZE (%d)", buffer.remaining(), Helper.INTEGER_SIZE);
                    break;
                }
                int size = buffer.getInt();
                if (buffer.remaining() != size) {
                    Log.e(TAG, "TYPE_DATA, buffer size (%d) != count (%d)", buffer.remaining(), size);
                    break;
                }
                byte[] d = new byte[size];
                buffer.get(d);
                onDataReceived(src, dst, data);
                break;
            }
            case TYPE_NAME_CHANGE:
                break;
            case TYPE_LINK_CHANGE:
                break;
            default:
                Log.e(TAG, "Unknown type: 0x%02X", type & 0xFF);
                break;
        }
    }

    private void onDataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data) {
        // TODO: expand and send to subscribers
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

    private static int getWritePrefixSize() {
        return Helper.INTEGER_SIZE +    // magic
                1;                      // type
    }

    private static void writePrefix(@NonNull ByteBuffer buf, byte type) {
        buf.putInt(MAGIC);
        buf.put(type);
    }


}

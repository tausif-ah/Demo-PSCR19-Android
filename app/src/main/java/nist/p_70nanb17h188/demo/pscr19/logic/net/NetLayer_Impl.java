package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.Consumer;

public class NetLayer_Impl {

    private static final String TAG = "NetLayer_Impl";

    public static final String INITIATOR_INIT = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl.init";
    private static final String INITIATOR_NET = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl.net";

    private final HashMap<Name, HashSet<DataReceivedHandler>> dataHandlers = new HashMap<>();
    private final GossipModule gossipModule;
    private final Namespace namespace;

    private static final byte TYPE_DATA = 1;
    private static final byte TYPE_NAME_CHANGE = 2;
    private static final byte TYPE_LINK_CHANGE = 3;
    private static final int MAGIC = 0x87654321;

    NetLayer_Impl() {
        gossipModule = new GossipModule();
        namespace = new Namespace();
        Context.getContext(GossipModule.CONTEXT_GOSSIP_MODULE).registerReceiver((context, intent) -> {
            if (!intent.getAction().equals(GossipModule.ACTION_DATA_RECEIVED)) {
                return;
            }
            byte[] data = intent.getExtra(GossipModule.EXTRA_DATA);
            if (data == null) return;
            onDataReceivedFromGossip(data);
        }, new IntentFilter().addAction(GossipModule.ACTION_DATA_RECEIVED));
        Context.getContext(Namespace.CONTEXT_NAMESPACE).registerReceiver((context, intent) -> {
            switch (intent.getAction()) {
                case Namespace.ACTION_NAME_CHANGED: {
                    Name name = intent.getExtra(Namespace.EXTRA_NAME);
                    Boolean added = intent.getExtra(Namespace.EXTRA_ADDED);
                    String initiator = intent.getExtra(Namespace.EXTRA_INITIATOR);
                    assert name != null && added != null && initiator != null;
                    onNameChanged(name, added, initiator);
                    break;
                }
                case Namespace.ACTION_RELATIONSHIP_CHANGED: {
                    Name parent = intent.getExtra(Namespace.EXTRA_PARENT);
                    Name child = intent.getExtra(Namespace.EXTRA_CHILD);
                    Boolean added = intent.getExtra(Namespace.EXTRA_ADDED);
                    String initiator = intent.getExtra(Namespace.EXTRA_INITIATOR);
                    assert parent != null && child != null && added != null && initiator != null;
                    onLinkChanged(parent, child, added, initiator);
                    break;
                }
                default:
                    break;
            }
        }, new IntentFilter().addAction(Namespace.ACTION_NAME_CHANGED).addAction(Namespace.ACTION_RELATIONSHIP_CHANGED));
    }

    public GossipModule getGossipModule() {
        return gossipModule;
    }

    private Namespace getNamespace() {
        return namespace;
    }

    void sendData(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, int start, int len, boolean store) {
        byte[] tmp = new byte[len];
        System.arraycopy(data, start, tmp, 0, len);
        // notify the applications
        onDataReceived(src, dst, tmp);

        int size = getWritePrefixSize()
                + Name.WRITE_SIZE * 2  // src, dst
                + Helper.INTEGER_SIZE  // len
                + len;                 // data
        ByteBuffer buf = ByteBuffer.allocate(size);
        writePrefix(buf, TYPE_DATA);
        src.write(buf);
        dst.write(buf);
        buf.putInt(len);
        buf.put(tmp);
        gossipModule.addMessage(buf.array(), store);
    }

    Name registerRandomName(@NonNull String initiator) {
        return namespace.addRandomName(initiator);
    }

    boolean registerName(@NonNull Name n, boolean add, @NonNull String initiator) {
        if (add) {
            return namespace.addName(n, initiator);
        } else {
            return namespace.removeName(n, initiator);
        }
    }

    boolean hasName(@NonNull Name n) {
        return namespace.hasName(n);
    }

    void forEachAncestor(@NonNull Name leaf, @NonNull Consumer<Name> consumer) {
        namespace.forEachAncestor(leaf, consumer);
    }

    void forEachParent(@NonNull Name child, @NonNull Consumer<Name> consumer) {
        namespace.forEachParent(child, consumer);
    }

    void forEachChild(@NonNull Name parent, @NonNull Consumer<Name> consumer) {
        namespace.forEachChild(parent, consumer);
    }

    void forEachDescendant(@NonNull Name root, @NonNull Consumer<Name> consumer) {
        namespace.forEachDescendant(root, consumer);
    }

    boolean registerRelationship(@NonNull Name parent, @NonNull Name child, boolean add, @NonNull String initiator) {
        if (add) {
            return namespace.addRelationship(parent, child, initiator);
        } else {
            return namespace.removeRelationship(parent, child, initiator);
        }
    }

    boolean subscribe(@NonNull Name n, @NonNull DataReceivedHandler h) {
        synchronized (dataHandlers) {
            HashSet<DataReceivedHandler> handlers = dataHandlers.get(n);
            if (handlers == null) {
                dataHandlers.put(n, handlers = new HashSet<>());
            }
            return handlers.add(h);
        }
    }

    boolean unSubscribe(@NonNull Name n, @NonNull DataReceivedHandler h) {
        synchronized (dataHandlers) {
            HashSet<DataReceivedHandler> handlers = dataHandlers.get(n);
            if (handlers == null) {
                return false;
            }
            if (handlers.remove(h)) {
                if (handlers.isEmpty()) {
                    dataHandlers.remove(n);
                }
                return true;
            }
            return false;
        }
    }

    private void onDataReceivedFromGossip(@NonNull byte[] data) {
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
                    Log.e(TAG, "TYPE_DATA, Failed in reading dst");
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
                onDataReceived(src, dst, d);
                break;
            }
            case TYPE_NAME_CHANGE: {
                Name n = Name.read(buffer);
                if (n == null) {
                    Log.e(TAG, "TYPE_NAME_CHANGE, Failed in reading n");
                    break;
                }
                if (buffer.remaining() != 1) {
                    Log.e(TAG, "TYPE_NAME_CHANGE, remaing size %d != 1", buffer.remaining());
                    break;
                }
                boolean added = buffer.get() != 0;
                registerName(n, added, INITIATOR_NET);
                break;
            }
            case TYPE_LINK_CHANGE: {
                Name parent = Name.read(buffer);
                if (parent == null) {
                    Log.e(TAG, "TYPE_LINK_CHANGE, Failed in reading parent");
                    break;
                }
                Name child = Name.read(buffer);
                if (child == null) {
                    Log.e(TAG, "TYPE_LINK_CHANGE, Failed in reading child");
                    break;
                }
                if (buffer.remaining() != 1) {
                    Log.e(TAG, "TYPE_LINK_CHANGE, remaing size %d != 1", buffer.remaining());
                    break;
                }
                boolean added = buffer.get() != 0;
                registerRelationship(parent, child, added, INITIATOR_NET);
                break;
            }
            default:
                Log.e(TAG, "Unknown type: 0x%02X", type & 0xFF);
                break;
        }
    }

    private void onDataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data) {
        if (getNamespace().hasName(dst)) {
            getNamespace().forEachDescendant(dst, n -> {
                HashSet<DataReceivedHandler> handlers = dataHandlers.get(n);
                if (handlers != null) {
                    for (DataReceivedHandler h : handlers) {
                        h.dataReceived(src, n, data);
                    }
                }
            });
        } else {
            HashSet<DataReceivedHandler> handlers = dataHandlers.get(dst);
            if (handlers != null) {
                for (DataReceivedHandler h : handlers) {
                    h.dataReceived(src, dst, data);
                }
            }
        }
    }

    private void onNameChanged(@NonNull Name n, boolean added, @NonNull String initiator) {
        // ignore the events from the net layer
        if (INITIATOR_NET.equals(initiator) || INITIATOR_INIT.equals(initiator)) {
            return;
        }
        // send msg to the net
        int size = getWritePrefixSize()
                + Name.WRITE_SIZE   // n
                + 1;                // added
        ByteBuffer buffer = ByteBuffer.allocate(size);
        writePrefix(buffer, TYPE_NAME_CHANGE);
        n.write(buffer);
        buffer.put(added ? (byte) 1 : (byte) 0);
        gossipModule.addMessage(buffer.array(), true);
    }

    private void onLinkChanged(@NonNull Name parent, @NonNull Name child, boolean added, @NonNull String initiator) {
        // ignore the events from the net layer
        if (INITIATOR_NET.equals(initiator) || INITIATOR_INIT.equals(initiator)) {
            return;
        }
        // send msg to the net
        int size = getWritePrefixSize()
                + Name.WRITE_SIZE * 2 // parent, child
                + 1;                   // added
        ByteBuffer buffer = ByteBuffer.allocate(size);
        writePrefix(buffer, TYPE_LINK_CHANGE);
        parent.write(buffer);
        child.write(buffer);
        buffer.put(added ? (byte) 1 : (byte) 0);
        gossipModule.addMessage(buffer.array(), true);
    }

    private static int getWritePrefixSize() {
        return Helper.INTEGER_SIZE // magic
                + 1;               // type
    }

    private static void writePrefix(@NonNull ByteBuffer buf, byte type) {
        buf.putInt(MAGIC);
        buf.put(type);
    }

}

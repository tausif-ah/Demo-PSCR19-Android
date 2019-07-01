package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.BiConsumer;
import nist.p_70nanb17h188.demo.pscr19.logic.Consumer;
import nist.p_70nanb17h188.demo.pscr19.logic.FIFOMap;
import nist.p_70nanb17h188.demo.pscr19.logic.FIFOSet;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class GossipModule {

    /**
     * The context for wifi CONTEXT_GOSSIP_MODULE events.
     */
    public static final String CONTEXT_GOSSIP_MODULE = "nist.p_70nanb17h188.demo.pscr19.logic.net.GossipModule";

    /**
     * Broadcast intent action indicating that a neighbor list has changed.
     * One extra {@link #EXTRA_NEIGHBORS} ({@link NeighborID}[]) indicates the neighbors that are connected.
     * <p>
     * The values are also available with function {@link #getConnectNeighbors()} .
     */
    public static final String ACTION_NEIGHBOR_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.GossipModule.neighborChanged";
    public static final String EXTRA_NEIGHBORS = "neighbors";

    public static final String EXTRA_ADDED = "added";
    public static final String EXTRA_DIGEST = "digest";

    /**
     * Broadcast intent action indicating that a data has been added to/removed from the message pool.
     * One extra {@link #EXTRA_MESSAGE} ({@link Message) indicates the data,
     * Another extra {@link #EXTRA_ADDED} ({@link Boolean}) indicates if the data is added (true) or removed (false).
     * A third extra {@link #EXTRA_DIGEST} ({@link Digest}) indicates the digest of the data.
     * <p>
     * The values are also available for iterate with function {@link #forEachMessageInMessageBuffer(BiConsumer)}.
     * The values can be retrieved by checking {@link #getMessage(Digest)}.
     */
    public static final String ACTION_BUFFER_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.GossipModule.bufferChanged";
    public static final String EXTRA_MESSAGE = "message";

    /**
     * Broadcast intent action indicating that a digest has been added to/removed from the black list.
     * An extra {@link #EXTRA_ADDED} ({@link Boolean}) indicates if the digest is added (true) or removed (false).
     * A third extra {@link #EXTRA_DIGEST} ({@link Digest}) indicates the digest.
     * <p>
     * The values are also available for iterate with function {@link #forEachDigestInBlacklist(Consumer)}.
     */
    public static final String ACTION_BLACKLIST_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.GossipModule.blackListChanged";

    /**
     * Broadcast intent action indicating that a data has received.
     * An extra {@link #EXTRA_DATA} ({@link byte[]}) indicates the data.
     */
    public static final String ACTION_DATA_RECEIVED = "nist.p_70nanb17h188.demo.pscr19.logic.net.GossipModule.dataReceived";
    public static final String EXTRA_DATA = "data";

    private static final String TAG = "GossipModule";
    private static final int MAGIC = 0x12345678;
    private static final int CAPACITY_MESSAGE_BUFFER = 2000;
    private static final int CAPACITY_BLACK_LIST = 1000;

    private static final byte TYPE_SV_POSSESS = 1;  // SV of what I have
    private static final byte TYPE_SV_REQUEST = 2;  // SV of what I want
    private static final byte TYPE_MESSAGE = 3;     // Message

    private final HashSet<NeighborID> connectedNeighbors = new HashSet<>();

    private final FIFOMap<Digest, Message> messageBuffer = new FIFOMap<>(CAPACITY_MESSAGE_BUFFER);
    private final FIFOSet<Digest> blacklist = new FIFOSet<>(CAPACITY_BLACK_LIST);
    private Handler workThreadHandler;

    GossipModule() {
        startWorkThread();
        // listen to message buffer and blacklist events, send broadcast once event got
        messageBuffer.addItemChangedEventHandler((k, v, added) -> {
            // if a message is flushed out from message buffer, add it to black list
            if (!added) blacklist.add(k);
            Context.getContext(CONTEXT_GOSSIP_MODULE).sendBroadcast(new Intent(ACTION_BUFFER_CHANGED).putExtra(EXTRA_DIGEST, k).putExtra(EXTRA_MESSAGE, v).putExtra(EXTRA_ADDED, added));
        });
        blacklist.addItemChangedEventHandler((k, added) -> Context.getContext(CONTEXT_GOSSIP_MODULE).sendBroadcast(new Intent(ACTION_BLACKLIST_CHANGED).putExtra(EXTRA_DIGEST, k).putExtra(EXTRA_ADDED, added)));

        // listen to link layer events
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
        Log.i(TAG, "GossipModule ready!");
    }

    @NonNull
    public NeighborID[] getConnectNeighbors() {
        return connectedNeighbors.toArray(new NeighborID[0]);
    }

    public int getMessageBufferSize() {
        return messageBuffer.size();
    }

    public int getBlackListSize() {
        return blacklist.size();
    }

    @Nullable
    public Message getMessage(@NonNull Digest digest) {
        return messageBuffer.get(digest);
    }

    public void forEachDigestInBlacklist(Consumer<Digest> consumer) {
        blacklist.forEach(consumer);
    }

    public void forEachMessageInMessageBuffer(BiConsumer<Digest, Message> consumer) {
        messageBuffer.forEach(consumer);
    }

    public void addMessage(@NonNull byte[] value, boolean toStore) {
        // run it on the work thread, so that we don't have to synchronize messageBuffer.
        workThreadHandler.post(() -> {
            Message msg;
            Digest d;
            // create nonce using message until the digest does not match anything in the blacklist
            do {
                msg = new Message(value, toStore);
                d = new Digest(msg);
            } while (blacklist.contains(d) || messageBuffer.containsKey(d));
            if (msg.isStore()) messageBuffer.add(d, msg);
            else blacklist.add(d);
            Log.d(TAG, "Added digest: %s, msg: %s", d, msg);

            // No need to notify the applications that a message has been received
            // Context.getContext(CONTEXT_GOSSIP_MODULE).sendBroadcast(new Intent(ACTION_DATA_RECEIVED).putExtra(EXTRA_DATA, msg.getData()));

            // send MSG around
            byte[] toSend = messageToByteArray(msg);
            for (NeighborID neighbor : connectedNeighbors) {
                Log.d(TAG, "Send to %s, MSG, len=%d, %s", neighbor, toSend.length, msg);
                LinkLayer.sendData(neighbor, toSend);
            }
        });
    }

    @NonNull
    private byte[] messageToByteArray(@NonNull Message msg) {
        int size = getWritePrefixSize() + msg.getWriteSize();
        ByteBuffer buf = ByteBuffer.allocate(size);
        writePrefix(buf, TYPE_MESSAGE);
        msg.write(buf);
        return buf.array();

    }

    private void startWorkThread() {
        // start a work thread for heavy-load jobs.
        Thread t = new Thread(() -> {
            Looper.prepare();
            workThreadHandler = new android.os.Handler();
            Looper.loop();
        }, TAG);
        t.setDaemon(true);
        t.start();
        while (workThreadHandler == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void onLinkChanged(@NonNull Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        Boolean connected = intent.getExtra(LinkLayer.EXTRA_CONNECTED);
        assert neighborID != null && connected != null;
        boolean changed;
        if (connected) changed = connectedNeighbors.add(neighborID);
        else changed = connectedNeighbors.remove(neighborID);

        if (changed) {
            Context.getContext(CONTEXT_GOSSIP_MODULE).sendBroadcast(new Intent(ACTION_NEIGHBOR_CHANGED).putExtra(EXTRA_NEIGHBORS, connectedNeighbors.toArray(new NeighborID[0])));
            if (connected) {
                onNeighborConnected(neighborID);
            } else {
                onNeighborDisconnected(neighborID);
            }
        }
    }

    private void onNeighborConnected(NeighborID n) {
        // run it on the work thread, it is heavy
        workThreadHandler.post(() -> {
            // send SV_POSESS
            int itemCount = messageBuffer.size();
            int size = getWritePrefixSize()
                    + Helper.INTEGER_SIZE               // count
                    + Digest.DIGEST_SIZE * itemCount;   // digests
            ByteBuffer buf = ByteBuffer.allocate(size);
            writePrefix(buf, TYPE_SV_POSSESS);
            buf.putInt(itemCount);
            for (FIFOMap.Entry<Digest, Message> e : messageBuffer)
                e.getKey().write(buf);
            Log.d(TAG, "Send to %s, SV_POSSESS, count=%d, buf_len:%d", n, itemCount, buf.position());
            LinkLayer.sendData(n, buf.array());
        });
    }

    private void onNeighborDisconnected(NeighborID n) {

    }

    private void onDataReceived(@NonNull Intent intent) {
        // run it on the work thread
        workThreadHandler.post(() -> {
            NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
            byte[] data = intent.getExtra(LinkLayer.EXTRA_DATA);
            assert data != null;
            Log.d(TAG, "Received from %s, %d bytes", neighborID, data.length);

            ByteBuffer buffer = ByteBuffer.wrap(data);
            // read magic
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
                case TYPE_SV_POSSESS:
                    onSVPossessReceived(neighborID, buffer);
                    break;
                case TYPE_SV_REQUEST:
                    onSVRequestReceived(neighborID, buffer);
                    break;
                case TYPE_MESSAGE:
                    onMessageReceived(neighborID, buffer);
                    break;
                default:
                    Log.e(TAG, "Unknown type: 0x%02X", type & 0xFF);
                    break;
            }
        });
    }

    /**
     * Assuming that the MAGIC and TYPE are already read, TYPE == {@link #TYPE_SV_POSSESS}
     *
     * @param from   The neighbor ID of the incoming packet.
     * @param buffer The content buffer.
     */
    private void onSVPossessReceived(NeighborID from, ByteBuffer buffer) {
        // buffer does not contain the size of an integer
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "Received from %s, SV_POSSESS, buffer size (%d) < INTEGER_SIZE (%d)", from, buffer.remaining(), Helper.INTEGER_SIZE);
            return;
        }
        int count = buffer.getInt();
        // buffer size not correct
        if (buffer.remaining() != Digest.DIGEST_SIZE * count) {
            Log.e(TAG, "Received from %s, SV_POSSESS, buffer size (%d) != count (%d) * digestSize (%d)", from, buffer.remaining(), count, Digest.DIGEST_SIZE);
            return;
        }
        Log.d(TAG, "Received from %s, SV_POSSESS, count=%d", from, count);
        ArrayList<Digest> toRequests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Digest d = Digest.read(buffer);
            assert d != null;
            if (!blacklist.contains(d) && !messageBuffer.containsKey(d))
                toRequests.add(d);
        }
        int size = getWritePrefixSize()
                + Helper.INTEGER_SIZE                       // count
                + Digest.DIGEST_SIZE * toRequests.size();   // digests
        ByteBuffer buf = ByteBuffer.allocate(size);
        writePrefix(buf, TYPE_SV_REQUEST);
        buf.putInt(toRequests.size());
        for (Digest d : toRequests) {
            d.write(buf);
        }
        Log.d(TAG, "Send to %s, SV_REQUEST (count=%d, buf_len=%d)", from, toRequests.size(), buf.position());
        LinkLayer.sendData(from, buf.array());
    }

    /**
     * Assuming that the MAGIC and TYPE are already read, TYPE == {@link #TYPE_SV_REQUEST}
     *
     * @param from   The neighbor ID of the incoming packet.
     * @param buffer The content buffer.
     */
    private void onSVRequestReceived(NeighborID from, ByteBuffer buffer) {
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "Received from %s, SV_REQUEST, buffer size (%d) < INTEGER_SIZE (%d)", from, buffer.remaining(), Helper.INTEGER_SIZE);
            return;
        }
        int count = buffer.getInt();
        // buffer size not correct
        if (buffer.remaining() != Digest.DIGEST_SIZE * count) {
            Log.e(TAG, "Received from %s, SV_REQUEST, buffer size (%d) != count (%d) * digestSize (%d)", from, buffer.remaining(), count, Digest.DIGEST_SIZE);
            return;
        }
        Log.d(TAG, "Received from %s, SV_REQUEST, count=%d", from, count);
        for (int i = 0; i < count; i++) {
            Digest d = Digest.read(buffer);
            assert d != null;
            Message msg = messageBuffer.get(d);
            if (msg != null) {
                Log.d(TAG, "Send to %s, MSG d=%s, msg=%s", from, d, msg);
                LinkLayer.sendData(from, messageToByteArray(msg));
            }
        }
    }

    private void onMessageReceived(NeighborID from, ByteBuffer buffer) {
        Message msg = Message.read(buffer);
        if (msg == null) {
            Log.e(TAG, "Received from %s, MSG, failed in reading message from buffer!", from);
            return;
        }
        Digest d = new Digest(msg);
        Log.d(TAG, "Received from %s, MSG, d=%s, msg=%s", from, d, msg);
        if (blacklist.contains(d) || messageBuffer.containsKey(d)) {
            Log.d(TAG, "Black list/message buffer contains digest: %s", d);
            return;
        }
        if (msg.isStore()) messageBuffer.add(d, msg);
        else blacklist.add(d);

        // build toWrite buffer
        byte[] toWrite = messageToByteArray(msg);

        // notify applications that a data has received
        Context.getContext(CONTEXT_GOSSIP_MODULE).sendBroadcast(new Intent(ACTION_DATA_RECEIVED).putExtra(EXTRA_DATA, msg.getData()));

        for (NeighborID neighbor : connectedNeighbors) {
            if (neighbor.equals(from)) continue;
            LinkLayer.sendData(neighbor, toWrite);
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

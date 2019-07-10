package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

class RoutingModule {
    /**
     * The context for wifi CONTEXT_GOSSIP_MODULE events.
     */
    public static final String CONTEXT_ROUTING_MODULE = "nist.p_70nanb17h188.demo.pscr19.logic.net.RoutingModule";

    /**
     * Broadcast intent action indicating that a data has received.
     * An extra {@link #EXTRA_DATA} ({@link byte[]}) indicates the data.
     */
    public static final String ACTION_DATA_RECEIVED = "nist.p_70nanb17h188.demo.pscr19.logic.net.RoutingModule.dataReceived";
    public static final String EXTRA_DATA = "data";

    private static final String TAG = "RoutingModule";
    private static final int MAGIC = 0x3e4f2a1b;
    private Handler workThreadHandler;
    private final NeighborID myNeighborID;

    private static final byte TYPE_MESSAGE = 3;     // Message

    RoutingModule() {
        startWorkThread();
        myNeighborID = Constants.getNeighborID(Device.getName());
        // listen to link layer events
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).registerReceiver((context, intent) -> {
                    if (LinkLayer.ACTION_DATA_RECEIVED.equals(intent.getAction()))
                        onDataReceived(intent);
                },
                new IntentFilter()
                        .addAction(LinkLayer.ACTION_DATA_RECEIVED)
        );
        Log.i(TAG, "RoutingModule ready!");
    }

    void sendMessage(@NonNull Name dst, @NonNull byte[] data) {
        // run it on the work thread, so that we don't have to synchronize messageBuffer.
        workThreadHandler.post(() -> {
            NeighborID dstNeighborID = Constants.getNameDestination(dst);
            if (dstNeighborID == null) {
                Log.e(TAG, "Cannot find dst dstNeighborID for name %s, discard!", dst);
                return;
            }
            Log.d(TAG, "neighbor for name %s is %s", dst, dstNeighborID);
            NeighborID nextHopNeighborID = Constants.getNextHopNeighborID(myNeighborID, dstNeighborID);
            if (nextHopNeighborID == null) {
                Log.e(TAG, "Cannot find dst nextHopNeighborID for dstNeighborID %s, discard!", dstNeighborID);
                return;
            }
            Log.d(TAG, "next hop for dst %s is %s", dstNeighborID, nextHopNeighborID);
            int size = Helper.INTEGER_SIZE  // MAGIC
                    + 1  //TYPE
                    + dstNeighborID.getWriteSize() // DST_NA,
                    + myNeighborID.getWriteSize()// SRC_NA
                    + Helper.getByteArrayWriteSize(data);// CONTENT
            ByteBuffer buffer = ByteBuffer.allocate(size);
            buffer.putInt(MAGIC);
            buffer.put(TYPE_MESSAGE);
            dstNeighborID.write(buffer);
            myNeighborID.write(buffer);
            Helper.writeByteArray(buffer, data);
            boolean sent = LinkLayer.sendData(nextHopNeighborID, buffer.array());
            Log.d(TAG, "send %d bytes %s", size, sent ? "succeeded" : "failed");
        });
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
            // will deal with other messages in the future, e.g., LP, LPA, LSA
            if (type == TYPE_MESSAGE) {
                onMessageReceived(neighborID, buffer);
            } else {
                Log.e(TAG, "Unknown type: 0x%02X", type & 0xFF);
            }
        });
    }

    private void onMessageReceived(NeighborID from, ByteBuffer buffer) {
        NeighborID dstNeighborID = NeighborID.read(buffer);
        if (dstNeighborID == null) {
            Log.e(TAG, "Cannot read dstNeighborID, discard!");
            return;
        }
        Log.d(TAG, "Got msg with dstNeighborID: %s", dstNeighborID);
        if (!dstNeighborID.equals(myNeighborID)) { // forward the message
            NeighborID nextHopNeighborID = Constants.getNextHopNeighborID(myNeighborID, dstNeighborID);
            if (nextHopNeighborID == null) {
                Log.e(TAG, "Cannot find nextHopNeighborID for dstNeighborID %s", dstNeighborID);
                return;
            }
            Log.d(TAG, "nextHopNeighborID=%s", nextHopNeighborID);
            // buffer.array() includes magic, type and dstNeighborID
            byte[] toSend = buffer.array();
            boolean sent = LinkLayer.sendData(nextHopNeighborID, toSend);
            Log.d(TAG, "send %d bytes %s", toSend.length, sent ? "succeeded" : "failed");
        } else {
            // read out the message and broadcast it
            Log.d(TAG, "This is a message for me");
            NeighborID srcNeighborID = NeighborID.read(buffer);
            if (srcNeighborID == null) {
                Log.e(TAG, "Cannot read srcNeighborID, discard");
                return;
            }
            Log.d(TAG, "This is a message from %s", srcNeighborID);
            byte[] data = Helper.readByteArray(buffer);
            if (data == null) {
                Log.e(TAG, "Cannot read data, discard");
                return;
            }
            Log.d(TAG, "Received msg from %s, src=%s, dst=%s, len=%d", from, srcNeighborID, dstNeighborID, data.length);
            Context.getContext(CONTEXT_ROUTING_MODULE).sendBroadcast(new Intent(ACTION_DATA_RECEIVED).putExtra(EXTRA_DATA, data));
        }
    }
}

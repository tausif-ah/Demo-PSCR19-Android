package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class GossipModule {
    private static final String TAG = "GossipModule";
    static final byte TYPE_SV_POSSESS = -128; // SV of what I have
    static final byte TYPE_SV_REQUEST = -127; // SV of what I want
    static final byte TYPE_MESSAGE = -126; // Message

    @NonNull
    private static byte[] writeSV(byte type, @NonNull Collection<Digest> digests) {
        int size = NetLayer_Impl.getWritePrefixSize() + Digest.getDigestsWriteSize(digests.size());
        ByteBuffer buffer = ByteBuffer.allocate(size);
        NetLayer_Impl.writePrefix(buffer, type);
        Digest.writeDigests(buffer, digests);
        return buffer.array();
    }

    @NonNull
    private static byte[] writeMessage(@NonNull Digest digest, @NonNull byte[] content) {
        // why add digest? the other side can compute it with the message itself.
        int size = NetLayer_Impl.getWritePrefixSize()   // prefix
//                + Digest.DIGEST_SIZE                    // digest
                + Helper.INTEGER_SIZE                   // content length
                + content.length;                       // content
        ByteBuffer buffer = ByteBuffer.allocate(size);
        NetLayer_Impl.writePrefix(buffer, TYPE_MESSAGE);
//        digest.writeTo(buffer);
        buffer.putInt(content.length);
        buffer.put(content);
        return buffer.array();
    }

    @Nullable
    private static byte[] readMessage(ByteBuffer buf) {
        // why add digest? the other side can compute it with the message itself.
//        Digest digest = Digest.read(buf);
//        if (digest == null) {
//            Log.e(TAG, "Cannot read digest from buf");
//            return null;
//        }
        if (buf.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "Cannot read size from buf");
            return null;
        }
        int size = buf.getInt();
        if (buf.remaining() != size) {
            Log.e(TAG, "size does not match, size=%d, remaining=%d", size, buf.remaining());
            return null;
        }
        byte[] ret = new byte[size];
        buf.get(ret);
        return ret;
    }

    private final HashMap<Digest, byte[]> messageBuffer = new HashMap<>();
    private Handler workThreadHandler;

    GossipModule() {
        // start a work thread for heavy-load jobs.
        Thread t = new Thread(this::workerThread, "GossipModule");
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

    private void innerAddMessage(@NonNull byte[] value) {
        Digest d = new Digest(value);
        messageBuffer.put(d, value);
        Log.d(TAG, "Added message: %s->%s", d, Helper.getHexString(value));
    }

    public void addMessage(@NonNull byte[] value) {
        // run it on the work thread, so that we don't have to synchronize messageBuffer.
        workThreadHandler.post(() -> innerAddMessage(value));
    }

    void onNeighborConnected(NeighborID n) {
        // run it on the work thread, it is heavy
        workThreadHandler.post(() -> {
            //exchange summary vectors
            int itemCount = messageBuffer.size();
            Log.d(TAG, "I have %d msgs in buffer to %s", itemCount, n);
            byte[] buf = writeSV(TYPE_SV_POSSESS, messageBuffer.keySet());
            Log.d(TAG, "SV to send: %d", buf.length);
            LinkLayer.sendData(n, buf, 0, buf.length);
        });
    }

    void onNeighborDisconnected(NeighborID n) {

    }

    /**
     * Assuming that the MAGIC and TYPE are already read, TYPE == {@link #TYPE_SV_POSSESS}
     *
     * @param from   The neighbor ID of the incoming packet.
     * @param buffer The content buffer.
     */
    void readSVPossess(NeighborID from, ByteBuffer buffer) {
        // run it on the work thread
        workThreadHandler.post(() -> {
            ArrayList<Digest> svPossess = Digest.readDigests(buffer);
            if (svPossess == null) {
                Log.e(TAG, "Failed in reading SV from %s, ignore.", from);
                return;
            }
            // are you sure you need such a long string?
            Log.d(TAG, "received from %s (POSESS): %s", from, svPossess);
            ArrayList<Digest> svRequest = new ArrayList<>();
            for (Digest digest : svPossess) {
                if (!messageBuffer.containsKey(digest))
                    svRequest.add(digest);
            }
            byte[] wish = writeSV(TYPE_SV_REQUEST, svRequest);
            Log.d(TAG, "Send requests to %s (REQ): %s", from, svRequest);
            LinkLayer.sendData(from, wish, 0, wish.length);
        });
    }

    /**
     * Assuming that the MAGIC and TYPE are already read, TYPE == {@link #TYPE_SV_REQUEST}
     *
     * @param from   The neighbor ID of the incoming packet.
     * @param buffer The content buffer.
     */
    void readSVRequest(NeighborID from, ByteBuffer buffer) {
        // run it on the work thread
        workThreadHandler.post(() -> {
            ArrayList<Digest> svRequest = Digest.readDigests(buffer);
            if (svRequest == null) {
                Log.e(TAG, "Failed in reading request from %s, ignore.", from);
                return;
            }
            for (Digest digest : svRequest) {
                Log.d(TAG, "%s requests %s", from, digest);
                byte[] msg = messageBuffer.get(digest);
                if (msg == null) {
                    Log.e(TAG, "Cannot send %s to %s, digest not exist!", digest, from);
                    continue;
                }
                byte[] toSend = writeMessage(digest, msg);
                LinkLayer.sendData(from, toSend, 0, toSend.length);
            }
        });
    }

    void readMessage(NeighborID from, ByteBuffer buffer) {
        // run it on the work thread
        workThreadHandler.post(() -> {
            byte[] buf = readMessage(buffer);
            if (buf == null) {
                Log.e(TAG, "Failed in reading msg from %s", from);
                return;
            }
            // use innerAddMessage since we are already on the work thread.
            innerAddMessage(buf);
            Log.d(TAG, "Added message from %s: %s", from, Helper.getHexString(buf));
        });
    }

    public void printBuffer() {
        // run it on the work thread, so that we don't have to synchronize
        workThreadHandler.post(() -> Log.d(TAG, "buffer at: %s", messageBuffer));
    }


    private void workerThread() {
        Looper.prepare();
        workThreadHandler = new android.os.Handler();
        Looper.loop();
    }
}

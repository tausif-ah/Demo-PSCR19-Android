package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;

import nist.p_70nanb17h188.demo.pscr19.Helper;

public class Message {
    @NonNull
    private final byte[] data;
    private final long nonce;
    private final boolean store;

    private Message(@NonNull byte[] data, boolean store, long nonce) {
        this.data = data;
        this.store = store;
        this.nonce = nonce;
    }

    Message(@NonNull byte[] data, boolean store) {
        this(data, store, Helper.DEFAULT_RANDOM.nextLong());
    }

    @NonNull
    public byte[] getData() {
        return data;
    }

    public long getNonce() {
        return nonce;
    }

    public boolean isStore() {
        return store;
    }

    int getWriteSize() {
        return 1 +                      // store
                Helper.LONG_SIZE +      // nonce
                Helper.INTEGER_SIZE +   // data len
                data.length;            // data
    }


    boolean write(@NonNull ByteBuffer byteBuffer) {
        if (byteBuffer.capacity() - byteBuffer.position() < getWriteSize()) return false;
        byteBuffer.put(store ? (byte) 1 : (byte) 0);
        byteBuffer.putLong(nonce);
        byteBuffer.putInt(data.length);
        byteBuffer.put(data);
        return true;
    }

    @Nullable
    static Message read(@NonNull ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < Helper.LONG_SIZE + Helper.INTEGER_SIZE) {
            return null;
        }
        boolean store = byteBuffer.get() != 0;
        long nonce = byteBuffer.getLong();
        int len = byteBuffer.getInt();
        if (byteBuffer.remaining() < len)
            return null;
        byte[] data = new byte[len];
        byteBuffer.get(data);
        return new Message(data, store, nonce);
    }
}

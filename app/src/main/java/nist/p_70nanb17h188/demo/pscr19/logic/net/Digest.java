package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import nist.p_70nanb17h188.demo.pscr19.Helper;

/**
 * Create a digest class instead of byte[], so that we can use hashCode ({@link Arrays#hashCode(byte[])}) and equals ({@link Arrays#equals(byte[], byte[])}).
 */
public class Digest {
    private static final String TAG = "Digest";
    private static final String DEFAULT_DIGEST_ALGORITHM = "SHA-1";
    static final int DIGEST_SIZE;

    static {
        try {
            MessageDigest tmpDigest = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM);
            DIGEST_SIZE = tmpDigest.getDigestLength();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(System.err);
            throw new AssertionError("Cannot find digest " + DEFAULT_DIGEST_ALGORITHM);
        }
    }

    // we can happily keep it a byte array
    private static byte[] getMessageDigest(Message message) {
        try {
            // generate a new object as it is not thread-safe.
            MessageDigest tmpDigest = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM);
            tmpDigest.update(message.getData());
            ByteBuffer buf = ByteBuffer.allocate(Helper.LONG_SIZE);
            buf.putLong(message.getNonce());
            return tmpDigest.digest(buf.array());
        } catch (NoSuchAlgorithmException e) {
            // should never reach here
            e.printStackTrace(System.err);
            throw new AssertionError(DEFAULT_DIGEST_ALGORITHM);
        }
    }

    @NonNull
    private final byte[] digest;
    private final int hashCode;

    /**
     * Get a digest from a piece of data.
     *
     * @param message The message to be digested.
     */
    Digest(@NonNull Message message) {
        this(getMessageDigest(message));
    }

    /**
     * Internal set of digest, should never be used from outside
     */
    private Digest(@NonNull byte[] digest) {
        this.digest = digest;
        this.hashCode = Arrays.hashCode(digest);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Digest digest1 = (Digest) o;
        return Arrays.equals(digest, digest1.digest);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @NonNull
    @Override
    public String toString() {
        return Helper.getHexString(digest);
    }

    void write(@NonNull ByteBuffer buffer) {
        buffer.put(digest);
    }

    @Nullable
    static Digest read(@NonNull ByteBuffer buffer) {
        if (buffer.remaining() < DIGEST_SIZE) return null;
        byte[] buf = new byte[DIGEST_SIZE];
        buffer.get(buf);
        return new Digest(buf);
    }

}

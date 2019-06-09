package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

/**
 * Create a digest class instead of byte[], so that we can use hashCode ({@link Arrays#hashCode(byte[])}) and equals ({@link Arrays#equals(byte[], byte[])}).
 */
class Digest {
    private static final String TAG = "Digest";
    private static final String DEFAULT_DIGEST_ALGORITHM = "SHA-1";
    private static final int DIGEST_SIZE;

    static {
        try {
            MessageDigest tmpDigest = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM);
            DIGEST_SIZE = tmpDigest.getDigestLength();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Cannot find digest " + DEFAULT_DIGEST_ALGORITHM);
        }
    }

    // we can happily keep it a byte array
    private static byte[] getMessageDigest(byte[] message) {
        try {
            MessageDigest tmpDigest = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM);
            return tmpDigest.digest(message);
        } catch (NoSuchAlgorithmException e) {
            // should never reach here
            e.printStackTrace();
            throw new AssertionError(DEFAULT_DIGEST_ALGORITHM);
        }
    }

    @NonNull
    private final byte[] digest;
    private final int hashCode;

    /**
     * Get a digest from a piece of data.
     *
     * @param data The digest of the data.
     */
    Digest(@NonNull byte[] data) {
        this(getMessageDigest(data), true);
    }

    /**
     * Internal set of digest, should never be used from outside
     * Added parameter unused to distinguish from {@link #Digest(byte[])}
     */
    private Digest(@NonNull byte[] digest, boolean unused) {
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
        return String.format(Locale.US, "Digest{%s}", Helper.getHexString(digest));
    }

    void writeTo(@NonNull ByteBuffer buffer) {
        buffer.put(digest);
    }

    @Nullable
    static Digest read(@NonNull ByteBuffer buffer) {
        if (buffer.remaining() < DIGEST_SIZE) return null;
        byte[] buf = new byte[DIGEST_SIZE];
        buffer.get(buf);
        return new Digest(buf, true);
    }

    static int getDigestsWriteSize(int count) {
        return Helper.INTEGER_SIZE + DIGEST_SIZE * count; // count + digests
    }

    static void writeDigests(@NonNull ByteBuffer buffer, @NonNull Collection<Digest> digests) {
        buffer.putInt(digests.size());
        for (Digest digest : digests) {
            digest.writeTo(buffer);
        }
    }

    @Nullable
    static ArrayList<Digest> readDigests(@NonNull ByteBuffer buffer) {
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "Buffer size (%d) < int size (%d)", buffer.remaining(), Helper.INTEGER_SIZE);
            return null;
        }
        int itemCount = buffer.getInt();
        Log.d(TAG, "itemCount = %d", itemCount);
        if (buffer.remaining() != itemCount * DIGEST_SIZE) {
            Log.e(TAG, "itemCount=%d, digestSize=%d, should have=%d, buf remaining=%d", itemCount, DIGEST_SIZE, itemCount * DIGEST_SIZE, buffer.remaining());
            return null;
        }
        ArrayList<Digest> ret = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            Digest next = read(buffer);
            if (next == null) {
                Log.e(TAG, "Cannot read next, i=%d", i);
                return null;
            }
            ret.add(next);
        }
        return ret;
    }


}

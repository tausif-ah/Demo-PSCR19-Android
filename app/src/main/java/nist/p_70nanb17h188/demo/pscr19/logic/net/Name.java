package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.Helper;

public class Name {
    static final int WRITE_SIZE = Helper.LONG_SIZE;

    private final long value;

    public Name(long value) {
        this.value = value;
    }

    public Name (String value){
        this.value = Long.parseLong(value);
    }

    public boolean isMulticast() {
        return value < 0;
    }

    /**
     * Gets the value of the name.
     * Should not be used by app layer. Do not set it public.
     *
     * @return the value of the name
     */
    long getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (int) (this.value ^ (this.value >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Name other = (Name) obj;
        return this.value == other.value;
    }

    boolean write(@NonNull ByteBuffer byteBuffer) {
        if (byteBuffer.capacity() - byteBuffer.position() < Helper.LONG_SIZE) return false;
        byteBuffer.putLong(value);
        return true;
    }

    @Nullable
    static Name read(@NonNull ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < Helper.LONG_SIZE) {
            return null;
        }
        return new Name(byteBuffer.getLong());
    }


    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.US, "<<%016x>>", value);
    }

}

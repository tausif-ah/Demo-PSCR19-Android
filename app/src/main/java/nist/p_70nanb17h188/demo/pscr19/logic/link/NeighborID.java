package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;

import nist.p_70nanb17h188.demo.pscr19.Helper;

/**
 * ID for neighbors. Unique for each neighbor.
 */

public class NeighborID {

    @NonNull
    private final String name;

    NeighborID(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + this.name.hashCode();
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
        final NeighborID other = (NeighborID) obj;
        return this.name.equals(other.name);
    }

    public int getWriteSize() {
        return Helper.getStringWriteSize(name);
    }

    public void write(@NonNull ByteBuffer buffer) {
        Helper.writeString(buffer, name);
    }

    @Nullable
    public static NeighborID read(@NonNull ByteBuffer buffer) {
        String name = Helper.readString(buffer);
        if (name == null) return null;
        return new NeighborID(name);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("<%s>", name);
    }
}

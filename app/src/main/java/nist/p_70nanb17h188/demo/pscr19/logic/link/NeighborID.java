package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.support.annotation.NonNull;

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

    @NonNull
    @Override
    public String toString() {
        return String.format("Neighbor{%s}", name);
    }
}

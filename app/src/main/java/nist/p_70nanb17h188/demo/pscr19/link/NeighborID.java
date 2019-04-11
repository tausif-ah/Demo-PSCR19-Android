package nist.p_70nanb17h188.demo.pscr19.link;

/**
 * ID for neighbors. Unique for each neighbor.
 *
 * Override equals, hashcode and toString functions.
 *
 * Here is an example.
 *
 */

public class NeighborID {

    private final int id;

    public NeighborID(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + this.id;
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
        return this.id == other.id;
    }

    @Override
    public String toString() {
        return "NeighborID{" + "id=" + id + '}';
    }
}

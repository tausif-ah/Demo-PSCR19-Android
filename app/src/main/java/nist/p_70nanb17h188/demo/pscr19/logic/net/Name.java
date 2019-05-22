package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;

public class Name {
    private final long value;

    public Name(long value) {
        this.value = value;
    }

    public boolean isMulticast() {
        return value < 0;
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

    @NonNull
    @Override
    public String toString() {
        return "Name{" + "value=" + value + '}';
    }

}

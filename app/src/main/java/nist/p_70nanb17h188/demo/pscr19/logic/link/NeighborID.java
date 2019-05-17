package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * ID for neighbors. Unique for each neighbor.
 */

public class NeighborID implements Parcelable {

    @NonNull
    public final String name;

    NeighborID(@NonNull String name) {
        this.name = name;
    }

    private NeighborID(@NonNull Parcel in) {
        String tmp = in.readString();
        assert tmp != null;
        name = tmp;
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

    public static final Creator<NeighborID> CREATOR = new Creator<NeighborID>() {
        @NonNull
        @Override
        public NeighborID createFromParcel(Parcel in) {
            return new NeighborID(in);
        }

        @NonNull
        @Override
        public NeighborID[] newArray(int size) {
            return new NeighborID[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return String.format("Neighbor{%s}", name);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}

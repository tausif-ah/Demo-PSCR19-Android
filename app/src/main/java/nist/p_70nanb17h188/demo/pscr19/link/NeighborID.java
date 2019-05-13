package nist.p_70nanb17h188.demo.pscr19.link;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * ID for neighbors. Unique for each neighbor.
 */

public class NeighborID implements Parcelable {

    @NonNull
    private final String id;

    public NeighborID(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + this.id.hashCode();
        return hash;
    }

    @Contract(value = "null -> false", pure = true)
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
        return this.id.equals(other.id);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("Neighbor{%s}", id);
    }

    private NeighborID(@NotNull Parcel in) {
        String tmp = in.readString();
        if (tmp == null) throw new IllegalArgumentException("Failed in reading NeighborID from parcel!");
        id = tmp;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<NeighborID> CREATOR = new Creator<NeighborID>() {
        @NotNull
        @Contract("_ -> new")
        @Override
        public NeighborID createFromParcel(Parcel in) {
            return new NeighborID(in);
        }

        @NotNull
        @Contract(value = "_ -> new", pure = true)
        @Override
        public NeighborID[] newArray(int size) {
            return new NeighborID[size];
        }
    };
}

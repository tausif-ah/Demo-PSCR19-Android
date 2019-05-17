package nist.p_70nanb17h188.demo.pscr19.logic.log;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class LogItem implements Parcelable {
    public static final Creator<LogItem> CREATOR = new Creator<LogItem>() {
        @Override
        public LogItem createFromParcel(Parcel in) {
            return new LogItem(in);
        }

        @Override
        public LogItem[] newArray(int size) {
            return new LogItem[size];
        }
    };
    private static final AtomicLong GLOBAL_SERIAL = new AtomicLong(1);

    public final long id;
    @NonNull
    public final Date time;
    @NonNull
    public final LogType type;
    @NonNull
    public final String tag;
    @NonNull
    public final String message;

    LogItem(@NonNull LogType type, @NonNull String tag, @NonNull String message) {
        id = GLOBAL_SERIAL.getAndIncrement();
        time = new Date();
        this.type = type;
        this.tag = tag;
        this.message = message;
    }

    private LogItem(Parcel in) {
        id = in.readLong();
        time = new Date(in.readLong());
        type = Enum.valueOf(LogType.class, in.readString());
        String tagFromIn = in.readString();
        assert tagFromIn != null;
        tag = tagFromIn;
        String messageFromIn = in.readString();
        assert messageFromIn != null;
        message = messageFromIn;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(time.getTime());
        dest.writeString(type.name());
        dest.writeString(tag);
        dest.writeString(message);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}

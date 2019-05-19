package nist.p_70nanb17h188.demo.pscr19.logic.log;

import android.support.annotation.NonNull;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class LogItem {
    private static final AtomicLong GLOBAL_SERIAL = new AtomicLong(1);

    private final long id;
    @NonNull
    private final Date time;
    @NonNull
    private final LogType type;
    @NonNull
    private final String tag;
    @NonNull
    private final String message;

    LogItem(@NonNull LogType type, @NonNull String tag, @NonNull String message) {
        id = GLOBAL_SERIAL.getAndIncrement();
        time = new Date();
        this.type = type;
        this.tag = tag;
        this.message = message;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public Date getTime() {
        return time;
    }

    @NonNull
    public LogType getType() {
        return type;
    }

    @NonNull
    public String getTag() {
        return tag;
    }

    @NonNull
    public String getMessage() {
        return message;
    }
}

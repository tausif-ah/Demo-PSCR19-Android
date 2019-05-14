package nist.p_70nanb17h188.demo.pscr19.logic.log;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Log {
    public static final int DEFAULT_CAPACITY = 1000;
    public static final String ACTION_ITEMS_INSERTED = "nist.p_70nanb17h188.demo.pscr19.logic.log.Log.itemsInserted";
    public static final String ACTION_ITEMS_REMOVED = "nist.p_70nanb17h188.demo.pscr19.logic.log.Log.itemsRemoved";
    public static final String ACTION_TAGS_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.log.Log.tagsChanged";
    public static final String EXTRA_POSITION_START = "positionStart";
    public static final String EXTRA_ITEM_COUNT = "itemCount";
    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static Log DEFAULT_INSTANCE = null;
    private final ArrayList<LogItem> logs = new ArrayList<>();
    private final HashMap<String, AtomicInteger> tagCounts = new HashMap<>();
    private final Application application;
    private int capacity;

    private Log(int capacity, Application application) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity should be > 0");
        this.capacity = capacity;
        this.application = application;
    }

    public static void init(int capacity, Application application) {
        if (DEFAULT_INSTANCE == null) DEFAULT_INSTANCE = new Log(capacity, application);
    }

    public static void v(String tag, String fmt, Object... params) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._v(tag, fmt, params);
    }

    public static void d(String tag, String fmt, Object... params) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._d(tag, fmt, params);
    }

    public static void i(String tag, String fmt, Object... params) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._i(tag, fmt, params);
    }

    public static void w(String tag, String fmt, Object... params) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._w(tag, fmt, params);
    }

    public static void e(String tag, String fmt, Object... params) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._e(tag, fmt, params);
    }

    public static void e(String tag, Throwable tr, String fmt, Object... params) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._e(tag, tr, fmt, params);
    }

    public static void v(String tag, String msg) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._v(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._d(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._i(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._e(tag, msg);
    }

    public static void e(String tag, Throwable tr, String msg) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._e(tag, tr, msg);
    }

    public static int getCapacity() {
        if (DEFAULT_INSTANCE == null) return 0;
        return DEFAULT_INSTANCE._getCapacity();
    }

    public static void setCapacity(int newCapacity) {
        if (DEFAULT_INSTANCE == null) return;
        DEFAULT_INSTANCE._setCapacity(newCapacity);
    }

    public static int getSize() {
        if (DEFAULT_INSTANCE == null) return 0;
        return DEFAULT_INSTANCE._getSize();
    }

    public static String[] getTags() {
        if (DEFAULT_INSTANCE == null) return new String[0];
        return DEFAULT_INSTANCE._getTags();
    }

    public static LogItem getItemAt(int position) {
        if (DEFAULT_INSTANCE == null) return null;
        return DEFAULT_INSTANCE._getItemAt(position);
    }

    public void _v(String tag, String fmt, Object... params) {
        _v(tag, String.format(fmt, params));
    }

    public void _d(String tag, String fmt, Object... params) {
        _d(tag, String.format(fmt, params));
    }

    public void _i(String tag, String fmt, Object... params) {
        _i(tag, String.format(fmt, params));
    }

    public void _w(String tag, String fmt, Object... params) {
        _w(tag, String.format(fmt, params));
    }

    public void _e(String tag, String fmt, Object... params) {
        _e(tag, null, String.format(fmt, params));
    }

    public void _e(String tag, Throwable tr, String fmt, Object... params) {
        _e(tag, tr, String.format(fmt, params));
    }

    public void _v(String tag, String msg) {
        android.util.Log.v(tag, msg);
        _addLog(new LogItem(LogType.Verbose, tag, msg));
    }

    public void _d(String tag, String msg) {
        android.util.Log.d(tag, msg);
        _addLog(new LogItem(LogType.Debug, tag, msg));
    }

    public void _i(String tag, String msg) {
        android.util.Log.i(tag, msg);
        _addLog(new LogItem(LogType.Info, tag, msg));
    }

    public void _w(String tag, String msg) {
        android.util.Log.w(tag, msg);
        _addLog(new LogItem(LogType.Warn, tag, msg));
    }

    public void _e(String tag, String msg) {
        _e(tag, null, msg);
    }

    public void _e(String tag, Throwable tr, String msg) {
        android.util.Log.e(tag, msg, tr);
        if (tr == null) {
            _addLog(new LogItem(LogType.Error, tag, msg));
        } else {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            tr.printStackTrace(pw);
            pw.flush();
            _addLog(new LogItem(LogType.Error, tag, String.format("%s%n%s", msg, writer.getBuffer())));
        }
    }

    public void _setCapacity(int newCapacity) {
        if (newCapacity <= 0) throw new IllegalArgumentException("Capacity should be > 0");
        synchronized (this) {
            capacity = newCapacity;
            if (_removeExtra()) _fireTagsChanged();
        }
    }

    public int _getCapacity() {
        return capacity;
    }

    public int _getSize() {
        return logs.size();
    }

    public LogItem _getItemAt(int position) {
        if (position < 0 || position >= logs.size()) return null;
        return logs.get(position);
    }

    public String[] _getTags() {
        String[] tags = new String[tagCounts.size()];
        tagCounts.keySet().toArray(tags);
        return tags;
    }

    private void _addLog(@NonNull LogItem item) {
        synchronized (this) {
            boolean tagsChanged = false;
            AtomicInteger count = tagCounts.get(item.getTag());
            if (count == null) {
                tagsChanged = true;
                tagCounts.put(item.getTag(), count = new AtomicInteger());
            }
            logs.add(0, item);
            count.incrementAndGet();
            _fireItemInserted(0, 1);

            tagsChanged |= _removeExtra();
            if (tagsChanged) _fireTagsChanged();
        }
    }

    private boolean _removeExtra() {
        int toRemove = logs.size() - capacity;
        if (toRemove <= 0) return false;
        boolean tagsChanged = false;

        for (int i = 0; i < toRemove; i++) {
            LogItem item = logs.remove(capacity);
            AtomicInteger count = tagCounts.get(item.getTag());
            if (count != null && count.decrementAndGet() == 0) {
                tagCounts.remove(item.getTag());
                tagsChanged = true;
            }
        }
        _fireItemRemoved(capacity, toRemove);
        return tagsChanged;
    }

    private void _fireItemInserted(int positionStart, int itemCount) {
        Intent intent = new Intent(ACTION_ITEMS_INSERTED).putExtra(EXTRA_POSITION_START, positionStart).putExtra(EXTRA_ITEM_COUNT, itemCount);
        application.getApplicationContext().sendBroadcast(intent);
    }

    private void _fireItemRemoved(int positionStart, int itemCount) {
        Intent intent = new Intent(ACTION_ITEMS_REMOVED).putExtra(EXTRA_POSITION_START, positionStart).putExtra(EXTRA_ITEM_COUNT, itemCount);
        application.getApplicationContext().sendBroadcast(intent);
    }

    private void _fireTagsChanged() {
        Intent intent = new Intent(ACTION_TAGS_CHANGED);
        application.getApplicationContext().sendBroadcast(intent);
    }

    public enum LogType {
        Verbose(2, "V"), Debug(3, "D"), Info(4, "I"), Warn(5, "W"), Error(6, "E");

        private final int val;
        private final String acry;

        LogType(int val, String acry) {
            this.val = val;
            this.acry = acry;
        }

        public int getVal() {
            return val;
        }

        public String getAcry() {
            return acry;
        }
    }

    public static class LogItem {
        private static final AtomicLong GLOBAL_SERIAL = new AtomicLong(1);
        private final long id;
        private final Date time;
        private final LogType type;
        private final String tag;
        private final String message;

        LogItem(LogType type, String tag, String message) {
            id = GLOBAL_SERIAL.getAndIncrement();
            time = new Date();
            this.type = type;
            this.tag = tag;
            this.message = message;
        }

        public long getId() {
            return id;
        }

        public String getTimeString() {
            return DEFAULT_DATE_FORMAT.format(time);
        }

        public LogType getType() {
            return type;
        }

        public String getTag() {
            return tag;
        }

        public String getMessage() {
            return message;
        }
    }
}

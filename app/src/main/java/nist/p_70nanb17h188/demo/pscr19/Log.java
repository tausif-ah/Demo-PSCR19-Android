package nist.p_70nanb17h188.demo.pscr19;

import android.support.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Log {
    public static final int DEFAULT_CAPACITY = 1000;
    public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final Log DEFAULT_INSTANCE = new Log(DEFAULT_CAPACITY);
    private final ArrayList<LogItem> logs = new ArrayList<>();
    private final HashMap<String, AtomicInteger> tagCounts = new HashMap<>();
    private final HashSet<RangeChangedHandler> insertHandlers = new HashSet<>(), removeHandlers = new HashSet<>();
    private final HashSet<TagsChangedHandler> tagsChangedHandlers = new HashSet<>();
    private int capacity;

    private Log(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity should be > 0");
        this.capacity = capacity;
    }

    public static void v(String tag, String fmt, Object... params) {
        DEFAULT_INSTANCE._v(tag, fmt, params);
    }

    public static void d(String tag, String fmt, Object... params) {
        DEFAULT_INSTANCE._d(tag, fmt, params);
    }

    public static void i(String tag, String fmt, Object... params) {
        DEFAULT_INSTANCE._i(tag, fmt, params);
    }

    public static void w(String tag, String fmt, Object... params) {
        DEFAULT_INSTANCE._w(tag, fmt, params);
    }

    public static void e(String tag, String fmt, Object... params) {
        DEFAULT_INSTANCE._e(tag, fmt, params);
    }

    public static void e(String tag, Throwable tr, String fmt, Object... params) {
        DEFAULT_INSTANCE._e(tag, tr, fmt, params);
    }

    public static void v(String tag, String msg) {
        DEFAULT_INSTANCE._v(tag, msg);
    }

    public static void d(String tag, String msg) {
        DEFAULT_INSTANCE._d(tag, msg);
    }

    public static void i(String tag, String msg) {
        DEFAULT_INSTANCE._i(tag, msg);
    }

    public static void w(String tag, String msg) {
        DEFAULT_INSTANCE._w(tag, msg);
    }

    public static void e(String tag, String msg) {
        DEFAULT_INSTANCE._e(tag, msg);
    }

    public static void e(String tag, Throwable tr, String msg) {
        DEFAULT_INSTANCE._e(tag, tr, msg);
    }

    public static int getCapacity() {
        return DEFAULT_INSTANCE._getCapacity();
    }

    public static void setCapacity(int newCapacity) {
        DEFAULT_INSTANCE._setCapacity(newCapacity);
    }

    public static int getSize() {
        return DEFAULT_INSTANCE._getSize();
    }

    public static String[] getTags() {
        return DEFAULT_INSTANCE._getTags();
    }

    public static LogItem getItemAt(int position) {
        return DEFAULT_INSTANCE._getItemAt(position);
    }

    public static boolean addItemRangeInsertedHandler(RangeChangedHandler h) {
        return DEFAULT_INSTANCE._addItemRangeInsertedHandler(h);
    }

    public static boolean removeItemRangeInsertedHandler(RangeChangedHandler h) {
        return DEFAULT_INSTANCE._removeItemRangeInsertedHandler(h);
    }

    public static boolean addItemRangeRemovedHandler(RangeChangedHandler h) {
        return DEFAULT_INSTANCE._addItemRangeRemovedHandler(h);
    }

    public static boolean removeItemRangeRemovedHandler(RangeChangedHandler h) {
        return DEFAULT_INSTANCE._removeItemRangeRemovedHandler(h);
    }

    public static boolean addTagsChangedHandler(TagsChangedHandler h) {
        return DEFAULT_INSTANCE._addTagsChangedHandler(h);
    }

    public static boolean removeTagsChangedHandler(TagsChangedHandler h) {
        return DEFAULT_INSTANCE._removeTagsChangedHandler(h);
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

    public boolean _addItemRangeInsertedHandler(RangeChangedHandler h) {
        return insertHandlers.add(h);
    }

    public boolean _removeItemRangeInsertedHandler(RangeChangedHandler h) {
        return insertHandlers.remove(h);
    }

    public boolean _addItemRangeRemovedHandler(RangeChangedHandler h) {
        return removeHandlers.add(h);
    }

    public boolean _removeItemRangeRemovedHandler(RangeChangedHandler h) {
        return removeHandlers.remove(h);
    }

    public boolean _addTagsChangedHandler(TagsChangedHandler h) {

        return tagsChangedHandlers.add(h);
    }

    public boolean _removeTagsChangedHandler(TagsChangedHandler h) {
        return tagsChangedHandlers.remove(h);
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
        for (RangeChangedHandler h : insertHandlers) {
            h.rangeChanged(this, positionStart, itemCount);
        }
    }

    private void _fireItemRemoved(int positionStart, int itemCount) {
        for (RangeChangedHandler h : removeHandlers) {
            h.rangeChanged(this, positionStart, itemCount);
        }
    }

    private void _fireTagsChanged() {
        for (TagsChangedHandler h : tagsChangedHandlers) {
            h.tagsChanged(this);
        }
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

    public interface RangeChangedHandler {
        void rangeChanged(Log log, int positionStart, int itemCount);
    }

    public interface TagsChangedHandler {
        void tagsChanged(Log log);
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

        public Date getTime() {
            return time;
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

package nist.p_70nanb17h188.demo.pscr19.logic.log;

import android.support.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;

public class Log {
    /**
     * The context for log events.
     */
    public static final String CONTEXT_LOG = "nist.p_70nanb17h188.demo.pscr19.logic.log";
    /**
     * Broadcast intent action indicating that a log item is added to the log list.
     * One extra {@link #EXTRA_LOG_ITEM_INSERTED} ({@link LogItem}) is the log item added.
     * Another extra {@link #EXTRA_LOG_ITEM_REMOVED} ({@link LogItem} is the log item removed.
     * Both could be null.
     * <p>
     * The latest logs can be retrieved from {@link #getLatestLogItems()}.
     */
    public static final String ACTION_LOG_ITEM_UPDATED = "nist.p_70nanb17h188.demo.pscr19.logic.log.itemUpdated";
    public static final String EXTRA_LOG_ITEM_INSERTED = "logItemInserted";
    public static final String EXTRA_LOG_ITEM_REMOVED = "logItemRemoved";

    public static final int DEFAULT_CAPACITY = 1000;

    private static Log DEFAULT_INSTANCE = null;
    private final ArrayList<LogItem> items;
    private final int capacity;

    private Log(int capacity) {
        items = new ArrayList<>(capacity);
        this.capacity = capacity;
    }

    public static void init(int capacity) {
        if (DEFAULT_INSTANCE == null) DEFAULT_INSTANCE = new Log(capacity);
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

    public static ArrayList<LogItem> getLatestLogItems() {
        return DEFAULT_INSTANCE._getLatestLogItems();
    }

    public static int getCapacity() {
        return DEFAULT_INSTANCE._getCapacity();
    }

    private void _v(String tag, String fmt, Object... params) {
        _v(tag, String.format(fmt, params));
    }

    private void _d(String tag, String fmt, Object... params) {
        _d(tag, String.format(fmt, params));
    }

    private void _i(String tag, String fmt, Object... params) {
        _i(tag, String.format(fmt, params));
    }

    private void _w(String tag, String fmt, Object... params) {
        _w(tag, String.format(fmt, params));
    }

    private void _e(String tag, String fmt, Object... params) {
        _e(tag, null, String.format(fmt, params));
    }

    private void _e(String tag, Throwable tr, String fmt, Object... params) {
        _e(tag, tr, String.format(fmt, params));
    }

    private void _v(String tag, String msg) {
        android.util.Log.v(tag, msg);
        _addLog(new LogItem(LogType.Verbose, tag, msg));
    }

    private void _d(String tag, String msg) {
        android.util.Log.d(tag, msg);
        _addLog(new LogItem(LogType.Debug, tag, msg));
    }

    private void _i(String tag, String msg) {
        android.util.Log.i(tag, msg);
        _addLog(new LogItem(LogType.Info, tag, msg));
    }

    private void _w(String tag, String msg) {
        android.util.Log.w(tag, msg);
        _addLog(new LogItem(LogType.Warn, tag, msg));
    }

    private void _e(String tag, String msg) {
        _e(tag, null, msg);
    }

    private void _e(String tag, Throwable tr, String msg) {
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

    private void _addLog(@NonNull LogItem item) {
        Intent intent = new Intent(ACTION_LOG_ITEM_UPDATED).putExtra(EXTRA_LOG_ITEM_INSERTED, item);
        synchronized (items) {
            if (items.size() == capacity) {
                intent.putExtra(EXTRA_LOG_ITEM_REMOVED, items.remove(capacity - 1));
            }
            items.add(0, item);
        }
        Context.getContext(CONTEXT_LOG).sendBroadcast(intent);
    }

    private int _getCapacity() {
        return capacity;
    }

    private ArrayList<LogItem> _getLatestLogItems() {
        synchronized (items) {
            return new ArrayList<>(items);
        }
    }
}

package nist.p_70nanb17h188.demo.pscr19.gui.log;

import android.support.annotation.NonNull;

import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;

public abstract class LogFilter {
    public static final LogFilter ACCEPT_ALL = new LogFilter() {
        @Override
        public boolean matches(@NonNull LogItem log) {
            return true;
        }
    };


    public interface LogFilterUpdatedListener {
        void logFilterUpdated(LogFilter filter);
    }

    private final HashSet<LogFilterUpdatedListener> listeners = new HashSet<>();

    void addLogFilterUpdatedListener(@NonNull LogFilterUpdatedListener listener) {
        listeners.add(listener);
    }

    void removeLogFilterUpdateListener(@NonNull LogFilterUpdatedListener listener) {
        listeners.remove(listener);
    }

    protected void fireLogFilterUpdated() {
        for (LogFilterUpdatedListener listener : listeners) {
            listener.logFilterUpdated(this);
        }
    }

    public abstract boolean matches(@NonNull LogItem log);
}

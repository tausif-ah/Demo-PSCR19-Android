package nist.p_70nanb17h188.demo.pscr19.imc;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;

public class Intent {
    @NonNull
    private final String action;

    private final HashMap<String, Object> extras = new HashMap<>();

    public Intent(@NonNull String action) {
        this.action = action;
    }

    public Intent putExtra(@NonNull String name, @NonNull Object extra) {
        synchronized (extras) {
            extras.put(name, extra);
            return this;
        }
    }

    @NonNull
    public String getAction() {
        return action;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getExtra(@NonNull String name) {
        synchronized (extras) {
            try {
                Object extra = extras.get(name);
                if (extra == null) return null;
                return (T) extra;
            } catch (ClassCastException e) {
                return null;
            }
        }
    }
}

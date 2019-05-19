package nist.p_70nanb17h188.demo.pscr19.imc;

import android.support.annotation.NonNull;

import java.util.HashSet;

public class IntentFilter {
    public interface ActionConsumer {
        void accept(String action);
    }

    private final HashSet<String> actions = new HashSet<>();


    public IntentFilter addAction(@NonNull String action) {
        synchronized (actions) {
            actions.add(action);
        }
        return this;
    }

    void forEachAction(ActionConsumer consumer) {
        synchronized (actions) {
            for (String action : actions)
                consumer.accept(action);
        }
    }
}

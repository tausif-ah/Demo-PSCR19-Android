package nist.p_70nanb17h188.demo.pscr19.imc;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Context {
    private static final HashMap<String, Context> EXISTING_CONTEXTS = new HashMap<>();

    /**
     * Gets an existing context. If it does not exist, create one.
     *
     * @param name The unique name of the context.
     * @return The context related to the name.
     */
    @NonNull
    public static Context getContext(@NonNull String name) {
        synchronized ((EXISTING_CONTEXTS)) {
            Context ret = EXISTING_CONTEXTS.get(name);
            if (ret == null) EXISTING_CONTEXTS.put(name, ret = new Context());
            return ret;
        }
    }


    private final HashMap<String, HashSet<BroadcastReceiver>> broadcastReceivers = new HashMap<>();

    public void registerReceiver(@NonNull BroadcastReceiver broadcastReceiver, @NonNull IntentFilter filter) {
        synchronized (broadcastReceivers) {
            filter.forEachAction(a -> {
                HashSet<BroadcastReceiver> receivers = broadcastReceivers.get(a);
                if (receivers == null)
                    broadcastReceivers.put(a, receivers = new HashSet<>());
                receivers.add(broadcastReceiver);
            });
        }
    }

    public void unregisterReceiver(@NonNull BroadcastReceiver broadcastReceiver) {
        synchronized (broadcastReceivers) {
            ArrayList<String> toRemoves = new ArrayList<>();
            for (Map.Entry<String, HashSet<BroadcastReceiver>> entry : broadcastReceivers.entrySet()) {
                HashSet<BroadcastReceiver> receivers = entry.getValue();
                receivers.remove(broadcastReceiver);
                if (receivers.size() == 0) toRemoves.add(entry.getKey());
            }
            for (String toRemove : toRemoves) {
                broadcastReceivers.remove(toRemove);
            }
        }
    }

    public void sendBroadcast(@NonNull Intent intent) {
        synchronized (broadcastReceivers) {
            HashSet<BroadcastReceiver> receivers = broadcastReceivers.get(intent.getAction());
            if (receivers != null) {
                for (BroadcastReceiver receiver : receivers) {
                    receiver.onReceive(this, intent);
                }
            }
        }
    }
}

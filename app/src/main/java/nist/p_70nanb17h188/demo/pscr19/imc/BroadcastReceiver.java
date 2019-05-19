package nist.p_70nanb17h188.demo.pscr19.imc;

import android.support.annotation.NonNull;

public interface BroadcastReceiver {
    /**
     * Receive callback for broadcast intents.
     * <p>
     * Do not modify the content in the intent! It will be reused across multiple mudules.
     *
     * @param context The context the broadcast is made.
     * @param intent  The intent of the broadcast.
     */
    void onReceive(@NonNull Context context, @NonNull Intent intent);
}

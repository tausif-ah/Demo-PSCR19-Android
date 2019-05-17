package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.app.Application;

public class NetLayer {
    private static NetLayer_Impl defaultInstance;

    public static void init(Application application) {
        defaultInstance = new NetLayer_Impl(application);
    }
}

package nist.p_70nanb17h188.demo.pscr19;

import android.app.Application;

public class MyApplication extends Application {
    private static MyApplication defaultInstance;

    public MyApplication() {
        defaultInstance = this;
    }


    public static MyApplication getDefaultInstance() {
        return defaultInstance;
    }
}

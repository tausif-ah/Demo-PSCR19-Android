package nist.p_70nanb17h188.demo.pscr19;

import android.app.Application;
import android.content.Context;

public class TheApplication extends Application {

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        TheApplication.appContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return TheApplication.appContext;
    }
}

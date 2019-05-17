package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.app.Application;
import android.support.annotation.NonNull;
import android.widget.Toast;

import nist.p_70nanb17h188.demo.pscr19.logic.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

final class LinkLayer_Impl {
    /**
     * Singleton pattern, prevent the class to be instantiated by the others.
     */
    LinkLayer_Impl(@NonNull Application application) {
        TCPConnectionManager instance = TCPConnectionManager.init();
        if (instance == null)
            Toast.makeText(application.getApplicationContext(), "Failed in creating TCPConnectionManager!", Toast.LENGTH_LONG).show();
        WifiLinkManager.init(application);
        WifiTCPConnectionManager.init(application);
        Log.d("LinkLayer_Impl", "%s initialized", Device.getName());
    }

    boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        // prefer Wifi over Bluetooth
        WifiTCPConnectionManager manager = WifiTCPConnectionManager.getDefaultInstance();
        if (manager == null) return false;
        return manager.sendData(id, data, start, len);
    }

}

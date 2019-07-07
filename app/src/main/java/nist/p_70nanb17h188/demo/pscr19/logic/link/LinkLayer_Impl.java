package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.support.annotation.NonNull;

import java.io.IOException;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public final class LinkLayer_Impl {
    private static final String TAG = "LinkLayer_Impl";
    //    @NonNull
//    private final TCPConnectionManager tcpConnectionManager;
    @NonNull
    private final ThreadTCPConnectionManager threadTCPConnectionManager;
    //    @NonNull
//    private final WifiTCPConnectionManager wifiTCPConnectionManager;
    @NonNull
    private final WifiThreadTCPConnectionManager wifiThreadTCPConnectionManager;
    @NonNull
    private final WifiLinkManager wifiLinkManager;

    /**
     * Singleton pattern, prevent the class to be instantiated by the others.
     */
    LinkLayer_Impl() {
//        tcpConnectionManager = new TCPConnectionManager();
        threadTCPConnectionManager = new ThreadTCPConnectionManager();
//        tcpConnectionManager.start();
        threadTCPConnectionManager.start();

        wifiLinkManager = new WifiLinkManager();

//        wifiTCPConnectionManager = WifiTCPConnectionManager.createWifiTCPConnectionManager(tcpConnectionManager);
        wifiThreadTCPConnectionManager = WifiThreadTCPConnectionManager.createWifiTCPConnectionManager(threadTCPConnectionManager);

        //        WifiTCPConnectionManager.init(context);
        Log.d("LinkLayer_Impl", "%s initialized", Device.getName());
    }

    boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        // prefer Wifi over Bluetooth
        Log.d(TAG, "send %d bytes to %s", len, id);
//        return wifiTCPConnectionManager.sendData(id, data, start, len);
        return wifiThreadTCPConnectionManager.sendData(id, data, start, len);

        // do bluetooth send
    }

//    @NonNull
//    public TCPConnectionManager getTcpConnectionManager() {
//        return tcpConnectionManager;
//    }

    @NonNull
    public ThreadTCPConnectionManager getThreadTCPConnectionManager() {
        return threadTCPConnectionManager;
    }

    @NonNull
    public WifiLinkManager getWifiLinkManager() {
        return wifiLinkManager;
    }

//    @NonNull
//    public WifiTCPConnectionManager getWifiTCPConnectionManager() {
//        return wifiTCPConnectionManager;
//    }

    @NonNull
    public WifiThreadTCPConnectionManager getWifiThreadTCPConnectionManager() {
        return wifiThreadTCPConnectionManager;
    }
}

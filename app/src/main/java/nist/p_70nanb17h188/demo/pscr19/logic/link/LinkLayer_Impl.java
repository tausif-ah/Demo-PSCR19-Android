package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl;

public final class LinkLayer_Impl {
    @NonNull
    private final TCPConnectionManager tcpConnectionManager;
    @NonNull
    private final WifiTCPConnectionManager wifiTCPConnectionManager;
    @NonNull
    private final WifiLinkManager wifiLinkManager;

    /**
     * Singleton pattern, prevent the class to be instantiated by the others.
     */
    LinkLayer_Impl() throws IOException {
        tcpConnectionManager = new TCPConnectionManager();
        tcpConnectionManager.start();
        wifiLinkManager = new WifiLinkManager();
        wifiTCPConnectionManager = WifiTCPConnectionManager.createWifiTCPConnectionManager(tcpConnectionManager);

        //        WifiTCPConnectionManager.init(context);
        Log.d("LinkLayer_Impl", "%s initialized", Device.getName());
    }

    boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        // prefer Wifi over Bluetooth
        //Log.v(NetLayer_Impl.TAG, "send data with "+ len +" bytes to "+id.getName());
        return wifiTCPConnectionManager.sendData(id, data, start, len);

        // do bluetooth send
    }

    @NonNull
    public TCPConnectionManager getTcpConnectionManager() {
        return tcpConnectionManager;
    }

    @NonNull
    public WifiLinkManager getWifiLinkManager() {
        return wifiLinkManager;
    }

    @NonNull
    public WifiTCPConnectionManager getWifiTCPConnectionManager() {
        return wifiTCPConnectionManager;
    }
}

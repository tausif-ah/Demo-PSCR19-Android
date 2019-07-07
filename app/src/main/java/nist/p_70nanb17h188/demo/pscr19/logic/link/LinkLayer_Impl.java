package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.support.annotation.NonNull;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public final class LinkLayer_Impl {
    private static final String TAG = "LinkLayer_Impl";
    //@NonNull
    //private final TCPConnectionManager tcpConnectionManager;
    @NonNull
    private final ThreadTCPConnectionManager threadTCPConnectionManager;
    @NonNull
    private final WifiLinkManager wifiLinkManager;
    //@NonNull
    //private final WifiTCPConnectionManager wifiTCPConnectionManager;
    @NonNull
    private final WifiThreadTCPConnectionManager wifiThreadTCPConnectionManager;
    @NonNull
    private final BluetoothLinkManager bluetoothLinkManager;
    @NonNull
    private final BluetoothThreadTCPConnectionManager bluetoothThreadTCPConnectionManager;

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

        bluetoothLinkManager = new BluetoothLinkManager();
        bluetoothThreadTCPConnectionManager = BluetoothThreadTCPConnectionManager.createBluetoothTCPConnectionManager(threadTCPConnectionManager);

        Log.d("LinkLayer_Impl", "%s initialized", Device.getName());
    }

    boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        // prefer Wifi over Bluetooth
        Log.d(TAG, "send %d bytes to %s", len, id);
//        return wifiTCPConnectionManager.sendData(id, data, start, len);
        boolean sent = wifiThreadTCPConnectionManager.sendData(id, data, start, len);
        if (sent) return true;
        // do bluetooth send
        return bluetoothThreadTCPConnectionManager.sendData(id, data, start, len);
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

    @NonNull
    public BluetoothLinkManager getBluetoothLinkManager() {
        return bluetoothLinkManager;
    }

    @NonNull
    public BluetoothThreadTCPConnectionManager getBluetoothThreadTCPConnectionManager() {
        return bluetoothThreadTCPConnectionManager;
    }
}

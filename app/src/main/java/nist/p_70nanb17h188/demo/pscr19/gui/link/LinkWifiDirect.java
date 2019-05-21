package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.net.wifi.p2p.WifiP2pDevice;

import nist.p_70nanb17h188.demo.pscr19.imc.DelayRunner;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.WifiTCPConnectionManager;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
//import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

class LinkWifiDirect extends Link {
    private static final long WIFI_DIRECT_CONNECTION_CLOSE_DELAY_MS = 500;
    private WifiP2pDevice deviceInDiscovery;

    LinkWifiDirect(String name) {
        super(name);
    }

    private void updateLinkStatus() {
        if (isTcpConnected()) {
            status.postValue(LinkStatus.TCPEstablished);
            establishConnection.postValue(true);
//            Log.d("LinkWifiDirect", "updateWifiDeviceList, state=%s, name=%s", status.getValue(), name);
            return;
        }
        if (deviceInDiscovery == null) {
            status.postValue(LinkStatus.NotFound);
            establishConnection.postValue(false);
        } else {
            switch (deviceInDiscovery.status) {
                case WifiP2pDevice.AVAILABLE:
//                    Log.d("LinkFragment", "updateWifiDeviceList, state=available, name=%s", deviceInDiscovery.deviceName);
                    status.postValue(LinkStatus.NotConnected);
                    establishConnection.postValue(false);
                    break;
                case WifiP2pDevice.INVITED:
//                    Log.d("LinkFragment", "updateWifiDeviceList, state=invited, name=%s", deviceInDiscovery.deviceName);
                    status.postValue(LinkStatus.Invited);
                    establishConnection.postValue(true);
                    break;
                case WifiP2pDevice.CONNECTED:
//                    Log.d("LinkFragment", "updateWifiDeviceList, state=connected, name=%s", deviceInDiscovery.deviceName);
                    status.postValue(LinkStatus.Connected);
                    establishConnection.postValue(true);
                    break;
//                case WifiP2pDevice.UNAVAILABLE:
//                case WifiP2pDevice.FAILED:
                default:
//                    Log.d("LinkFragment", "updateWifiDeviceList, state=unknown, name=%s", deviceInDiscovery.deviceName);
                    status.postValue(LinkStatus.NotFound);
                    establishConnection.postValue(false);
                    break;

            }
        }
    }

    void setDeviceInDiscovery(WifiP2pDevice deviceInDiscovery) {
        this.deviceInDiscovery = deviceInDiscovery;
        updateLinkStatus();
    }

    @Override
    void setTCPConnected(boolean tcpConnected) {
        super.setTCPConnected(tcpConnected);
//        Log.d("LinkWifiDirect", "setTCPConnected, connected=%s, %s, name=%s", tcpConnected, isTcpConnected(), name);
        updateLinkStatus();
    }

    @Override
    void onEstablishConnectionClick() {
        Log.d("LAUNCHER", "onEstablishConnectionClick start");
        DelayRunner.getDefaultInstance().post(() -> {
            if (deviceInDiscovery != null) {
                WifiTCPConnectionManager wifiTCPConnectionManager = LinkLayer.getDefaultImplementation().getWifiTCPConnectionManager();
                Boolean establishConnectionInverse = establishConnection.getValue();
                assert establishConnectionInverse != null;
                wifiTCPConnectionManager.modifyConnection(deviceInDiscovery.deviceName, !establishConnectionInverse);
            }
            DelayRunner.getDefaultInstance().postDelayed(WIFI_DIRECT_CONNECTION_CLOSE_DELAY_MS, () -> LinkLayer.getDefaultImplementation().getWifiLinkManager().modifyConnection(deviceInDiscovery));
        });
        Log.d("LAUNCHER", "onEstablishConnectionClick end");
    }
}
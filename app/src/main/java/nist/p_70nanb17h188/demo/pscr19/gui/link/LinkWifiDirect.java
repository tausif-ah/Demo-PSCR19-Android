package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.net.wifi.p2p.WifiP2pDevice;

import nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager;
//import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

class LinkWifiDirect extends Link {
    private WifiP2pDevice deviceInDiscovery;

    LinkWifiDirect(String name) {
        super(name);
    }

    private void updateLinkStatus() {
        if (isTcpConnected()) {
            status.setValue(LinkStatus.TCPEstablished);
            establishConnection.setValue(true);
//            Log.d("LinkWifiDirect", "updateWifiDeviceList, state=%s, name=%s", status.getValue(), name);
            return;
        }
        if (deviceInDiscovery == null) {
            status.setValue(LinkStatus.NotFound);
            establishConnection.setValue(false);
        } else {
            switch (deviceInDiscovery.status) {
                case WifiP2pDevice.AVAILABLE:
//                    Log.d("LinkFragment", "updateWifiDeviceList, state=available, name=%s", deviceInDiscovery.deviceName);
                    status.setValue(LinkStatus.NotConnected);
                    establishConnection.setValue(false);
                    break;
                case WifiP2pDevice.INVITED:
//                    Log.d("LinkFragment", "updateWifiDeviceList, state=invited, name=%s", deviceInDiscovery.deviceName);
                    status.setValue(LinkStatus.Invited);
                    establishConnection.setValue(true);
                    break;
                case WifiP2pDevice.CONNECTED:
//                    Log.d("LinkFragment", "updateWifiDeviceList, state=connected, name=%s", deviceInDiscovery.deviceName);
                    status.setValue(LinkStatus.Connected);
                    establishConnection.setValue(true);
                    break;
//                case WifiP2pDevice.UNAVAILABLE:
//                case WifiP2pDevice.FAILED:
                default:
//                    Log.d("LinkFragment", "updateWifiDeviceList, state=unknown, name=%s", deviceInDiscovery.deviceName);
                    status.setValue(LinkStatus.NotFound);
                    establishConnection.setValue(false);
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
        WifiLinkManager.getDefaultInstance().modifyConnection(deviceInDiscovery);
    }
}
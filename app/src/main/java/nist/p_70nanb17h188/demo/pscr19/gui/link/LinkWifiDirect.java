package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.net.wifi.p2p.WifiP2pDevice;

import nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager;

class LinkWifiDirect extends Link {
    private WifiP2pDevice deviceInDiscovery;

    LinkWifiDirect(String name) {
        super(name);
    }

    private void updateLinkStatus() {
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
                    //check if tcp established
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
    void onEstablishConnectionClick() {
        WifiLinkManager.getDefaultInstance().modifyConnection(deviceInDiscovery);
    }
}
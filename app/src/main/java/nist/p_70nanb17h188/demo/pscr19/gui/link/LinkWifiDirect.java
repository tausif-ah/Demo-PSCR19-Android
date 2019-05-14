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
            status.postValue(LinkStatus.NotFound);
            establishConnection.postValue(false);
        } else {
            switch (deviceInDiscovery.status) {
                case WifiP2pDevice.AVAILABLE:
                    status.postValue(LinkStatus.NotConnected);
                    establishConnection.postValue(false);
                    break;
                case WifiP2pDevice.INVITED:
                    status.postValue(LinkStatus.Invited);
                    establishConnection.postValue(true);
                    break;
                case WifiP2pDevice.CONNECTED:
                    //check if tcp established
                    status.postValue(LinkStatus.Connected);
                    establishConnection.postValue(true);
                    break;
//                case WifiP2pDevice.UNAVAILABLE:
//                case WifiP2pDevice.FAILED:
                default:
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
    void onEstablishConnectionClick() {
        WifiLinkManager.getDefaultInstance().modifyConnection(deviceInDiscovery);
    }
}
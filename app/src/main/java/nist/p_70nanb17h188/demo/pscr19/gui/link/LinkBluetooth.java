package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.bluetooth.BluetoothDevice;

class LinkBluetooth extends Link {

    private BluetoothDevice deviceInDiscovery;
    LinkBluetooth(String name) {
        super(name);
    }

    void setDeviceInDiscovery(BluetoothDevice deviceInDiscovery) {
        this.deviceInDiscovery = deviceInDiscovery;
        updateLinkStatus(LinkStatus.NotConnected);
    }

    private void updateLinkStatus(LinkStatus newStatus) {
        status.postValue(newStatus);
        establishConnection.postValue(false);
    }

    @Override
    void onEstablishConnectionClick() {

    }
}

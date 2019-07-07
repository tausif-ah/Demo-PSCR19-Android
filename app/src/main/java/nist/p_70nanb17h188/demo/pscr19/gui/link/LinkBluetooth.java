package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.bluetooth.BluetoothDevice;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.DelayRunner;
import nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothThreadTCPConnectionManager;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

class LinkBluetooth extends Link {
    private BluetoothDevice deviceInDiscovery;

    LinkBluetooth(String name) {
        super(name);
    }

    void setDeviceInDiscovery(BluetoothDevice deviceInDiscovery) {
        this.deviceInDiscovery = deviceInDiscovery;
        updateLinkStatus();
    }

    @Override
    void setTCPConnected(boolean tcpConnected) {
        super.setTCPConnected(tcpConnected);
//        Log.d("LinkBluetooth", "setTCPConnected, connected=%s, %s, name=%s", tcpConnected, isTcpConnected(), name);
        updateLinkStatus();
    }

    @Override
    void onEstablishConnectionClick() {
        Log.d("LAUNCHER", "onEstablishConnectionClick start");
        DelayRunner.getDefaultInstance().post(() -> {
            BluetoothThreadTCPConnectionManager bluetoothThreadTCPConnectionManager = LinkLayer.getDefaultImplementation().getBluetoothThreadTCPConnectionManager();
            Boolean establishConnectionInverse = establishConnection.getValue();
            assert establishConnectionInverse != null;
            if (deviceInDiscovery != null) {
                bluetoothThreadTCPConnectionManager.modifyConnection(deviceInDiscovery, !establishConnectionInverse);
            } else {
                if (!establishConnectionInverse) {
                    Helper.notifyUser(LogType.Info, "Cannot connect to a Bluetooth device %s, not discovered!", name);
                } else {
                    if (!bluetoothThreadTCPConnectionManager.closeConnection(name))
                        Helper.notifyUser(LogType.Info, "Cannot disconnect bluetooth link %s", name);

                }
            }//            DelayRunner.getDefaultInstance().postDelayed(WIFI_DIRECT_CONNECTION_CLOSE_DELAY_MS, () -> LinkLayer.getDefaultImplementation().getWifiLinkManager().modifyConnection(deviceInDiscovery));
        });
        Log.d("LAUNCHER", "onEstablishConnectionClick end");
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
            status.postValue(LinkStatus.NotConnected);
            establishConnection.postValue(false);
        }
    }

}

package nist.p_70nanb17h188.demo.pscr19.link;

import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;

public class Link {
    public int linkType;
    public WifiP2pDevice wifiP2pDevice;
    public BluetoothDevice bluetoothDevice;

    Link(int linkType, WifiP2pDevice wifiP2pDevice, BluetoothDevice bluetoothDevice) {
        this.linkType = linkType;
        this.wifiP2pDevice = wifiP2pDevice;
        this.bluetoothDevice = bluetoothDevice;
    }
}

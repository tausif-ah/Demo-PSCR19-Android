package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.bluetooth.BluetoothAdapter;

import nist.p_70nanb17h188.demo.pscr19.Device;

class BluetoothLinkManager {

    private BluetoothAdapter bluetoothAdapter;

    BluetoothLinkManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        setBluetoothName();
    }

    private void setBluetoothName() {
        String name = Device.getName();
        bluetoothAdapter.setName(name);
    }
}

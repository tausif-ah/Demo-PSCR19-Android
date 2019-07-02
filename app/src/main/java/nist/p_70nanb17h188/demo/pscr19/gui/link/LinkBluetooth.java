package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.util.UUID;

import nist.p_70nanb17h188.demo.pscr19.logic.link.Constants;

class LinkBluetooth extends Link {

    private BluetoothDevice deviceInDiscovery;
    private BluetoothSocket bluetoothSocket;
    LinkBluetooth(String name) {
        super(name);
        this.bluetoothSocket = null;
    }

    void setDeviceInDiscovery(BluetoothDevice deviceInDiscovery) {
        this.deviceInDiscovery = deviceInDiscovery;
        updateLinkStatus(LinkStatus.NotConnected, false);
    }

    void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
        boolean connectionStatus;
        if (bluetoothSocket != null) {
            connectionStatus = true;
            updateLinkStatus(LinkStatus.TCPEstablished, connectionStatus);
            DataListner dataListner = new DataListner(this.bluetoothSocket);
            dataListner.start();
        }
    }

    private void updateLinkStatus(LinkStatus newStatus, boolean connectionStatus) {
        status.postValue(newStatus);
        establishConnection.postValue(connectionStatus);
    }

    @Override
    void onEstablishConnectionClick() {
        try {
            bluetoothSocket = deviceInDiscovery.createRfcommSocketToServiceRecord(UUID.fromString(Constants.MY_UUID));
            if (bluetoothSocket != null) {
                bluetoothSocket.connect();
                updateLinkStatus(LinkStatus.TCPEstablished, true);
                DataListner dataListener = new DataListner(bluetoothSocket);
                dataListener.start();
            }
        } catch (Exception ex) {

        }
    }

    private class DataListner extends Thread {
        private BluetoothSocket bluetoothSocket;
        private boolean threadStarted;

        DataListner(BluetoothSocket bluetoothSocket) {
            this.bluetoothSocket = bluetoothSocket;
            this.threadStarted = false;
        }

        @Override
        public void run() {
            super.run();
            if (!threadStarted) {
                threadStarted = true;
            }
        }
    }
}

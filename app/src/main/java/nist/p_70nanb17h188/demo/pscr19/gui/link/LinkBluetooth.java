package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.link.Constants;

class LinkBluetooth extends Link {

    private BluetoothDevice deviceInDiscovery;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
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
            try {
                this.inputStream = this.bluetoothSocket.getInputStream();
            } catch (Exception ex) {

            }
            dataListner.start();
            byte[] dataToSend = Device.getName().getBytes();
//            TODO send data
            sendData(dataToSend);
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
                this.inputStream = bluetoothSocket.getInputStream();
                dataListener.start();
                byte[] dataToSend = Device.getName().getBytes();
//                TODO send data
                sendData(dataToSend);
            }
        } catch (Exception ex) {

        }
    }

    private void sendData(byte[] data) {
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(data);
            outputStream.flush();
        } catch (Exception writeEx) {
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
            byte [] readBuffer = new byte[1500];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = inputStream.read(readBuffer);
                    String receivedData = new String(readBuffer);
                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}

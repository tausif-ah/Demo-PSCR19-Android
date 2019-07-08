package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.link.Constants;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

class LinkBluetooth extends Link {

    private BluetoothDevice deviceInDiscovery;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;

    private static final int MAGIC = 0xdeadbeef;
    private static final byte TYPE_NAME = 1;
    private static final byte TYPE_KEEP_ALIVE = 2;
    private static final byte TYPE_DATA = 3;
    private static final byte TYPE_CONNECTION_CLOSE = 4;

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
            byte[] dataToSend = Device.getName().getBytes();
            sendData(TYPE_NAME, dataToSend);
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
                byte[] dataToSend = Device.getName().getBytes();
                sendData(TYPE_NAME, dataToSend);
            }
        } catch (Exception ex) {

        }
    }

    private void sendData(byte type, byte[] data) {
        synchronized (this) {
            try {
                OutputStream outputStream = bluetoothSocket.getOutputStream();

                ByteBuffer magicBuffer = ByteBuffer.allocate(Helper.INTEGER_SIZE);
                magicBuffer.putInt(MAGIC);
                magicBuffer.rewind();
                outputStream.write(magicBuffer.array());
                outputStream.flush();

//            writing type byte
                ByteBuffer typeBuffer = ByteBuffer.allocate(1);
                typeBuffer.put(type);
                typeBuffer.rewind();
                outputStream.write(typeBuffer.array());
                outputStream.flush();

//            writing size bytes
                ByteBuffer sizeBuffer = ByteBuffer.allocate(4);
                sizeBuffer.putInt(data.length);
                sizeBuffer.rewind();
                outputStream.write(sizeBuffer.array());
                outputStream.flush();

                int fullWriteCount = data.length / Constants.BLUETOOTH_DATA_CHUNK_SIZE;
                int remainder = data.length % Constants.BLUETOOTH_DATA_CHUNK_SIZE;
                int srcPos = 0;
                for (int i=0; i<fullWriteCount; i++) {
                    byte[] writeBuffer = new byte[Constants.BLUETOOTH_DATA_CHUNK_SIZE];
                    System.arraycopy(data, srcPos, writeBuffer, 0, Constants.BLUETOOTH_DATA_CHUNK_SIZE);
                    outputStream.write(writeBuffer);
                    outputStream.flush();
                    srcPos += Constants.BLUETOOTH_DATA_CHUNK_SIZE;
                }
                if (remainder > 0) {
                    byte[] writeBuffer = new byte[remainder];
                    System.arraycopy(data, srcPos, writeBuffer, 0, remainder);
                    outputStream.write(writeBuffer);
                    outputStream.flush();
                }
            } catch (Exception writeEx) {
            }
        }
    }

    private class DataListner extends Thread {

        DataListner(BluetoothSocket bluetoothSocket) {
            try {
                inputStream = bluetoothSocket.getInputStream();
            } catch (Exception ex) {

            }
        }

        @Override
        public void run() {
            super.run();
            byte[] magicBuffer = new byte[Helper.INTEGER_SIZE];
            byte[] typeBuffer = new byte[1];
            byte[] sizeBuffer = new byte[4];
            int numBytes;

            while (true) {
                try {
                    numBytes = inputStream.read(magicBuffer);
                    int magic = ByteBuffer.wrap(magicBuffer).getInt();
                    Log.d("BT received magic", String.valueOf(magic));
//                    reading type byte
                    numBytes = inputStream.read(typeBuffer);
                    byte type = ByteBuffer.wrap(typeBuffer).get();

//                    reading size bytes
                    numBytes = inputStream.read(sizeBuffer);
                    int size = ByteBuffer.wrap(sizeBuffer).getInt();

                    Log.d("BT data type", String.valueOf(type));
                    Log.d("BT data size", String.valueOf(size));

                    byte[] receivedData = new byte[size];
                    int destPos = 0;
                    byte[] readBuffer;
                    int fullRead = size / Constants.BLUETOOTH_DATA_CHUNK_SIZE;
                    int remainder = size % Constants.BLUETOOTH_DATA_CHUNK_SIZE;
                    for (int i=0; i<fullRead; i++) {
                        readBuffer = new byte[Constants.BLUETOOTH_DATA_CHUNK_SIZE];
                        numBytes = inputStream.read(readBuffer);
                        System.arraycopy(readBuffer, 0, receivedData, destPos, readBuffer.length);
                        destPos += Constants.BLUETOOTH_DATA_CHUNK_SIZE;
                    }
                    if (remainder  > 0) {
                        readBuffer = new byte[remainder];
                        numBytes = inputStream.read(readBuffer);
                        System.arraycopy(readBuffer, 0, receivedData, destPos, readBuffer.length);
                    }
                    Log.d("BT data recv", new String(receivedData));
                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}

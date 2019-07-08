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
        Boolean connectionStatus = establishConnection.getValue();
        assert connectionStatus != null;
        if (!connectionStatus) {
            try {
                Log.d("BT action", "connect");
                bluetoothSocket = deviceInDiscovery.createRfcommSocketToServiceRecord(UUID.fromString(Constants.MY_UUID));
                if (bluetoothSocket != null) {
                    bluetoothSocket.connect();
                    updateLinkStatus(LinkStatus.TCPEstablished, true);
                    DataListner dataListener = new DataListner(bluetoothSocket);
                    dataListener.start();
//                exchanging names
                    byte[] dataToSend = Device.getName().getBytes();
                    sendData(TYPE_NAME, dataToSend);
                }
            } catch (Exception ex) {

            }
        }
        else {
            Log.d("BT action", "disconnect");
            closeConnection(true);
        }
    }

    private void closeConnection(boolean initiator) {
        if (initiator) {
            sendData(TYPE_CONNECTION_CLOSE, new byte[0]);
        }
        try {
            inputStream.close();
            bluetoothSocket.close();
            updateLinkStatus(LinkStatus.NotConnected, false);
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

//            writing length bytes
                ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
                lengthBuffer.putInt(data.length);
                lengthBuffer.rewind();
                outputStream.write(lengthBuffer.array());
                outputStream.flush();

//                writing data
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
            byte[] lengthBuffer = new byte[Helper.INTEGER_SIZE];
            int numBytes;

            while (true) {
                try {
                    numBytes = inputStream.read(magicBuffer);
                    int magic = ByteBuffer.wrap(magicBuffer).getInt();
                    Log.d("BT received magic", String.valueOf(magic));

//                    reading type byte+
                    numBytes = inputStream.read(typeBuffer);
                    byte type = ByteBuffer.wrap(typeBuffer).get();
                    Log.d("BT data type", String.valueOf(type));

//                    reading size bytes
                    numBytes = inputStream.read(lengthBuffer);
                    int length = ByteBuffer.wrap(lengthBuffer).getInt();
                    Log.d("BT data length", String.valueOf(length));

                    if (magic != MAGIC || (type != TYPE_NAME && type != TYPE_DATA && type != TYPE_CONNECTION_CLOSE))
                        continue;

                    byte[] receivedData = new byte[length];
                    int destPos = 0;
                    byte[] readBuffer;
                    int totalRead = 0;
                    while (totalRead < length) {
                        readBuffer = new byte[Constants.BLUETOOTH_DATA_CHUNK_SIZE];
                        numBytes = inputStream.read(readBuffer);
                        System.arraycopy(readBuffer, 0, receivedData,destPos, numBytes);
                        destPos += numBytes;
                        totalRead += numBytes;
                    }

                    switch (type) {
                        case TYPE_NAME:
                            Log.d("BT name exchanged", new String(receivedData));
                            break;
                        case TYPE_DATA:
                            break;
                        case TYPE_CONNECTION_CLOSE:
                            Log.d("BT action", "disconnect");
                            closeConnection(false);
                            break;
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}

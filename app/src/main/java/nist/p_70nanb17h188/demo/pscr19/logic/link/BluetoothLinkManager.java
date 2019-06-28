package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.MyApplication;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class BluetoothLinkManager extends Thread{

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    public static final String CONTEXT_BLUETOOTH_LINK_MANAGER = "nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager";
    public static final String ACTION_BLUETOOTH_DEVICE_FOUND = "nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager.deviceFound";
    public static final String EXTRA_DEVICE = "device";
    private BluetoothServerSocket bluetoothServerSocket;
    private BluetoothSocket bluetoothSocket1;
    private BluetoothSocket bluetoothSocket2;
    private InputStream inputStream1;
    private InputStream inputStream2;

    BluetoothLinkManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevices = new ArrayList<>();
        setBluetoothName();
        bluetoothSocket1 = null;
        bluetoothSocket2 = null;
//        try {
//            bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(Constants.RF_COMM_LISTENER, UUID.fromString(Constants.MY_UUID));
//        }catch (IOException ex) {
//
//        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        Context context = MyApplication.getDefaultInstance().getApplicationContext();
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null)
                    return;
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        BluetoothDeviceFound(device);
                        break;
                }
            }
        }, intentFilter);
    }

    private void setBluetoothName() {
        String name = Device.getName();
        bluetoothAdapter.setName(name);
    }

    public void startDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            if (bluetoothDevices.size() > 0)
                bluetoothDevices.clear();
            bluetoothDevices = new ArrayList<>();
        }
        bluetoothAdapter.startDiscovery();
    }

    private void BluetoothDeviceFound(BluetoothDevice newDevice) {
        String deviceName = newDevice.getName();
        if (deviceName == null)
            return;
        if (Device.isNameExists(deviceName)) {
//            checking if device is already found and added
            boolean isNewDevice = true;
            for (BluetoothDevice oldDevice: bluetoothDevices
                 ) {
                if (oldDevice.getName().equals(deviceName)) {
                    isNewDevice = false;
                    break;
                }
            }
            if (isNewDevice) {
                android.util.Log.d("newBtFromManager", newDevice.getName());
                bluetoothDevices.add(newDevice);
                nist.p_70nanb17h188.demo.pscr19.imc.Context.getContext(CONTEXT_BLUETOOTH_LINK_MANAGER).sendBroadcast(new nist.p_70nanb17h188.demo.pscr19.imc.Intent(ACTION_BLUETOOTH_DEVICE_FOUND).putExtra(EXTRA_DEVICE, newDevice));
            }
        }
    }

    @Override
    public void run() {
        super.run();
        BluetoothSocket bluetoothSocket;
        byte buffer1[] = new byte[500];
        while (true) {
            try {
                if (bluetoothSocket1 == null) {
                    bluetoothSocket1 = bluetoothServerSocket.accept();
                    inputStream1 = bluetoothSocket1.getInputStream();
                }
                else {
                    bluetoothSocket2 = bluetoothServerSocket.accept();
                }
                int numbytes = inputStream1.read(buffer1);
                String data;
                if (numbytes > 0) {
                    data = new String(buffer1);
                    Log.i("Name Received", data);
                }
            } catch (IOException ex) {

            }
        }
    }

    private void connect(BluetoothDevice device) {
        if (bluetoothSocket1 == null) {
            try {
                bluetoothSocket1 = device.createRfcommSocketToServiceRecord(UUID.fromString(Constants.MY_UUID));
                bluetoothSocket1.connect();
                inputStream1 = bluetoothSocket1.getInputStream();
//                write(bluetoothSocket1, Device.getName());
            } catch (IOException ex) {

            }
        }
    }

    private void write(BluetoothSocket bluetoothSocket, String data) {
        try {
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(data.getBytes());
            outputStream.flush();
        } catch (IOException writeEx) {
        }
    }
}

package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.MyApplication;

public class BluetoothLinkManager extends Thread{

    private BluetoothAdapter bluetoothAdapter;
    private int timeSlotNo;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    public static final String CONTEXT_BLUETOOTH_LINK_MANAGER = "nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager";
    public static final String ACTION_BLUETOOTH_LIST_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager.listChanged";
    public static final String EXTRA_DEVICE_LIST = "deviceList";
    private BluetoothServerSocket bluetoothServerSocket;

    BluetoothLinkManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        timeSlotNo = 0;
        bluetoothDevices = new ArrayList<>();
        setBluetoothName();
        makeBluetoothDeviceDiscoverable();
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
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new controlBluetoothDiscovery(), 0, Constants.BLUETOOTH_DISCOVERY_CYCLE_LENGTH * 1000);
    }

    private void setBluetoothName() {
        String name = Device.getName();
        bluetoothAdapter.setName(name);
    }

    private void makeBluetoothDeviceDiscoverable() {
        android.content.Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, Constants.BLUETOOTH_dISCOVERABLE_LENGTH);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MyApplication.getDefaultInstance().getApplicationContext().startActivity(intent);
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
            if (isNewDevice)
                bluetoothDevices.add(newDevice);
        }
    }

    @Override
    public void run() {
        super.run();
        BluetoothSocket bluetoothSocket;
        while (true) {
            try {
                bluetoothSocket = bluetoothServerSocket.accept();

            } catch (IOException ex) {

            }
        }
    }

    private class controlBluetoothDiscovery extends TimerTask {
        @Override
        public void run() {
            if (timeSlotNo%2==0) {
                bluetoothDevices = new ArrayList<>();
//                adding paired devices
                bluetoothDevices.addAll(bluetoothAdapter.getBondedDevices());
//                initiating discovery
                bluetoothAdapter.startDiscovery();
            }
            else {
//                canceling discovery
                bluetoothAdapter.cancelDiscovery();
                nist.p_70nanb17h188.demo.pscr19.imc.Context.getContext(CONTEXT_BLUETOOTH_LINK_MANAGER).sendBroadcast(new nist.p_70nanb17h188.demo.pscr19.imc.Intent(ACTION_BLUETOOTH_LIST_CHANGED).putExtra(EXTRA_DEVICE_LIST, bluetoothDevices));
            }
            timeSlotNo++;
        }
    }
}

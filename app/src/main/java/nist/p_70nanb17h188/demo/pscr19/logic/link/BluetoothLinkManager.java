package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.MyApplication;

public class BluetoothLinkManager extends Thread{

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    public static final String CONTEXT_BLUETOOTH_LINK_MANAGER = "nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager";
    public static final String ACTION_BLUETOOTH_DEVICE_FOUND = "nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager.deviceFound";
    public static final String EXTRA_DEVICE = "device";
    private BluetoothServerSocket bluetoothServerSocket;

    BluetoothLinkManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.setName(Device.getName());
        bluetoothDevices = new ArrayList<>();
        try {
            this.bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(Constants.RF_COMM_LISTENER, UUID.fromString(Constants.MY_UUID));
        }catch (IOException ex) {

        }
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
                bluetoothDevices.add(newDevice);
                nist.p_70nanb17h188.demo.pscr19.imc.Context.getContext(CONTEXT_BLUETOOTH_LINK_MANAGER).sendBroadcast(new nist.p_70nanb17h188.demo.pscr19.imc.Intent(ACTION_BLUETOOTH_DEVICE_FOUND).putExtra(EXTRA_DEVICE, newDevice));
            }
        }
    }

    @Override
    public void run() {
        super.run();
        BluetoothSocket bluetoothSocket;
        while (true) {
            try {
                bluetoothSocket = bluetoothServerSocket.accept();
                BluetoothDevice remoteDevice = bluetoothSocket.getRemoteDevice();
                Log.d("remote device name", remoteDevice.getName());
            } catch (Exception ex) {

            }
        }
    }

    private void connect(BluetoothDevice device) {
    }
}

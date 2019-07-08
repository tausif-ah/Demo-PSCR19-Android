package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContextWrapper;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.MyApplication;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;


public class BluetoothLinkManager {
    private static final String TAG = "BluetoothLinkManager";
    /**
     * The context for bluetooth link manager events.
     */
    public static final String CONTEXT_BLUETOOTH_LINK_MANAGER = "nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager";
    /**
     * Broadcast intent action indicating that the discovery state of the device has changed.
     * One extra {@link #EXTRA_IS_DISCOVERING} ({@link Boolean}) indicates if the discovery is working.
     * <p>
     * The value is also available with function {@link #isBluetoothDiscovering()}.
     */
    public static final String ACTION_BLUETOOTH_DISCOVERY_STATE_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager.discoveryStateChanged";
    public static final String EXTRA_IS_DISCOVERING = "isDiscovering";

    /**
     * Broadcast intent action indicating that the discovered bluetooth device list has changed.
     * One extra {@link #EXTRA_TIME} ({@link Date}) indicates time the change is observed.
     * Another extra {@link #EXTRA_DEVICE_LIST} ({@link BluetoothDevice[]}) indicates the new device list.
     * <p>
     * The values are also available with functions {@link #getLastDiscoverTime()} and {@link #getLastDiscoverList()}.
     */
    public static final String ACTION_BLUETOOTH_LIST_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager.listChanged";
    public static final String EXTRA_DEVICE_LIST = "deviceList";
    public static final String EXTRA_TIME = "time";


    private final BluetoothAdapter bluetoothAdapter;
    private final boolean enabled;
    private boolean discovering = false;
    private Date lastUpdateTime = null;
    private ArrayList<BluetoothDevice> foundDevices = new ArrayList<>();

    BluetoothLinkManager() {
        android.content.Context context = MyApplication.getDefaultInstance().getApplicationContext();
//        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(android.content.Context.BLUETOOTH_SERVICE);
//        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        if (Constants.getBluetoothNeighbors().length > 0) {
            enabled = true;
            android.content.IntentFilter filter = new android.content.IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
            context.registerReceiver(new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, @NonNull android.content.Intent intent) {
                    if (intent.getAction() == null) return;
                    switch (intent.getAction()) {
                        case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                            onConnectionStateChanged(context, intent);
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                            onDiscoveryStopped(context, intent);
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                            onDiscoveryStarted(context, intent);
                            break;
                        case BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED:
                            onLocalNameChanged(context, intent);
                            break;
                        case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                            onScanModeChanged(context, intent);
                            break;
                        case BluetoothAdapter.ACTION_STATE_CHANGED:
                            onStateChanged(context, intent);
                            break;
                        case BluetoothDevice.ACTION_FOUND:
                            onDeviceFound(context, intent);
                            break;
                        case BluetoothDevice.ACTION_NAME_CHANGED:
                            onDeviceNameChanged(context, intent);
                            break;
                    }
                }
            }, filter);

            discovering = bluetoothAdapter.isDiscovering();
            bluetoothAdapter.setName(Device.getName());


        } else {
            enabled = false;
            bluetoothAdapter.cancelDiscovery();
            bluetoothAdapter.setName(Helper.getRandomString(5, 10, Helper.CANDIDATE_CHARSET_LETTERS_NUMBERS));
            Log.d(TAG, "my name: %s", bluetoothAdapter.getName());
            Log.d(TAG, "Bluetooth module disabled");
        }
    }

    private void requestDiscoverable(ContextWrapper context) {
        android.content.Intent intent = new android.content.Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, Constants.BLUETOOTH_DISCOVERABLE_DURATION);
        context.startActivity(intent);
//        MyApplication.getDefaultInstance().getApplicationContext().startActivity(intent);
    }

    public void discoverPeers(ContextWrapper context) {
        if (!enabled) return;
        requestDiscoverable(context);
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        foundDevices.clear();
        lastUpdateTime = new Date();
        Context.getContext(CONTEXT_BLUETOOTH_LINK_MANAGER).sendBroadcast(new Intent(ACTION_BLUETOOTH_LIST_CHANGED).putExtra(EXTRA_DEVICE_LIST, foundDevices.toArray(new BluetoothDevice[0])).putExtra(EXTRA_TIME, lastUpdateTime));
        bluetoothAdapter.startDiscovery();
    }

    public boolean isBluetoothDiscovering() {
        return discovering;
    }

    public Date getLastDiscoverTime() {
        return lastUpdateTime;
    }

    public BluetoothDevice[] getLastDiscoverList() {
        return foundDevices.toArray(new BluetoothDevice[0]);
    }

    private void onConnectionStateChanged(android.content.Context context, android.content.Intent intent) {
        Log.v(TAG, "onConnectionStateChanged: %s", intent);
    }


    private void onDiscoveryStarted(android.content.Context context, android.content.Intent intent) {
        discovering = true;
        Helper.notifyUser(LogType.Info, "Bluetooth discovery started!");

        Context.getContext(CONTEXT_BLUETOOTH_LINK_MANAGER).sendBroadcast(new Intent(ACTION_BLUETOOTH_DISCOVERY_STATE_CHANGED).putExtra(EXTRA_IS_DISCOVERING, discovering));
    }

    private void onDiscoveryStopped(android.content.Context context, android.content.Intent intent) {
        discovering = false;
        Helper.notifyUser(LogType.Info, "Bluetooth discovery stopped!");
        Context.getContext(CONTEXT_BLUETOOTH_LINK_MANAGER).sendBroadcast(new Intent(ACTION_BLUETOOTH_DISCOVERY_STATE_CHANGED).putExtra(EXTRA_IS_DISCOVERING, discovering));
    }

    private void onStateChanged(android.content.Context context, @NonNull android.content.Intent intent) {
        Log.v(TAG, "onStateChanged: %s",
                getBluetoothState(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)),
                getBluetoothState(intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0)));

    }

    @NonNull
    private static String getBluetoothState(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
                return "off";
            case BluetoothAdapter.STATE_TURNING_ON:
                return "turning on";
            case BluetoothAdapter.STATE_ON:
                return "on";
            case BluetoothAdapter.STATE_TURNING_OFF:
                return "turning off";
            default:
                return "unknown";
        }
    }

    private void onScanModeChanged(android.content.Context context, @NonNull android.content.Intent intent) {
        int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
        int previousScanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, 0);
        Log.v(TAG, "onScanModeChanged: %d/%s %d/%s",
                scanMode, getBluetoothScanMode(scanMode),
                previousScanMode, getBluetoothScanMode(previousScanMode));
    }

    @NonNull
    private static String getBluetoothScanMode(int scanMode) {
        switch (scanMode) {
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                return "connectable";
            case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                return "connectable|discoverable";
            case BluetoothAdapter.SCAN_MODE_NONE:
                return "none";
            default:
                return "unknown";
        }
    }

    private void onLocalNameChanged(android.content.Context context, @NonNull android.content.Intent intent) {
        String name = intent.getStringExtra(BluetoothAdapter.EXTRA_LOCAL_NAME);
        Log.v(TAG, "onLocalNameChanged: %s", name);
        if (!Objects.equals(name, Device.getName())) {
            bluetoothAdapter.setName(Device.getName());
        }
    }

    private void onDeviceFound(android.content.Context context, @NonNull android.content.Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Log.v(TAG, "onDeviceFound: device=%s, name=%s, class=%s, rssi=%d",
                device,
                intent.getStringExtra(BluetoothDevice.EXTRA_NAME),
                intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS),
                intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0));
        foundDevices.add(device);
        lastUpdateTime = new Date();
        Context.getContext(CONTEXT_BLUETOOTH_LINK_MANAGER).sendBroadcast(new Intent(ACTION_BLUETOOTH_LIST_CHANGED).putExtra(EXTRA_DEVICE_LIST, foundDevices.toArray(new BluetoothDevice[0])).putExtra(EXTRA_TIME, lastUpdateTime));
    }

    private void onDeviceNameChanged(android.content.Context context, @NonNull android.content.Intent intent) {
        Log.v(TAG, "onDeviceNameChanged: device=%s, name=%s",
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE),
                intent.getStringExtra(BluetoothDevice.EXTRA_NAME));

    }
}

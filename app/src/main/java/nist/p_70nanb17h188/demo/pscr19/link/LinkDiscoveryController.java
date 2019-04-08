package nist.p_70nanb17h188.demo.pscr19.link;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

import nist.p_70nanb17h188.demo.pscr19.Constants;

public class LinkDiscoveryController {
    private Context context;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private IntentFilter intentFilter;
    private LinkBroadcastReceiver linkBroadcastReceiver;

    public LinkDiscoveryController(Context context) {
        this.context = context;
        linkBroadcastReceiver = new LinkBroadcastReceiver();
        linkBroadcastReceiver.setLinkDiscoveryController(this);
        intentFilter = new IntentFilter();
        configureWiFiDirect();
        configureBluetooth();
        context.registerReceiver(linkBroadcastReceiver, intentFilter);
    }

    private void configureWiFiDirect() {
        wifiP2pManager = (WifiP2pManager)context.getSystemService(Context.WIFI_P2P_SERVICE);
        linkBroadcastReceiver.setWifiP2pManager(wifiP2pManager);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        linkBroadcastReceiver.setChannel(channel);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    }

    void setWiFiDirectName(WifiP2pDevice hostDevice) {
        hostDevice.deviceName = Constants.hostName;
    }

    private void configureBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.setName(Constants.hostName);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
    }
}

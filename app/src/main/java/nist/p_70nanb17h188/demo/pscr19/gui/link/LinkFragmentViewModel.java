package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.bluetooth.BluetoothDevice;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothLinkManager;
import nist.p_70nanb17h188.demo.pscr19.logic.link.BluetoothThreadTCPConnectionManager;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager;
import nist.p_70nanb17h188.demo.pscr19.logic.link.WifiThreadTCPConnectionManager;

//import nist.p_70nanb17h188.demo.pscr19.logic.link.WifiTCPConnectionManager;

public class LinkFragmentViewModel extends ViewModel {
    static final String DATE_STRING_ON_NULL = "--:--:--.---";
    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    final MutableLiveData<WifiP2pGroup> wifiGroupInfo = new MutableLiveData<>();
    final MutableLiveData<Boolean> wifiDiscovering = new MutableLiveData<>(), bluetoothDiscovering = new MutableLiveData<>();
    final Link[] links;
    private final MutableLiveData<Date> wifiDiscoverUpdateTime = new MutableLiveData<>();
    private final MutableLiveData<Date> bluetoothUpdateTime = new MutableLiveData<>();
    private final Function<Date, String> updateTimeTransformation = d -> d == null ? DATE_STRING_ON_NULL : DEFAULT_TIME_FORMAT.format(d);
    final LiveData<String> strWifiDiscoverUpdateTime = Transformations.map(wifiDiscoverUpdateTime, updateTimeTransformation);
    final LiveData<String> strBluetoothUpdateTime = Transformations.map(bluetoothUpdateTime, updateTimeTransformation);
    private final BroadcastReceiver receiver = (context, intent) -> onReceiveBroadcastIntent(intent);

    public LinkFragmentViewModel() {
        links = Constants.getConnections();
        Arrays.sort(links, 0, links.length, (a, b) -> a.name.compareTo(b.name));
        Context.getContext(WifiLinkManager.CONTEXT_WIFI_LINK_MANAGER).registerReceiver(
                receiver,
                new IntentFilter()
                        .addAction(WifiLinkManager.ACTION_WIFI_GROUP_CHANGED)
                        .addAction(WifiLinkManager.ACTION_WIFI_DISCOVERY_STATE_CHANGED)
                        .addAction(WifiLinkManager.ACTION_WIFI_LIST_CHANGED)
        );
        Context.getContext(BluetoothLinkManager.CONTEXT_BLUETOOTH_LINK_MANAGER).registerReceiver(
                receiver,
                new IntentFilter()
                        .addAction(BluetoothLinkManager.ACTION_BLUETOOTH_DISCOVERY_STATE_CHANGED)
                        .addAction(BluetoothLinkManager.ACTION_BLUETOOTH_LIST_CHANGED)
        );
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).registerReceiver(
                receiver,
                new IntentFilter()
                        .addAction(LinkLayer.ACTION_LINK_CHANGED)
        );
        synchronizeData();
    }

    private void onReceiveBroadcastIntent(Intent intent) {
        String action = intent.getAction();
        switch (action) {
            case WifiLinkManager.ACTION_WIFI_DISCOVERY_STATE_CHANGED:
                wifiDiscovering.postValue(intent.getExtra(WifiLinkManager.EXTRA_IS_DISCOVERING));
                break;
            case WifiLinkManager.ACTION_WIFI_GROUP_CHANGED:
                wifiGroupInfo.postValue(intent.getExtra(WifiLinkManager.EXTRA_GROUP_INFO));
                break;
            case WifiLinkManager.ACTION_WIFI_LIST_CHANGED:
                wifiDiscoverUpdateTime.postValue(intent.getExtra(WifiLinkManager.EXTRA_TIME));
                updateWifiDeviceList(intent.getExtra(WifiLinkManager.EXTRA_DEVICE_LIST));
                break;
            case BluetoothLinkManager.ACTION_BLUETOOTH_DISCOVERY_STATE_CHANGED:
                bluetoothDiscovering.postValue(intent.getExtra(BluetoothLinkManager.EXTRA_IS_DISCOVERING));
                break;
            case BluetoothLinkManager.ACTION_BLUETOOTH_LIST_CHANGED:
                bluetoothUpdateTime.postValue(intent.getExtra(BluetoothLinkManager.EXTRA_TIME));
                updateBluetoothDeviceList(intent.getExtra(BluetoothLinkManager.EXTRA_DEVICE_LIST));
                break;
            case LinkLayer.ACTION_LINK_CHANGED:
                NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
                assert neighborID != null;
                String name = neighborID.getName();
                Boolean connected = intent.getExtra(LinkLayer.EXTRA_CONNECTED);
                assert connected != null;
                // Log.d(TAG, "received ACTION_TCP_CONNECTION_CHANGED %s tcp %s", name, connected ? "CONNECTED" : "DISCONNECTED");
                for (Link l : links) {
                    if (l.name.equals(name)) {
                        // Log.d(TAG, "received ACTION_TCP_CONNECTION_CHANGED %s tcp %s, l=%s", name, connected ? "CONNECTED" : "DISCONNECTED", l);
                        l.setTCPConnected(connected);
                    }
                }
                break;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).unregisterReceiver(receiver);
        Context.getContext(WifiLinkManager.CONTEXT_WIFI_LINK_MANAGER).unregisterReceiver(receiver);
    }

    private void synchronizeData() {
        WifiLinkManager wifiLinkManager = LinkLayer.getDefaultImplementation().getWifiLinkManager();
        wifiGroupInfo.postValue(wifiLinkManager.getLastGroupInfo());
        wifiDiscovering.postValue(wifiLinkManager.isWifiDiscovering());
        wifiDiscoverUpdateTime.postValue(wifiLinkManager.getLastDiscoverTime());
        updateWifiDeviceList(wifiLinkManager.getLastDiscoverList());

        BluetoothLinkManager bluetoothLinkManager = LinkLayer.getDefaultImplementation().getBluetoothLinkManager();
        bluetoothDiscovering.postValue(bluetoothLinkManager.isBluetoothDiscovering());
        bluetoothUpdateTime.postValue(bluetoothLinkManager.getLastDiscoverTime());
        updateBluetoothDeviceList(bluetoothLinkManager.getLastDiscoverList());

        updateTCPConnectionList();
    }

    private void updateTCPConnectionList() {
//        WifiTCPConnectionManager wifiTCPConnectionManager = LinkLayer.getDefaultImplementation().getWifiTCPConnectionManager();
        WifiThreadTCPConnectionManager wifiThreadTCPConnectionManager = LinkLayer.getDefaultImplementation().getWifiThreadTCPConnectionManager();
        for (Link l : links) {
            if (l instanceof LinkWifiDirect) {
//                l.setTCPConnected(wifiTCPConnectionManager.isDeviceTCPConnected(l.name));
                l.setTCPConnected(wifiThreadTCPConnectionManager.isDeviceTCPConnected(l.name));
            }
        }
        BluetoothThreadTCPConnectionManager bluetoothThreadTCPConnectionManager = LinkLayer.getDefaultImplementation().getBluetoothThreadTCPConnectionManager();
        for (Link l : links) {
            if (l instanceof LinkBluetooth) {
                l.setTCPConnected(bluetoothThreadTCPConnectionManager.isDeviceTCPConnected(l.name));
            }
        }
    }

    private void updateWifiDeviceList(WifiP2pDeviceList list) {
//            Log.d(TAG, "updateWifiDeviceList, list=%s", list);
        if (list == null) return;
        HashMap<String, LinkWifiDirect> remaining = new HashMap<>();
        for (Link l : links) {
            if (l instanceof LinkWifiDirect) remaining.put(l.name, (LinkWifiDirect) l);
        }
        for (WifiP2pDevice device : list.getDeviceList()) {
            String name = device.deviceName;
            if (name.startsWith("[Phone]")) name = name.substring(7).trim();

            LinkWifiDirect l = remaining.get(name);
            if (l == null) {
                continue;
            }
//                Log.d(TAG, "updateWifiDeviceList, name=%s, status=%d", device.deviceName, device.status);
            l.setDeviceInDiscovery(device);
            remaining.remove(name);
        }
        for (LinkWifiDirect l : remaining.values()) {
            l.setDeviceInDiscovery(null);
        }
    }

    private void updateBluetoothDeviceList(BluetoothDevice[] list) {
        if (list == null) return;
        HashMap<String, LinkBluetooth> remaining = new HashMap<>();
        for (Link l : links) {
            if (l instanceof LinkBluetooth) remaining.put(l.name, (LinkBluetooth) l);
        }
        for (BluetoothDevice device : list) {
            String name = device.getName();
            if (name == null) continue;
//            if (name.startsWith("[Phone]")) name = name.substring(7).trim();

            LinkBluetooth l = remaining.get(name);
            if (l == null) {
                continue;
            }
//                Log.d(TAG, "updateWifiDeviceList, name=%s, status=%d", device.deviceName, device.status);
            l.setDeviceInDiscovery(device);
            remaining.remove(name);
        }
        for (LinkBluetooth l : remaining.values()) {
            l.setDeviceInDiscovery(null);
        }
    }
}
package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager;
import nist.p_70nanb17h188.demo.pscr19.logic.link.WifiTCPConnectionManager;

class LinkFragmentViewModel extends ViewModel {
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
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onReceiveBroadcastIntent(intent);
        }
    };
    private Application application;

    public LinkFragmentViewModel() {
        links = Constants.getConnections();
        Arrays.sort(links, 0, links.length, (a, b) -> a.name.compareTo(b.name));
    }

    private void onReceiveBroadcastIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case WifiLinkManager.ACTION_WIFI_DISCOVERY_STATE_CHANGED:
                wifiDiscovering.postValue(intent.getBooleanExtra(WifiLinkManager.EXTRA_IS_DISCOVERING, false));
                break;
            case WifiLinkManager.ACTION_WIFI_GROUP_CHANGED:
                wifiGroupInfo.postValue(intent.getParcelableExtra(WifiLinkManager.EXTRA_GROUP_INFO));
                break;
            case WifiLinkManager.ACTION_WIFI_LIST_CHANGED:
                wifiDiscoverUpdateTime.postValue((Date) intent.getSerializableExtra(WifiLinkManager.EXTRA_TIME));
                updateWifiDeviceList(intent.getParcelableExtra(WifiLinkManager.EXTRA_DEVICE_LIST));
                break;
            case LinkLayer.ACTION_LINK_CHANGED:
                NeighborID neighborID = intent.getParcelableExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
                String name = neighborID.getName();
                boolean connected = intent.getBooleanExtra(LinkLayer.EXTRA_CONNECTED, false);
                // Log.d(TAG, "received ACTION_TCP_CONNECTION_CHANGED %s tcp %s", name, connected ? "CONNECTED" : "DISCONNECTED");
                for (Link l : links) {
                    if (l instanceof LinkWifiDirect && l.name.equals(name)) {
                        // Log.d(TAG, "received ACTION_TCP_CONNECTION_CHANGED %s tcp %s, l=%s", name, connected ? "CONNECTED" : "DISCONNECTED", l);
                        l.setTCPConnected(connected);
                    }
                }
                break;
        }
    }

    void setApplication(Application application) {
        synchronized (this) {
            if (this.application != null) return;
            this.application = application;
            synchronizeData();
        }
        Context context = application.getApplicationContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiLinkManager.ACTION_WIFI_GROUP_CHANGED);
        filter.addAction(WifiLinkManager.ACTION_WIFI_DISCOVERY_STATE_CHANGED);
        filter.addAction(WifiLinkManager.ACTION_WIFI_LIST_CHANGED);
        filter.addAction(LinkLayer.ACTION_LINK_CHANGED);

        context.registerReceiver(receiver, filter);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        synchronized (this) {
            if (application != null) {
                application.getApplicationContext().unregisterReceiver(receiver);
                application = null;
            }
        }
    }

    private void synchronizeData() {
        WifiLinkManager wifiLinkManager = WifiLinkManager.getDefaultInstance();
        if (wifiLinkManager != null) {
            wifiGroupInfo.postValue(wifiLinkManager.getLastGroupInfo());
            wifiDiscovering.postValue(wifiLinkManager.isWifiDiscovering());
            wifiDiscoverUpdateTime.postValue(wifiLinkManager.getLastDiscoverTime());
            wifiGroupInfo.postValue(wifiLinkManager.getLastGroupInfo());
            updateWifiDeviceList(wifiLinkManager.getLastDiscoverList());
            updateTCPConnectionList();
        }

        //if bluetoothLinkManager != null
        {
            bluetoothDiscovering.postValue(false);
            bluetoothUpdateTime.postValue(null);
        }
    }

    private void updateTCPConnectionList() {
        WifiTCPConnectionManager wifiTCPConnectionManager = WifiTCPConnectionManager.getDefaultInstance();
        if (wifiTCPConnectionManager != null) {
            for (Link l : links) {
                if (l instanceof LinkWifiDirect)
                    l.setTCPConnected(wifiTCPConnectionManager.isDeviceTCPConnected(l.name));
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
}
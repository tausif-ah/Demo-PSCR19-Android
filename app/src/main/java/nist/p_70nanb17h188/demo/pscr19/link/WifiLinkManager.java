package nist.p_70nanb17h188.demo.pscr19.link;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import android.os.Handler;

import org.jetbrains.annotations.NotNull;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Log;

public class WifiLinkManager {
    /**
     * Broadcast intent action indicating that the group status of the current device has changed.
     * One extra EXTRA_GROUP_NAME indicates the SSID of the group. If group is stopped, group name is null.
     * Another extra EXTRA_GROUP_PASS indicates the password of the group. If the group is stopped, group pass is null.
     * <p>
     * The values are also available with functions getGroupName() and getGroupPass().
     */
    public static final String ACTION_WIFI_GROUP_CHANGED = "nist.p_70nanb17h188.demo.pscr19.link.WifiLinkManager.groupChanged";
    public static final String EXTRA_GROUP_NAME = "groupName";
    public static final String EXTRA_GROUP_PASS = "groupPass";

    /**
     * Broadcast intent action indicating that the discovery state of the device has changed.
     * One extra EXTRA_IS_DISCOVERING indicates if the discovery is working.
     * <p>
     * The value is also available with function isWifiDiscovering().
     */
    public static final String ACTION_WIFI_DISCOVERY_STATE_CHANGED = "nist.p_70nanb17h188.demo.pscr19.link.WifiLinkManager.discoveryStateChanged";
    public static final String EXTRA_IS_DISCOVERING = "isDiscovering";

    /**
     * Broadcast intent action indicating that the discovered wifi p2p device list has changed.
     * One extra EXTRA_TIME indicates time the change is observed.
     * Another extra EXTRA_DEVICE_LIST indicates the new device list.
     * <p>
     * The values are also available with functions getLastDiscoverTime() and getLastDiscoverList().
     */
    public static final String ACTION_WIFI_LIST_CHANGED = "nist.p_70nanb17h188.demo.pscr19.link.WifiLinkManager.listChanged";
    public static final String EXTRA_DEVICE_LIST = "deviceList";
    public static final String EXTRA_TIME = "time";

    private static final String TAG = "WifiLinkManager";
    private static final int DEFAULT_DISCOVER_RETRY_DELAY_MS = 1000;
    //    private static final int DEFAULT_CLEANUP_RETRY_DELAY_MS = 1000;
    private static WifiLinkManager DEFAULT_INSTANCE;

    static void init(@NonNull Context context, @NonNull Handler handler) {
        if (DEFAULT_INSTANCE == null) {
            DEFAULT_INSTANCE = new WifiLinkManager(context, handler);
        }
    }

    public static WifiLinkManager getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }


    private final Context context;
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    private final Handler handler;
    private String groupName = null, groupPass = null;
    private boolean discovering = false;
    private Date lastDiscoverTime = null;
    private WifiP2pDeviceList lastDiscoverList = null;

    private WifiLinkManager(@NonNull Context context, @NonNull Handler handler) {
        this.context = context;
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), () -> Log.e(TAG, "Channel disconnected!"));
        IntentFilter filter = new IntentFilter();
//        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                Log.d(TAG, "linkBroadcastReceiver.onReceive, action=%s", action);
                switch (action) {
                    case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                        onPeersChanged(intent);
                        break;
                    case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                        onDiscoveryChanged(intent);
                        break;
                    case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                        onThisDeviceChanged(intent);
                        break;
                    case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                        onConnectionChanged(intent);
                        break;
                }
            }
        }, filter);
        Log.d(TAG, "Registered linkBroadcastReceiver");
        this.handler = handler;
        setWifiDirectName();
        createGroup();
        discoverPeers();
    }

    private void setWifiDirectName() {
        String name = Device.getName();

        setWifiDirectName(wifiP2pManager, channel, name, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Succeeded in changing name! My name: %s", name);
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failed in changing name! reason=%d", reason);
            }
        });
    }

    private void createGroup() {
        if (Constants.isWifiDirectGroupowner()) {
            wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Succeeded in creating group!");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Failed in creating group! reason=%d", reason);
                }
            });
        }
    }

    private void discoverPeers() {
//        Log.v(TAG, "Start discovering peers!");
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Initiated discovering peers!");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failed in discovering peers! reason=%d, retry in %dms", reason, DEFAULT_DISCOVER_RETRY_DELAY_MS);
                handler.postDelayed(WifiLinkManager.this::discoverPeers, DEFAULT_DISCOVER_RETRY_DELAY_MS);
            }
        });
//        handler.postDelayed(this::discoverPeers, DEFAULT_DISCOVER_DURATION_MS);
    }

    private void onThisDeviceChanged(@NotNull Intent intent) {
        WifiP2pDevice thisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        Log.i(TAG, "Device name: %s, isGroupOwner: %b", thisDevice.deviceName, thisDevice.isGroupOwner());
        if (Constants.isWifiDirectGroupowner()) {
            wifiP2pManager.requestGroupInfo(channel, WifiLinkManager.this::onGroupInfoAvailable);
        }
    }

    private void onGroupInfoAvailable(WifiP2pGroup group) {
//        Log.d(TAG, "onGroupInfoAvailable: %s", group);
        if (group != null) {
            groupName = group.getNetworkName();
            groupPass = group.getPassphrase();
            Log.i(TAG, "ssid=%s, pass=%s", groupName, groupPass);
            context.sendBroadcast(new Intent(ACTION_WIFI_GROUP_CHANGED).putExtra(EXTRA_GROUP_NAME, groupName).putExtra(EXTRA_GROUP_PASS, groupPass));
        } else {
            groupName = null;
            groupPass = null;
            Log.i(TAG, "ssid=%s, pass=%s", null, null);
            context.sendBroadcast(new Intent(ACTION_WIFI_GROUP_CHANGED).putExtra(EXTRA_GROUP_NAME, groupName).putExtra(EXTRA_GROUP_PASS, groupPass));
        }
    }

    private void onDiscoveryChanged(@NotNull Intent intent) {
        int discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
        switch (discoveryState) {
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                Log.i(TAG, "Link discovery started!");
                discovering = true;
                context.sendBroadcast(new Intent(ACTION_WIFI_DISCOVERY_STATE_CHANGED).putExtra(EXTRA_IS_DISCOVERING, discovering));
                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                Log.i(TAG, "Link discovery stopped!");
                discovering = false;
                context.sendBroadcast(new Intent(ACTION_WIFI_DISCOVERY_STATE_CHANGED).putExtra(EXTRA_IS_DISCOVERING, discovering));
                discoverPeers();
                break;
            default:
                Log.e(TAG, "Unknown link discovery state!");
                break;
        }
    }

    private void onPeersChanged(@NotNull Intent intent) {
        WifiP2pDeviceList deviceList = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
        StringBuilder debugBuilder = new StringBuilder(), infoBuilder = new StringBuilder();
        for (WifiP2pDevice device : deviceList.getDeviceList()) {
            debugBuilder.append('\n');
            debugBuilder.append(device);
            infoBuilder.append(String.format("\nDevice=%s(%s) %s", device.deviceName, device.deviceAddress, device.isGroupOwner() ? "M" : "S"));
        }
        Log.v(TAG, "onPeerListDiscovered (%d) T:%s%s", deviceList.getDeviceList().size(), Thread.currentThread(), debugBuilder.toString());
        Log.i(TAG, "onPeerListDiscovered (%d)%s", deviceList.getDeviceList().size(), infoBuilder.toString());
        this.lastDiscoverTime = new Date();
        this.lastDiscoverList = deviceList;
        context.sendBroadcast(new Intent(ACTION_WIFI_LIST_CHANGED).putExtra(EXTRA_DEVICE_LIST, deviceList).putExtra(EXTRA_TIME, lastDiscoverTime));
    }

    private void onConnectionChanged(@NotNull Intent intent) {
        // EXTRA_WIFI_P2P_INFO EXTRA_NETWORK_INFO EXTRA_WIFI_P2P_GROUP
        WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        WifiP2pGroup p2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
        Log.i(TAG, "connection changed!\np2pInfo=%s\nnetInfo=%s\np2pGroup=%s", p2pInfo, netInfo, p2pGroup);
    }


    public String getGroupName() {
        return groupName;
    }

    public String getGroupPass() {
        return groupPass;
    }

    public boolean isWifiDiscovering() {
        return discovering;
    }

    public Date getLastDiscoverTime() {
        return lastDiscoverTime;
    }

    public WifiP2pDeviceList getLastDiscoverList() {
        return lastDiscoverList;
    }

    private static void setWifiDirectName(@NotNull WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel, String name, WifiP2pManager.ActionListener listener) {
        try {
            Class[] paramTypes = new Class[]{WifiP2pManager.Channel.class, String.class, WifiP2pManager.ActionListener.class};
            Method setDeviceName = wifiP2pManager.getClass().getMethod("setDeviceName", paramTypes);
            Object[] argList = new Object[]{channel, name, listener};
            setDeviceName.invoke(wifiP2pManager, argList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.e(TAG, e, "Fail to set WifiDirectName.\nmanager=%s\nchannel=%s\nname=%s\nlistener=%s", wifiP2pManager, channel, name, listener);
        }
    }

}

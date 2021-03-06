package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.MyApplication;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.DelayRunner;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

public class WifiLinkManager {
    /**
     * The context for wifi link manager events.
     */
    public static final String CONTEXT_WIFI_LINK_MANAGER = "nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager";

    /**
     * Broadcast intent action indicating that the group status of the current device has changed.
     * One extra {@link #EXTRA_GROUP_INFO} ({@link String}) indicates group info, which should contain the group owner and client list.
     * <p>
     * The values are also available with function {@link #getLastGroupInfo()}.
     */
    public static final String ACTION_WIFI_GROUP_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager.groupChanged";
    public static final String EXTRA_GROUP_INFO = "groupInfo";

    /**
     * Broadcast intent action indicating that the discovery state of the device has changed.
     * One extra {@link #EXTRA_IS_DISCOVERING} ({@link Boolean}) indicates if the discovery is working.
     * <p>
     * The value is also available with function {@link #isWifiDiscovering()}.
     */
    public static final String ACTION_WIFI_DISCOVERY_STATE_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager.discoveryStateChanged";
    public static final String EXTRA_IS_DISCOVERING = "isDiscovering";

    /**
     * Broadcast intent action indicating that the discovered wifi p2p device list has changed.
     * One extra {@link #EXTRA_TIME} ({@link Date}) indicates time the change is observed.
     * Another extra {@link #EXTRA_DEVICE_LIST} ({@link WifiP2pDeviceList}) indicates the new device list.
     * <p>
     * The values are also available with functions {@link #getLastDiscoverTime()} and {@link #getLastDiscoverList()}.
     */
    public static final String ACTION_WIFI_LIST_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager.listChanged";
    public static final String EXTRA_DEVICE_LIST = "deviceList";
    public static final String EXTRA_TIME = "time";


    private static final String TAG = "WifiLinkManager";
    //    private static final int DEFAULT_DISCOVER_RETRY_DELAY_MS = 2000;
    private static final int DEFAULT_CHANGE_NAME_RETRY_DELAY_MS = 2000;
    private static final int DEFAULT_CREATE_GROUP_RETRY_DELAY_MS = 2000;
//    private static final int DEFAULT_DISCOVER_DURATION_MS = 20000;
//    private static final int DISCOVER_PEERS_CHECK_DELAY_MS = 500;

    @NonNull
    private static String getDeviceString(WifiP2pDevice device) {
        return (device == null) ?
                "null" :
                String.format("%s(%s) %s/%s",
                        device.deviceName,
                        device.deviceAddress,
                        device.isGroupOwner() ? "M" : "S",
                        getDeviceStatus(device.status));
    }

    @NonNull
    private static String getDeviceStatus(int status) {
        switch (status) {
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    private static void setWifiDirectName(@NonNull WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel, String name, WifiP2pManager.ActionListener listener) {
        try {
            Class[] paramTypes = new Class[]{WifiP2pManager.Channel.class, String.class, WifiP2pManager.ActionListener.class};
            Method setDeviceName = WifiP2pManager.class.getMethod("setDeviceName", paramTypes);
            Object[] argList = new Object[]{channel, name, listener};
            setDeviceName.invoke(wifiP2pManager, argList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.e(TAG, e, "Fail to set Name.\nmanager=%s\nchannel=%s\nname=%s\nlistener=%s", wifiP2pManager, channel, name, listener);
        }
    }

    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    private boolean discovering = false;
    private Date lastDiscoverTime = null;
    private WifiP2pDeviceList lastDiscoverList = null;
    private WifiP2pGroup lastGroupInfo = null;

    WifiLinkManager() {
        android.content.Context context = MyApplication.getDefaultInstance().getApplicationContext();
        wifiP2pManager = (WifiP2pManager) context.getSystemService(android.content.Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), () -> {
            Helper.notifyUser(LogType.Error, "Wifi Direct channel disconnected!");
            Log.e(TAG, "Channel disconnected!");
        });

        if (Constants.getWifiDirectNeighbors().length > 0) {
            android.content.IntentFilter filter = new android.content.IntentFilter();
            // filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
            filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            // Connection changed is listened in the WifiCPConnectionManagerClient
            //  filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            context.registerReceiver(new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, android.content.Intent intent) {
                    String action = intent.getAction();
                    if (action == null) return;
                    Log.d(TAG, "linkBroadcastReceiver.onReceive, action=%s", action);
                    switch (action) {
                        case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                            onPeersChanged(intent);
                            break;
                        case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
                            onDiscoverStatusChanged(intent);
                            break;
                        case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                            onThisDeviceChanged(intent);
                            break;
//                        case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
//                            onConnectionChanged(intent);
//                            break;
                    }
                }

            }, filter);
            Log.d(TAG, "Registered linkBroadcastReceiver");
            DelayRunner.getDefaultInstance().post(this::setWifiDirectName);
        } else {
            String tmpName;
            do {
                tmpName = "Android_" + Helper.getRandomString(4, 4, Helper.CANDIDATE_CHARSET_LETTERS_NUMBERS);
            } while (Device.isNameExists(tmpName));
            final String newName = tmpName;
            setWifiDirectName(wifiP2pManager, channel, newName, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Changed name to: %s. Close the channel!", newName);
                    try {
                        channel.close();
                    } catch (RuntimeException e) {
                        Log.e(TAG, e, "Failed in closing channel.");
                    }
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Failed in changing name to: %s. Close the channel!", newName);
                    try {
                        channel.close();
                    } catch (RuntimeException e) {
                        Log.e(TAG, e, "Failed in closing channel.");
                    }
                }
            });
        }
    }

    private void setWifiDirectName() {
        String name = Device.getName();

        setWifiDirectName(wifiP2pManager, channel, name, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Succeeded in changing name! My name: %s", name);
                DelayRunner.getDefaultInstance().post(WifiLinkManager.this::createGroup);
            }

            @Override
            public void onFailure(int reason) {
                Helper.notifyUser(LogType.Error, "Fail in changing Wifi Direct name! reason=%d, retry in %dms", reason, DEFAULT_CHANGE_NAME_RETRY_DELAY_MS);
                Log.e(TAG, "Failed in changing name! reason=%d, retry in %dms", reason, DEFAULT_CHANGE_NAME_RETRY_DELAY_MS);
                DelayRunner.getDefaultInstance().postDelayed(DEFAULT_CHANGE_NAME_RETRY_DELAY_MS, WifiLinkManager.this::setWifiDirectName);
            }
        });
    }

    private void createGroup() {
        if (Constants.isWifiDirectGroupOwner()) {
//            wifiP2pManager.removeGroup();
            wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Succeeded in creating group!");
                }

                @Override
                public void onFailure(int reason) {
                    if (reason != 2) {
                        Helper.notifyUser(LogType.Info, "Failed in creating Wifi Direct group! reason=%d, retry in %dms", reason, DEFAULT_CREATE_GROUP_RETRY_DELAY_MS);
                        Log.e(TAG, "Failed in creating Wifi Direct group! reason=%d, retry in %dms", reason, DEFAULT_CREATE_GROUP_RETRY_DELAY_MS);
                        DelayRunner.getDefaultInstance().postDelayed(DEFAULT_CREATE_GROUP_RETRY_DELAY_MS, WifiLinkManager.this::createGroup);
                    }
                }
            });

        }
    }

    public void discoverPeers() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Helper.notifyUser(LogType.Info, "Succeeded in initializing peer discovery!");
                Log.i(TAG, "Succeeded in initializing peer discovery!");
            }

            @Override
            public void onFailure(int reason) {
                Helper.notifyUser(LogType.Info, "Failed in discovering peers! reason=%d", reason);
                Log.e(TAG, "Failed in discovering peers! reason=%d", reason);
            }
        });
    }

    private void onThisDeviceChanged(@NonNull android.content.Intent intent) {
        WifiP2pDevice thisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        Log.i(TAG, "Device name: %s, thisDevice.isGroupOwner: %b, shouldBeGroupOwner: %b", thisDevice.deviceName, thisDevice.isGroupOwner(), Constants.isWifiDirectGroupOwner());
        if (Constants.isWifiDirectGroupOwner()) {
            wifiP2pManager.requestGroupInfo(channel, WifiLinkManager.this::onGroupInfoAvailable);
        }
    }

    private void onGroupInfoAvailable(WifiP2pGroup group) {
//        Log.d(TAG, "onGroupInfoAvailable: %s", group);
        if (group != null) {
            Log.i(TAG, "ssid=%s, pass=%s", group.getNetworkName(), group.getPassphrase());
            lastGroupInfo = group;

            Context.getContext(CONTEXT_WIFI_LINK_MANAGER).sendBroadcast(new Intent(ACTION_WIFI_GROUP_CHANGED).putExtra(EXTRA_GROUP_INFO, group));
        } else {
            Context.getContext(CONTEXT_WIFI_LINK_MANAGER).sendBroadcast(new Intent(ACTION_WIFI_GROUP_CHANGED));
        }
    }

    private void onDiscoverStatusChanged(@NonNull android.content.Intent intent) {
        discovering = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0) == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED;
        if (discovering) {
            Log.i(TAG, "Discovery started!");
        } else {
            Helper.notifyUser(LogType.Error, "Discovery stopped!");
            Log.i(TAG, "Discovery stopped!");
        }
        Context.getContext(CONTEXT_WIFI_LINK_MANAGER).sendBroadcast(new Intent(ACTION_WIFI_DISCOVERY_STATE_CHANGED).putExtra(EXTRA_IS_DISCOVERING, discovering));
    }

    private void onPeersChanged(@NonNull android.content.Intent intent) {
        WifiP2pDeviceList deviceList = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
        StringBuilder debugBuilder = new StringBuilder(), infoBuilder = new StringBuilder();
        for (WifiP2pDevice device : deviceList.getDeviceList()) {
            debugBuilder.append('\n');
            debugBuilder.append(device);
            infoBuilder.append(String.format("\nDevice=%s", getDeviceString(device)));
        }
        Log.v(TAG, "onPeerListDiscovered (%d) T:%s%s", deviceList.getDeviceList().size(), Thread.currentThread(), debugBuilder.toString());
        Log.i(TAG, "onPeerListDiscovered (%d)%s", deviceList.getDeviceList().size(), infoBuilder.toString());
        this.lastDiscoverTime = new Date();
        this.lastDiscoverList = deviceList;
        Context.getContext(CONTEXT_WIFI_LINK_MANAGER).sendBroadcast(new Intent(ACTION_WIFI_LIST_CHANGED).putExtra(EXTRA_DEVICE_LIST, deviceList).putExtra(EXTRA_TIME, lastDiscoverTime));
    }

//    private void onConnectionChanged(@NonNull android.content.Intent intent) {
//        // EXTRA_WIFI_P2P_INFO EXTRA_NETWORK_INFO EXTRA_WIFI_P2P_GROUP
//        WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
//        NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
//        WifiP2pGroup p2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
//        StringBuilder infoBuilder = new StringBuilder();
//        for (WifiP2pDevice device : p2pGroup.getClientList()) {
//            infoBuilder.append(String.format("\nClient=%s", getDeviceString(device)));
//        }
//        WifiP2pDevice owner = p2pGroup.getOwner();
//        Log.i(TAG, "connection changed! (%d)\nOwner=%s%s", p2pGroup.getClientList().size(), getDeviceString(owner), infoBuilder.toString());
//        Log.v(TAG, "connection changed!\np2pInfo=%s\nnetInfo=%s\np2pGroup=%s", p2pInfo, netInfo, p2pGroup);
//    }


    public boolean isWifiDiscovering() {
        return discovering;
    }

    public Date getLastDiscoverTime() {
        return lastDiscoverTime;
    }

    public WifiP2pDeviceList getLastDiscoverList() {
        return lastDiscoverList;
    }

    public WifiP2pGroup getLastGroupInfo() {
        return lastGroupInfo;
    }

    public void modifyConnection(WifiP2pDevice device) {
        if (device == null) {
            Helper.notifyUser(LogType.Info, "Cannot connect to a Wifi Direct device that is not discovered!");
            return;
        }
        // Since there is no way the group master could uninvite or disconnect (other than remove whole group)
        // We make sure that the connection is operated on the clients
        if (Constants.isWifiDirectGroupOwner()) {
            Helper.notifyUser(LogType.Info, "Please connect/disconnect from the Wifi Direct client side!");
            return;
        }
        switch (device.status) {
            case WifiP2pDevice.AVAILABLE: {
                // check wifi list, see if i have connected to, or invited any device
                if (lastDiscoverList != null) {
                    for (WifiP2pDevice d : lastDiscoverList.getDeviceList()) {
                        if (d.status == WifiP2pDevice.INVITED || d.status == WifiP2pDevice.CONNECTED) {
                            Helper.notifyUser(LogType.Info, "Already connected or invited to another Wifi Direct device: %s", getDeviceString(d));
                            return;
                        }
                    }
                }
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.groupOwnerIntent = Constants.MIN_GROUP_OWNER_INTENT;
                wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.v(TAG, "Started connecting to device: %s", getDeviceString(device));
                    }

                    @Override
                    public void onFailure(int reason) {
                        Helper.notifyUser(LogType.Info, "Failed in connecting to Wifi Direct device: %s, reason=%d", getDeviceString(device), reason);
                        Log.e(TAG, "Failed in connecting to device: %s, reason=%d", getDeviceString(device), reason);
                    }
                });
                break;
            }
            case WifiP2pDevice.INVITED:
                wifiP2pManager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.v(TAG, "Started cancelling connect from device: %s", getDeviceString(device));
                    }

                    @Override
                    public void onFailure(int reason) {
                        Helper.notifyUser(LogType.Info, "Failed in cancelling Wifi Direct connection from device: %s, reason=%d", getDeviceString(device), reason);
                        Log.e(TAG, "Failed in cancelling connection from device: %s, reason=%d", getDeviceString(device), reason);
                    }
                });
                break;
            case WifiP2pDevice.CONNECTED:
                wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.v(TAG, "Started disconnecting from device: %s", getDeviceString(device));
                    }

                    @Override
                    public void onFailure(int reason) {
                        Helper.notifyUser(LogType.Info, "Failed in disconnecting from device: %s, reason=%d", getDeviceString(device), reason);
                        Log.e(TAG, "Failed in disconnecting from device: %s, reason=%d", getDeviceString(device), reason);
                    }
                });
                break;
            case WifiP2pDevice.FAILED:
            case WifiP2pDevice.UNAVAILABLE:
            default:
                Helper.notifyUser(LogType.Info, "Wifi Direct device not in connectable state: " + device.status);
                break;
        }
    }

}

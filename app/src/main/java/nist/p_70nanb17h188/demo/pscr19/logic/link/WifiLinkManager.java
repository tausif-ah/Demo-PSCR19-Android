package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.logic.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class WifiLinkManager {

    public static final String EXTRA_TIME = "time";

    /**
     * Broadcast intent action indicating that the group status of the current device has changed.
     * One extra EXTRA_GROUP_INFO (java.lang.String) indicates group info, which should contain the group owner and client list..
     * <p>
     * The values are also available with function getLastGroupInfo().
     */
    public static final String ACTION_WIFI_GROUP_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager.groupChanged";
    public static final String EXTRA_GROUP_INFO = "groupInfo";

    /**
     * Broadcast intent action indicating that the discovery state of the device has changed.
     * One extra #EXTRA_IS_DISCOVERING (boolean) indicates if the discovery is working.
     * <p>
     * The value is also available with function isWifiDiscovering().
     */
    public static final String ACTION_WIFI_DISCOVERY_STATE_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager.discoveryStateChanged";
    public static final String EXTRA_IS_DISCOVERING = "isDiscovering";

    /**
     * Broadcast intent action indicating that the discovered wifi p2p device list has changed.
     * One extra EXTRA_TIME (java.util.Date) indicates time the change is observed.
     * Another extra EXTRA_DEVICE_LIST (android.net.wifi.p2p.WifiP2pDeviceList) indicates the new device list.
     * <p>
     * The values are also available with functions getLastDiscoverTime() and getLastDiscoverList().
     */
    public static final String ACTION_WIFI_LIST_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager.listChanged";
    public static final String EXTRA_DEVICE_LIST = "deviceList";


    private static final String TAG = "WifiLinkManager";
    private static final int DEFAULT_DISCOVER_RETRY_DELAY_MS = 2000;
    private static final int DEFAULT_CREATE_GROUP_RETRY_DELAY_MS = 2000;
    private static final int DEFAULT_DISCOVER_DURATION_MS = 20000;
    private static WifiLinkManager DEFAULT_INSTANCE;

    private final Application application;
    private final WifiP2pManager wifiP2pManager;
    private final WifiP2pManager.Channel channel;
    private final Handler handler;
    private boolean discovering = false;
    private Date lastDiscoverTime = null;
    private WifiP2pDeviceList lastDiscoverList = null;
    private WifiP2pGroup lastGroupInfo = null;
    private long lastDiscoverInitiateTime = 0;
    private boolean lastDiscoverSucceed = false;

    private WifiLinkManager(@NonNull Application application) {
        this.application = application;
        Context context = application.getApplicationContext();
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), () -> {
            Toast.makeText(context, "Wifi Direct channel disconnected!", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Channel disconnected!");
        });

        if (Constants.getWifiDirectNeighbors().length > 0) {
            handler = new Handler(application.getMainLooper());
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
                            onDiscoverStatusChanged(intent);
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
            setWifiDirectName();
            createGroup();
            scheduleDiscoverPeers();
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
                    channel.close();
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(TAG, "Failed in changing name to: %s. Close the channel!", newName);
                    channel.close();
                }
            });
            handler = null;
        }
    }

    static void init(@NonNull Application application) {
        if (DEFAULT_INSTANCE == null) {
            DEFAULT_INSTANCE = new WifiLinkManager(application);
        }
    }

    public static WifiLinkManager getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static String getDeviceString(WifiP2pDevice device) {
        if (device == null) return "null";
        return String.format("%s(%s) %s/%s", device.deviceName, device.deviceAddress, device.isGroupOwner() ? "M" : "S", getDeviceStatus(device.status));
    }

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

    private void setWifiDirectName() {
        String name = Device.getName();

        setWifiDirectName(wifiP2pManager, channel, name, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Succeeded in changing name! My name: %s", name);
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(application.getApplicationContext(), "Fail in changing Wifi Direct name! reason=" + reason, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed in changing name! reason=%d", reason);
            }
        });
    }

    private void createGroup() {
        if (Constants.isWifiDirectGroupOwner()) {
            wifiP2pManager.createGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "Succeeded in creating group!");
                }

                @Override
                public void onFailure(int reason) {
                    String msg = String.format(Locale.US, "Failed in creating Wifi Direct group! reason=%d, retry in %dms", reason, DEFAULT_CREATE_GROUP_RETRY_DELAY_MS);
                    Toast.makeText(application.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, msg);
                    handler.postDelayed(WifiLinkManager.this::createGroup, DEFAULT_CREATE_GROUP_RETRY_DELAY_MS);
                }
            });

        }
    }

    private void scheduleDiscoverPeers() {
        long now = System.currentTimeMillis();
        long nextDiscoverTime = lastDiscoverInitiateTime + (lastDiscoverSucceed ? DEFAULT_DISCOVER_DURATION_MS : DEFAULT_DISCOVER_RETRY_DELAY_MS);
        if (nextDiscoverTime <= now) {
            discoverPeers();
        }
        handler.postDelayed(this::scheduleDiscoverPeers, 500);
    }

    private void discoverPeers() {
        lastDiscoverInitiateTime = System.currentTimeMillis();
//        Log.v(TAG, "Start discovering peers!");
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Initiated discovering peers! Schedule next discover in %dms", DEFAULT_DISCOVER_DURATION_MS);
                lastDiscoverSucceed = true;
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(application.getApplicationContext(), "Failed in discovering Wifi Direct peers! reason=" + reason + ", retry in " + DEFAULT_DISCOVER_RETRY_DELAY_MS + "ms", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed in discovering peers! reason=%d, retry in %dms", reason, DEFAULT_DISCOVER_RETRY_DELAY_MS);
                lastDiscoverSucceed = false;
            }
        });
    }

    private void onThisDeviceChanged(@NonNull Intent intent) {
        WifiP2pDevice thisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
        Log.i(TAG, "Device name: %s, isGroupOwner: %b", thisDevice.deviceName, thisDevice.isGroupOwner());
        if (Constants.isWifiDirectGroupOwner()) {
            wifiP2pManager.requestGroupInfo(channel, WifiLinkManager.this::onGroupInfoAvailable);
        }
    }

    private void onGroupInfoAvailable(WifiP2pGroup group) {
//        Log.d(TAG, "onGroupInfoAvailable: %s", group);
        if (group != null) {
            Log.i(TAG, "ssid=%s, pass=%s", group.getNetworkName(), group.getPassphrase());
            lastGroupInfo = group;
            application.getApplicationContext().sendBroadcast(new Intent(ACTION_WIFI_GROUP_CHANGED).putExtra(EXTRA_GROUP_INFO, group));
        }
    }

    private void onDiscoverStatusChanged(@NonNull Intent intent) {
        int discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
        switch (discoveryState) {
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                Log.i(TAG, "Discovery started!");
                discovering = true;
                application.getApplicationContext().sendBroadcast(new Intent(ACTION_WIFI_DISCOVERY_STATE_CHANGED).putExtra(EXTRA_IS_DISCOVERING, discovering));
                break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                Log.i(TAG, "Discovery stopped!");
                lastDiscoverSucceed = false;
                discovering = false;
                application.getApplicationContext().sendBroadcast(new Intent(ACTION_WIFI_DISCOVERY_STATE_CHANGED).putExtra(EXTRA_IS_DISCOVERING, discovering));
                break;
            default:
                Log.e(TAG, "Unknown discovery state!");
                break;
        }
    }

    private void onPeersChanged(@NonNull Intent intent) {
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
        application.getApplicationContext().sendBroadcast(new Intent(ACTION_WIFI_LIST_CHANGED).putExtra(EXTRA_DEVICE_LIST, deviceList).putExtra(EXTRA_TIME, lastDiscoverTime));
    }

    private void onConnectionChanged(@NonNull Intent intent) {
        // EXTRA_WIFI_P2P_INFO EXTRA_NETWORK_INFO EXTRA_WIFI_P2P_GROUP
        WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
        WifiP2pGroup p2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
        StringBuilder infoBuilder = new StringBuilder();
        for (WifiP2pDevice device : p2pGroup.getClientList()) {
            infoBuilder.append(String.format("\nClient=%s", getDeviceString(device)));
        }
        WifiP2pDevice owner = p2pGroup.getOwner();
        Log.i(TAG, "connection changed! (%d)\nOwner=%s%s", p2pGroup.getClientList().size(), getDeviceString(owner), infoBuilder.toString());
        Log.v(TAG, "connection changed!\np2pInfo=%s\nnetInfo=%s\np2pGroup=%s", p2pInfo, netInfo, p2pGroup);
        onGroupInfoAvailable(p2pGroup);
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

    public WifiP2pGroup getLastGroupInfo() {
        return lastGroupInfo;
    }

    public void modifyConnection(WifiP2pDevice device) {
        if (device == null) {
            Toast.makeText(application.getApplicationContext(), "Cannot connect to a Wifi Direct device that is not discovered!", Toast.LENGTH_SHORT).show();
            return;
        }
        // Since there is no way the group master could uninvite or disconnect (other than remove whole group)
        // We make sure that the connection is operated on the clients
        if (Constants.isWifiDirectGroupOwner()) {
            Toast.makeText(application.getApplicationContext(), "Please connect/disconnect from the Wifi Direct client side!", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (device.status) {
            case WifiP2pDevice.AVAILABLE: {
                // check wifi list, see if i have connected to, or invited any device
                if (lastDiscoverList != null) {
                    for (WifiP2pDevice d : lastDiscoverList.getDeviceList()) {
                        if (d.status == WifiP2pDevice.INVITED || d.status == WifiP2pDevice.CONNECTED) {
                            Toast.makeText(application.getApplicationContext(), String.format(Locale.US, "Already connected or invited to another Wifi Direct device: %s", getDeviceString(d)), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(application.getApplicationContext(), String.format(Locale.US, "Failed in connecting to Wifi Direct device: %s, reason=%d", getDeviceString(device), reason), Toast.LENGTH_LONG).show();
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
                        Toast.makeText(application.getApplicationContext(), String.format(Locale.US, "Failed in cancelling Wifi Direct connection from device: %s, reason=%d", getDeviceString(device), reason), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed in canclling connection from device: %s, reason=%d", getDeviceString(device), reason);
                    }
                });
                break;
            case WifiP2pDevice.CONNECTED:
                // TODO:
                //   if there is tcp connection, disconnect tcp connection
                wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.v(TAG, "Started disconnecting from device: %s", getDeviceString(device));
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(application.getApplicationContext(), String.format(Locale.US, "Failed in disconnecting from device: %s, reason=%d", getDeviceString(device), reason), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed in disconnecting from device: %s, reason=%d", getDeviceString(device), reason);
                    }
                });
                lastDiscoverSucceed = false;
                break;
            case WifiP2pDevice.FAILED:
            case WifiP2pDevice.UNAVAILABLE:
            default:
                Toast.makeText(application.getApplicationContext(), "Wifi Direct device not in connectable state: " + device.status, Toast.LENGTH_SHORT).show();
                break;
        }
    }

}

package nist.p_70nanb17h188.demo.pscr19.link;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nist.p_70nanb17h188.demo.pscr19.Constants;
import nist.p_70nanb17h188.demo.pscr19.Device;

public class LinkDiscoveryController {
    private Context context;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private IntentFilter intentFilter;
    private LinkBroadcastReceiver linkBroadcastReceiver;

    LinkDiscoveryController() {
        this.context = Constants.mainContext;
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
        setWiFiDirectName();
    }

    private void setWiFiDirectName() {
        try {
            Class[] paramTypes = new Class[3];
            paramTypes[0] = WifiP2pManager.Channel.class;
            paramTypes[1] = String.class;
            paramTypes[2] = WifiP2pManager.ActionListener.class;
            Method setDeviceName = wifiP2pManager.getClass().getMethod("setDeviceName", paramTypes);
            Object[] argList = new Object[3];
            argList[0] = channel;
            argList[1] = Device.getName();
            argList[2] = new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onFailure(int i) {

                }
            };
            setDeviceName.invoke(wifiP2pManager, argList);
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private void configureBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.setName(Device.getName());
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
    }
}

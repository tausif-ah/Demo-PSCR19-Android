//package nist.p_70nanb17h188.demo.pscr19.link;
//
//import android.bluetooth.BluetoothDevice;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.net.wifi.p2p.WifiP2pDeviceList;
//import android.net.wifi.p2p.WifiP2pManager;
//
//public class LinkBroadcastReceiver extends BroadcastReceiver {
//
//    private WifiP2pManager wifiP2pManager;
//    private WifiP2pManager.Channel channel;
//    private LinkDiscoveryController linkDiscoveryController;
//
//    public void setWifiP2pManager(WifiP2pManager wifiP2pManager) {
//        this.wifiP2pManager = wifiP2pManager;
//    }
//
//    public void setChannel(WifiP2pManager.Channel channel) {
//        this.channel = channel;
//    }
//
//    public void setLinkDiscoveryController(LinkDiscoveryController linkDiscoveryController) {
//        this.linkDiscoveryController = linkDiscoveryController;
//    }
//
//    @Override
//    public void onReceive(Context context, Intent intent) {
//        String action = intent.getAction();
//        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
//            WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
//                @Override
//                public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
//                    linkDiscoveryController.WiFiDirectLinkFound(wifiP2pDeviceList);
//                }
//            };
//            if (wifiP2pManager != null)
//                wifiP2pManager.requestPeers(channel, peerListListener);
//        }
//        else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            linkDiscoveryController.BluetoothLinkFound(device);
//        }
//    }
//}

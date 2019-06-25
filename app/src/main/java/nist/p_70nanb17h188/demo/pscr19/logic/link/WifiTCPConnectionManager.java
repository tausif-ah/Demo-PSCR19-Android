package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.MyApplication;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.DelayRunner;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public abstract class WifiTCPConnectionManager {
    static final long SERVER_SOCKET_RETRY_DURATION_MS = 2000;
    static final long SOCKET_RCONNECT_DURATION_MS = 2000;

    static WifiTCPConnectionManager createWifiTCPConnectionManager(@NonNull TCPConnectionManager tcpConnectionManager) {
        if (Constants.getWifiDirectNeighbors().length == 0) {
            return new WifiTCPConnectionManagerDoNothing(tcpConnectionManager);
        } else if (Constants.isWifiDirectGroupOwner()) {
            return new WifiTCPConnectionManagerGroupOwner(tcpConnectionManager);
        } else {
            return new WifiTCPConnectionManagerClient(tcpConnectionManager);
        }
    }

    @NonNull
    private final TCPConnectionManager tcpConnectionManager;

    WifiTCPConnectionManager(@NonNull TCPConnectionManager tcpConnectionManager) {
        this.tcpConnectionManager = tcpConnectionManager;
    }

    static void checkValidSendDataParams(@NonNull byte[] data, int start, int len) {
        if (len < 0 || start + len > data.length)
            throw new IllegalArgumentException(String.format(Locale.US, "wrong start(%d) or len(%d) value, data.length=%d", start, len, data.length));
    }

    @NonNull
    TCPConnectionManager getTcpConnectionManager() {
        return tcpConnectionManager;
    }

    public abstract boolean isDeviceTCPConnected(@NonNull String name);

    public abstract void modifyConnection(@NonNull String name, boolean establish);

    public abstract boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len);

}

class WifiTCPConnectionManagerDoNothing extends WifiTCPConnectionManager {
    WifiTCPConnectionManagerDoNothing(TCPConnectionManager tcpConnectionManager) {
        super(tcpConnectionManager);
    }

    @Override
    public void modifyConnection(@NonNull String name, boolean establish) {

    }

    @Override
    public boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        checkValidSendDataParams(data, start, len);
        return false;
    }

    @Override
    public boolean isDeviceTCPConnected(@NonNull String name) {
        return false;
    }
}

class WifiTCPConnectionManagerGroupOwner extends WifiTCPConnectionManager implements TCPConnectionManager.ServerSocketChannelEventHandler, TCPConnectionManager.SocketChannelEventHandler {
    private static final String TAG = "WifiTCPConnectionManagerGroupOwner";
    //    @NonNull
//    private final String[] wifiDirectNeighbors;
    private final HashMap<String, SocketChannel> connectedNeighbors = new HashMap<>();
    private final HashMap<SocketChannel, String> connectedNeighborsReverse = new HashMap<>();


    WifiTCPConnectionManagerGroupOwner(TCPConnectionManager tcpConnectionManager) {
        super(tcpConnectionManager);
//        wifiDirectNeighbors = Constants.getWifiDirectNeighbors();
        DelayRunner.getDefaultInstance().post(this::startServerSocket);
    }

    @Override
    public boolean isDeviceTCPConnected(@NonNull String name) {
        return connectedNeighbors.containsKey(name);
    }

    private void startServerSocket() {
        ServerSocketChannel serverSocketChannel = getTcpConnectionManager().addServerSocketChannel(new InetSocketAddress(Constants.WIFI_DIRECT_SERVER_LISTEN_PORT), this);
        if (serverSocketChannel == null) {
            Log.i(TAG, "Failed in listening to port %d, retry in %dms.", Constants.WIFI_DIRECT_SERVER_LISTEN_PORT, SERVER_SOCKET_RETRY_DURATION_MS);
            DelayRunner.getDefaultInstance().postDelayed(SERVER_SOCKET_RETRY_DURATION_MS, this::startServerSocket);
        }
    }

    @Override
    public void modifyConnection(@NonNull String name, boolean establish) {
        // Do nothing. The Link manager will till the user that we shouldn't do it on the group owner side.
    }

    @Override
    public boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        checkValidSendDataParams(data, start, len);
        SocketChannel socketChannel = connectedNeighbors.get(id.getName());
        if (socketChannel == null) return false;
        byte[] buf = new byte[len];
        if (len > 0) System.arraycopy(data, start, buf, 0, len);
        return getTcpConnectionManager().writeToSocket(socketChannel, buf);
    }

//    @Override
//    public void onServerSocketChannelClosed(@NonNull ServerSocketChannel serverSocketChannel) {
//        Log.i(TAG, "ServerSocketChannel closed: %s, retry in %dms!", serverSocketChannel, SERVER_SOCKET_RETRY_DURATION_MS);
//        DelayRunner.getDefaultInstance().postDelayed(SERVER_SOCKET_RETRY_DURATION_MS, this::startServerSocket);
//    }
//
//    @Override
//    public void onServerSocketChannelCloseFailed(@NonNull ServerSocketChannel serverSocketChannel) {
//        Log.e(TAG, "Should not reach here (onServerSocketChannelCloseFailed). I'll never close server socket!");
//    }

    @Override
    public void onServerSocketChannelAcceptFailed(@NonNull ServerSocketChannel serverSocketChannel) {
        Log.e(TAG, "Failed in accepting SocketChannel, serverSocketChannel=%s", serverSocketChannel);
    }

    @NonNull
    @Override
    public TCPConnectionManager.SocketChannelEventHandler getSocketChannelEventHandler() {
        return this;
    }

    @Override
    public void onSocketConnected(@NonNull SocketChannel socketChannel) {
        Log.i(TAG, "Connected to a socket, socketChannel=%s", socketChannel);
        // do nothing until we can get the name of the device.
    }

    @Override
    public void onSocketConnectFailed(@NonNull SocketChannel socketChannel) {
        Log.d(TAG, "Should not reach here (onSocketConnectFailed), group owner will never try to connect to a socket socketChannel=%s", socketChannel);
    }

    @Override
    public void onSocketChannelNameReceived(@NonNull SocketChannel socketChannel, @NonNull String name) {
        // If it is my neighbor, connect
//        int i;
//        for (i = 0; i < wifiDirectNeighbors.length; i++) {
//            if (name.equals(wifiDirectNeighbors[i])) {
//                break;
//            }
//        }
//        if (i == wifiDirectNeighbors.length) {
//            Log.i(TAG, "Connected device (%s) is not my neighbor, colse connection!", name);
//            TCPConnectionManager.getDefaultInstance().closeSocketChannel(socketChannel);
//            return;
//        }
        SocketChannel originalSocketChannel;
        synchronized (connectedNeighbors) {
            originalSocketChannel = connectedNeighbors.put(name, socketChannel);
            Log.i(TAG, "Received name=%s, originalSocketChannel=%s", name, originalSocketChannel);
            if (originalSocketChannel != null) {
                connectedNeighborsReverse.remove(originalSocketChannel);
            }
            connectedNeighborsReverse.put(socketChannel, name);
        }
        if (originalSocketChannel != null) {
            getTcpConnectionManager().closeSocketChannel(originalSocketChannel);
        } else {
            Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                    new Intent(LinkLayer.ACTION_LINK_CHANGED).
                            putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                            putExtra(LinkLayer.EXTRA_CONNECTED, true));
        }
        // if not null, it is already in the connected state
    }

    @Override
    public void onSocketChannelDataReceived(@NonNull SocketChannel socketChannel, @NonNull byte[] data) {
        String name = connectedNeighborsReverse.get(socketChannel);
        // from a neighbor that does not exist? how can that be?
        if (name == null) return;
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                new Intent(LinkLayer.ACTION_DATA_RECEIVED).
                        putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                        putExtra(LinkLayer.EXTRA_DATA, data));
    }

    @Override
    public void onSocketChannelClosed(@NonNull SocketChannel socketChannel) {
        String name;
        synchronized (connectedNeighbors) {
            name = connectedNeighborsReverse.remove(socketChannel);
            if (name != null) connectedNeighbors.remove(name);
        }
        if (name != null) {
            Log.i(TAG, "Closed a remote socket socketChannel=%s, original remote device: %s", socketChannel, name);
            Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                    new Intent(LinkLayer.ACTION_LINK_CHANGED).
                            putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                            putExtra(LinkLayer.EXTRA_CONNECTED, false));
        } else {
            Log.i(TAG, "Closed a remote socket socketChannel=%s", socketChannel);
        }
        // I'm a group owner, wait for the client to reconnect.
    }

    @Override
    public void onSocketChannelCloseFailed(@NonNull SocketChannel socketChannel) {
        Log.e(TAG, "Failed in closing a remote socket, socketChannel=%s", socketChannel);
    }
}

class WifiTCPConnectionManagerClient extends WifiTCPConnectionManager implements TCPConnectionManager.SocketChannelEventHandler {
    private static final String TAG = "WifiTCPConnectionManagerClient";
    @Nullable
    private SocketChannel currentSocket;
    private boolean reconnect = false;
    private String connectedName;
    private InetSocketAddress address;
//    @NonNull
//    private final String[] wifiDirectNeighbors;

    WifiTCPConnectionManagerClient(@NonNull TCPConnectionManager tcpConnectionManager) {
        super(tcpConnectionManager);
        // wifiDirectNeighbors = Constants.getWifiDirectNeighbors();

        // need to wait for group formation, and then connect to group owner
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        MyApplication.getDefaultInstance().getApplicationContext().registerReceiver(new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                if (intent == null || !WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(intent.getAction()))
                    return;
                WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
//                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                Log.v(TAG, "Connection Changed: %nwifiP2pInfo=%s", wifiP2pInfo);
                // has to be connection changed action
                reconnect = wifiP2pInfo.groupFormed;
                if (reconnect) {
                    address = new InetSocketAddress(wifiP2pInfo.groupOwnerAddress, Constants.WIFI_DIRECT_SERVER_LISTEN_PORT);
                    DelayRunner.getDefaultInstance().post(WifiTCPConnectionManagerClient.this::establishConnection);
                } else {
                    closeConnection();
                }
            }
        }, filter);
    }

    private void closeConnection() {
        synchronized (this) {
            if (currentSocket != null) {
                Log.v(TAG, "closeConnection, currentSocket=%s", currentSocket);
                getTcpConnectionManager().closeSocketChannel(currentSocket);
                // do not set the current socket, let the close handler set it to null
            } else {
                Log.v(TAG, "closeConnection, currentSocket == null");
            }
        }
    }

    private void establishConnection() {
        synchronized (this) {
            // If there is already a connection, do nothing. If it is a staled one, we can wait for it to timeout.
            // If we somehow decided to disconnect (reconnect == false) we also don't connect.
            Log.v(TAG, "establishConnection, currentSocket=%s, address=%s, reconnect = %b", currentSocket, address, reconnect);

            if (currentSocket != null || !reconnect) return;
            SocketChannel channel = getTcpConnectionManager().addSocketChannel(address, this);
            if (channel == null) {
                Log.i(TAG, "Failed in creating socketChannel, retry in %dms", SOCKET_RCONNECT_DURATION_MS);
                DelayRunner.getDefaultInstance().postDelayed(SOCKET_RCONNECT_DURATION_MS, this::establishConnection);
            } else {
                Log.i(TAG, "Succeeded in creating socketChannel: %s", channel);
            }
            currentSocket = channel;
        }
    }

    @Override
    public boolean isDeviceTCPConnected(@NonNull String name) {
        return name.equals(connectedName);
    }

    @Override
    public void modifyConnection(@NonNull String name, boolean establish) {
        reconnect = establish;
        if (establish) {
            // need to wait for the physical connection to establish, do nothing.
            Log.v(TAG, "Try to establish connection, name=%s", name);
        } else {
            Log.v(TAG, "Try to close connection, name=%s", name);
            closeConnection();
        }
    }

    @Override
    public boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        checkValidSendDataParams(data, start, len);
        synchronized (this) {
            if (!id.getName().equals(connectedName) || currentSocket == null) return false;
            byte[] buf = new byte[len];
            if (len > 0) System.arraycopy(data, start, buf, 0, len);
            return getTcpConnectionManager().writeToSocket(currentSocket, buf);
        }
    }

    @Override
    public void onSocketConnected(@NonNull SocketChannel socketChannel) {
        // do nothing, wait for the other side to give me the name.
        synchronized (this) {
            if (socketChannel != currentSocket) return;
        }
        Log.v(TAG, "Socket connected: %s", socketChannel);
    }

    @Override
    public void onSocketConnectFailed(@NonNull SocketChannel socketChannel) {
        synchronized (this) {
            if (socketChannel != currentSocket) return;
            currentSocket = null;
        }
        Log.v(TAG, "onSocketConnectFailed: %s, reconnect=%b", socketChannel, reconnect);
        if (reconnect) {
            Log.i(TAG, "Connection failed on socketChannel %s, reconnect in %dms.", socketChannel, SOCKET_RCONNECT_DURATION_MS);
            DelayRunner.getDefaultInstance().postDelayed(SOCKET_RCONNECT_DURATION_MS, this::establishConnection);
        }
    }

    @Override
    public void onSocketChannelNameReceived(@NonNull SocketChannel socketChannel, @NonNull String name) {
//        // if that is not my neighbor, disconnect.
//        int i;
//        for (i = 0; i < wifiDirectNeighbors.length; i++) {
//            if (name.equals(wifiDirectNeighbors[i])) {
//                break;
//            }
//        }
//        if (i == wifiDirectNeighbors.length) {
//            Log.i(TAG, "Connected device (%s) is not my neighbor, colse connection!", name);
//            TCPConnectionManager.getDefaultInstance().closeSocketChannel(socketChannel);
//            return;
//        }
        synchronized (this) {
            if (socketChannel != currentSocket) return;
            connectedName = name;
        }
        Log.i(TAG, "Connected to %s, %s", name, socketChannel);
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                new Intent(LinkLayer.ACTION_LINK_CHANGED).
                        putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                        putExtra(LinkLayer.EXTRA_CONNECTED, true));

    }

    @Override
    public void onSocketChannelClosed(@NonNull SocketChannel socketChannel) {
        // If I'm a client and I know who I am, and the user still wants to connect
        String name = connectedName;
        Log.v(TAG, "onSocketChannelClosed: %s, currentSocket: %s name=%s, reconnect=%b", socketChannel, currentSocket, name, reconnect);
        synchronized (this) {
            // not closing the current socket, ignore
            if (socketChannel != currentSocket) return;
            currentSocket = null;
            connectedName = null;
        }
        if (name != null) {
            Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                    new Intent(LinkLayer.ACTION_LINK_CHANGED).
                            putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                            putExtra(LinkLayer.EXTRA_CONNECTED, false));
        }
        if (reconnect) {
            Log.i(TAG, "Closed socketChannel=%s, reconnect in %dms", socketChannel, SOCKET_RCONNECT_DURATION_MS);
            DelayRunner.getDefaultInstance().postDelayed(SOCKET_RCONNECT_DURATION_MS, this::establishConnection);
        } else {
            Log.i(TAG, "Closed socketChannel=%s", socketChannel);
        }

    }

    @Override
    public void onSocketChannelCloseFailed(@NonNull SocketChannel socketChannel) {
        Log.e(TAG, "Failed in closing a remote socket, socketChannel=%s", socketChannel);
    }


    @Override
    public void onSocketChannelDataReceived(@NonNull SocketChannel socketChannel, @NonNull byte[] data) {
        // from a neighbor that does not exist? how can that be?
        if (connectedName == null) return;
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                new Intent(LinkLayer.ACTION_DATA_RECEIVED)
                        .putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(connectedName))
                        .putExtra(LinkLayer.EXTRA_DATA, data));

    }

}
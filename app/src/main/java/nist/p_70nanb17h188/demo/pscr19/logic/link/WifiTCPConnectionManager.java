package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public abstract class WifiTCPConnectionManager {
    static final long SERVER_SOCKET_RETRY_DURATION_MS = 2000;
    static final long SOCKET_RCONNECT_DURATION_MS = 2000;
    private static WifiTCPConnectionManager DEFAULT_INSTANCE = null;
    @NonNull
    final Application application;
    @NonNull
    final Context context;
    @NonNull
    final Handler handler;

    WifiTCPConnectionManager(@NonNull Application application) {
        this.application = application;
        context = application.getApplicationContext();
        handler = new Handler(application.getMainLooper());
    }

    static void init(@NonNull Application application) {
        if (DEFAULT_INSTANCE == null) {
            if (Constants.getWifiDirectNeighbors().length == 0) {
                DEFAULT_INSTANCE = new WifiTCPConnectionManagerDoNothing(application);
            } else if (Constants.isWifiDirectGroupOwner()) {
                DEFAULT_INSTANCE = new WifiTCPConnectionManagerGroupOwner(application);
            } else {
                DEFAULT_INSTANCE = new WifiTCPConnectionManagerClient(application);
            }
        }
    }

    @Nullable
    public static WifiTCPConnectionManager getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public abstract boolean isDeviceTCPConnected(@NonNull String name);

    public abstract void modifyConnection(@NonNull String name);

}

class WifiTCPConnectionManagerDoNothing extends WifiTCPConnectionManager {
    WifiTCPConnectionManagerDoNothing(@NonNull Application application) {
        super(application);
    }

    @Override
    public void modifyConnection(@NonNull String name) {

    }

    @Override
    public boolean isDeviceTCPConnected(@NonNull String name) {
        return false;
    }
}

class WifiTCPConnectionManagerGroupOwner extends WifiTCPConnectionManager implements TCPConnectionManager.ServerSocketChannelEventHandler, TCPConnectionManager.SocketChannelEventHandler {
    private static final String TAG = "WifiTCPConnectionManagerGroupOwner";
    @NonNull
    private final String[] wifiDirectNeighbors;
    private final HashMap<String, SocketChannel> connectedNeighbors = new HashMap<>();
    private final HashMap<SocketChannel, String> connectedNeighborsReverse = new HashMap<>();


    WifiTCPConnectionManagerGroupOwner(@NonNull Application application) {
        super(application);
        wifiDirectNeighbors = Constants.getWifiDirectNeighbors();
        startServerSocket();
    }

    @Override
    public boolean isDeviceTCPConnected(@NonNull String name) {
        return connectedNeighbors.containsKey(name);
    }

    private void startServerSocket() {
        ServerSocketChannel serverSocketChannel = TCPConnectionManager.getDefaultInstance().addServerSocketChannel(new InetSocketAddress(Constants.WIFI_DIRECT_SERVER_LISTEN_PORT), this);
        if (serverSocketChannel == null) {
            Log.i(TAG, "Failed in listening to port %d, retry in %dms.", Constants.WIFI_DIRECT_SERVER_LISTEN_PORT, SERVER_SOCKET_RETRY_DURATION_MS);
            handler.postDelayed(this::startServerSocket, SERVER_SOCKET_RETRY_DURATION_MS);
        }
    }

    @Override
    public void modifyConnection(@NonNull String name) {
        // Do nothing. The Link manager will till the user that we shouldn't do it on the group owner side.
    }

    @Override
    public void onServerSocketChannelClosed(@NonNull ServerSocketChannel serverSocketChannel) {
        Log.i(TAG, "ServerSocketChannel closed: %s, retry in %dms!", serverSocketChannel, SERVER_SOCKET_RETRY_DURATION_MS);
        handler.postDelayed(this::startServerSocket, SERVER_SOCKET_RETRY_DURATION_MS);
    }

    @Override
    public void onServerSocketChannelCloseFailed(@NonNull ServerSocketChannel serverSocketChannel) {
        Log.e(TAG, "Should not reach here (onServerSocketChannelCloseFailed). I'll never close server socket!");
    }

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
            TCPConnectionManager.getDefaultInstance().closeSocketChannel(originalSocketChannel);
        } else {
            context.sendBroadcast(new Intent(LinkLayer.ACTION_LINK_CHANGED).
                    putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                    putExtra(LinkLayer.EXTRA_CONNECTED, true));
        }
        // if not null, it is already in the connected state
    }

    @Override
    public void onSocketChannelDataReceived(@NonNull SocketChannel socketChannel, @NonNull byte[] data) {

    }

    @Override
    public void onSocketChannelClosed(@NonNull SocketChannel socketChannel) {
        String name;
        synchronized (connectedNeighbors) {
            name = connectedNeighborsReverse.remove(socketChannel);
            if (name != null)
                connectedNeighbors.remove(name);
        }
        if (name != null) {
            Log.i(TAG, "Closed a remote socket socketChannel=%s, original remote device: %s", socketChannel, name);
            context.sendBroadcast(new Intent(LinkLayer.ACTION_LINK_CHANGED).
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

    WifiTCPConnectionManagerClient(@NonNull Application application) {
        super(application);
        // need to wait for group formation, and then connect to group owner
    }

    @Override
    public boolean isDeviceTCPConnected(@NonNull String name) {
        return false;
    }

    @Override
    public void modifyConnection(@NonNull String name) {

    }

    @Override
    public void onSocketConnected(@NonNull SocketChannel socketChannel) {

    }

    @Override
    public void onSocketConnectFailed(@NonNull SocketChannel socketChannel) {

    }

    @Override
    public void onSocketChannelNameReceived(@NonNull SocketChannel socketChannel, @NonNull String name) {

    }

    @Override
    public void onSocketChannelDataReceived(@NonNull SocketChannel socketChannel, @NonNull byte[] data) {

    }

    @Override
    public void onSocketChannelClosed(@NonNull SocketChannel socketChannel) {
        // If I'm a client and I know who I am, and the user still wants to connect

    }

    @Override
    public void onSocketChannelCloseFailed(@NonNull SocketChannel socketChannel) {

    }
}
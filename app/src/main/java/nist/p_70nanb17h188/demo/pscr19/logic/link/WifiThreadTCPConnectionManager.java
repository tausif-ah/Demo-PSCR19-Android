package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.MyApplication;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.DelayRunner;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public abstract class WifiThreadTCPConnectionManager {
    static final long SERVER_SOCKET_RETRY_DURATION_MS = 2000;
    static final long SOCKET_RCONNECT_DURATION_MS = 2000;

    static WifiThreadTCPConnectionManager createWifiTCPConnectionManager(@NonNull ThreadTCPConnectionManager threadTCPConnectionManager) {
        if (Constants.getWifiDirectNeighbors().length == 0) {
            return new WifiThreadTCPConnectionManagerDoNothing(threadTCPConnectionManager);
        } else if (Constants.isWifiDirectGroupOwner()) {
            return new WifiThreadTCPConnectionManagerGroupOwner(threadTCPConnectionManager);
        } else {
            return new WifiThreadTCPConnectionManagerClient(threadTCPConnectionManager);
        }
    }

    @NonNull
    private final ThreadTCPConnectionManager threadTCPConnectionManager;

    WifiThreadTCPConnectionManager(@NonNull ThreadTCPConnectionManager threadTCPConnectionManager) {
        this.threadTCPConnectionManager = threadTCPConnectionManager;
    }

    static void checkValidSendDataParams(@NonNull byte[] data, int start, int len) {
        if (len < 0 || start + len > data.length)
            throw new IllegalArgumentException(String.format(Locale.US, "wrong start(%d) or len(%d) value, data.length=%d", start, len, data.length));
    }

    @NonNull
    ThreadTCPConnectionManager getThreadTCPConnectionManager() {
        return threadTCPConnectionManager;
    }

    public abstract boolean isDeviceTCPConnected(@NonNull String name);

    public abstract void modifyConnection(@NonNull String name, boolean establish);

    public abstract boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len);
}

class WifiThreadTCPConnectionManagerDoNothing extends WifiThreadTCPConnectionManager {
    WifiThreadTCPConnectionManagerDoNothing(ThreadTCPConnectionManager threadTCPConnectionManager) {
        super(threadTCPConnectionManager);
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

class WifiThreadTCPConnectionManagerGroupOwner extends WifiThreadTCPConnectionManager implements ThreadTCPConnectionManager.ServerSocketWrapperEventHandler, ThreadTCPConnectionManager.SocketWrapperEventHandler {
    private static final String TAG = "WifiThreadTCPConnectionManagerGroupOwner";
    //    @NonNull
//    private final String[] wifiDirectNeighbors;
    private final HashMap<String, SocketWrapper> connectedNeighbors = new HashMap<>();
    private final HashMap<SocketWrapper, String> connectedNeighborsReverse = new HashMap<>();


    WifiThreadTCPConnectionManagerGroupOwner(ThreadTCPConnectionManager threadTCPConnectionManager) {
        super(threadTCPConnectionManager);
//        wifiDirectNeighbors = Constants.getWifiDirectNeighbors();
        DelayRunner.getDefaultInstance().post(this::startServerSocket);
    }

    @Override
    public boolean isDeviceTCPConnected(@NonNull String name) {
        return connectedNeighbors.containsKey(name);
    }

    private void startServerSocket() {
        ServerSocketWrapper serverSocketWrapper = getThreadTCPConnectionManager().addServerSocketWrapperTCP(Constants.WIFI_DIRECT_SERVER_LISTEN_PORT, this);
        if (serverSocketWrapper == null) {
            Log.i(TAG, "Failed in listening to port %d, retry in %dms.", Constants.WIFI_DIRECT_SERVER_LISTEN_PORT, SERVER_SOCKET_RETRY_DURATION_MS);
            DelayRunner.getDefaultInstance().postDelayed(SERVER_SOCKET_RETRY_DURATION_MS, this::startServerSocket);
        } else {
            serverSocketWrapper.start();
        }
    }

    @Override
    public void modifyConnection(@NonNull String name, boolean establish) {
        // Do nothing. The Link manager will till the user that we shouldn't do it on the group owner side.
    }

    @Override
    public boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        checkValidSendDataParams(data, start, len);
        SocketWrapper socketWrapper = connectedNeighbors.get(id.getName());
        if (socketWrapper == null) return false;
        byte[] buf = new byte[len];
        if (len > 0) System.arraycopy(data, start, buf, 0, len);
        return getThreadTCPConnectionManager().writeToSocket(socketWrapper, buf);
    }

    @Override
    public void onServerSocketWrapperAcceptFailed(@NonNull ServerSocketWrapper serverSocketWrapper) {
        Log.e(TAG, "Failed in accepting SocketWrapper, serverSocketWrapper=%s", serverSocketWrapper);
    }

    @NonNull
    @Override
    public ThreadTCPConnectionManager.SocketWrapperEventHandler getSocketWrapperEventHandler() {
        return this;
    }

    @Override
    public void onSocketConnected(@NonNull SocketWrapper socketWrapper) {
        Log.i(TAG, "Connected to a socket, socketWrapper=%s", socketWrapper);
        // do nothing until we can get the name of the device.
    }

    @Override
    public void onSocketConnectFailed(@NonNull SocketWrapper socketWrapper) {
        Log.d(TAG, "Should not reach here (onSocketConnectFailed), group owner will never try to connect to a socket socketWrapper=%s", socketWrapper);
    }

    @Override
    public void onSocketWrapperNameReceived(@NonNull SocketWrapper socketwrapper, @NonNull String name) {
        // Connect only if the other side is my neighbor
        int i;
        String[] wifiDirectNeighbors = Constants.getWifiDirectNeighbors();
        for (i = 0; i < wifiDirectNeighbors.length; i++) {
            if (name.equals(wifiDirectNeighbors[i])) {
                break;
            }
        }
        if (i == wifiDirectNeighbors.length) {
            Log.i(TAG, "Connected device (%s) is not my neighbor, close connection!", name);
            getThreadTCPConnectionManager().closeSocketWrapper(socketwrapper);
            return;
        }

        SocketWrapper origiSocketWrapper;
        synchronized (connectedNeighbors) {
            origiSocketWrapper = connectedNeighbors.put(name, socketwrapper);
            Log.i(TAG, "Received name=%s, originalSocketWrapper=%s", name, origiSocketWrapper);
            if (origiSocketWrapper != null) {
                connectedNeighborsReverse.remove(origiSocketWrapper);
            }
            connectedNeighborsReverse.put(socketwrapper, name);
        }
        if (origiSocketWrapper != null) {
            getThreadTCPConnectionManager().closeSocketWrapper(origiSocketWrapper);
        } else {
            Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                    new Intent(LinkLayer.ACTION_LINK_CHANGED).
                            putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                            putExtra(LinkLayer.EXTRA_CONNECTED, true));
        }
        // if not null, it is already in the connected state
    }

    @Override
    public void onSocketWrapperDataReceived(@NonNull SocketWrapper socketWrapper, @NonNull byte[] data) {
        String name = connectedNeighborsReverse.get(socketWrapper);
        // from a neighbor that does not exist? how can that be?
        if (name == null) return;
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                new Intent(LinkLayer.ACTION_DATA_RECEIVED).
                        putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                        putExtra(LinkLayer.EXTRA_DATA, data));
    }

    @Override
    public void onSocketWrapperClosed(@NonNull SocketWrapper socketWrapper) {
        String name;
        synchronized (connectedNeighbors) {
            name = connectedNeighborsReverse.remove(socketWrapper);
            if (name != null) connectedNeighbors.remove(name);
        }
        if (name != null) {
            Log.i(TAG, "Closed a remote socket socketWrapper=%s, original remote device: %s", socketWrapper, name);
            Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                    new Intent(LinkLayer.ACTION_LINK_CHANGED).
                            putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                            putExtra(LinkLayer.EXTRA_CONNECTED, false));
        } else {
            Log.i(TAG, "Closed a remote socket socketWraooer=%s", socketWrapper);
        }
        // I'm a group owner, wait for the client to reconnect.
    }

    @Override
    public void onSocketWrapperCloseFailed(@NonNull SocketWrapper socketWrapper) {
        Log.e(TAG, "Failed in closing a remote socket, socketWrapper=%s", socketWrapper);
    }
}

class WifiThreadTCPConnectionManagerClient extends WifiThreadTCPConnectionManager implements ThreadTCPConnectionManager.SocketWrapperEventHandler {
    private static final String TAG = "WifiThreadTCPConnectionManagerClient";
    @Nullable
    private SocketWrapper currentSocket;
    private boolean reconnect = false;
    private String connectedName;
    private InetSocketAddress address;
//    @NonNull
//    private final String[] wifiDirectNeighbors;

    WifiThreadTCPConnectionManagerClient(@NonNull ThreadTCPConnectionManager threadTCPConnectionManager) {
        super(threadTCPConnectionManager);
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
                    DelayRunner.getDefaultInstance().post(WifiThreadTCPConnectionManagerClient.this::establishConnection);
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
                getThreadTCPConnectionManager().closeSocketWrapper(currentSocket);
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
            SocketWrapper socketWrapper = getThreadTCPConnectionManager().addSocketWrapperTCP(address, this);
            if (socketWrapper == null) {
                Log.i(TAG, "Failed in creating socketWrapper, retry in %dms", SOCKET_RCONNECT_DURATION_MS);
                DelayRunner.getDefaultInstance().postDelayed(SOCKET_RCONNECT_DURATION_MS, this::establishConnection);
            } else {
                Log.i(TAG, "Succeeded in creating socketWrapper: %s", socketWrapper);
                socketWrapper.start();
            }
            currentSocket = socketWrapper;
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
            return getThreadTCPConnectionManager().writeToSocket(currentSocket, buf);
        }
    }

    @Override
    public void onSocketConnected(@NonNull SocketWrapper socketWrapper) {
        // do nothing, wait for the other side to give me the name.
        synchronized (this) {
            if (socketWrapper != currentSocket) return;
        }
        Log.v(TAG, "Socket connected: %s", socketWrapper);
    }

    @Override
    public void onSocketConnectFailed(@NonNull SocketWrapper socketWrapper) {
        synchronized (this) {
            if (socketWrapper != currentSocket) return;
            currentSocket = null;
        }
        Log.v(TAG, "onSocketConnectFailed: %s, reconnect=%b", socketWrapper, reconnect);
        if (reconnect) {
            Log.i(TAG, "Connection failed on socketWrapper %s, reconnect in %dms.", socketWrapper, SOCKET_RCONNECT_DURATION_MS);
            DelayRunner.getDefaultInstance().postDelayed(SOCKET_RCONNECT_DURATION_MS, this::establishConnection);
        }
    }

    @Override
    public void onSocketWrapperNameReceived(@NonNull SocketWrapper socketWrapper, @NonNull String name) {
        // if that is not my neighbor, disconnect.
        int i;
        String[] wifiDirectNeighbors = Constants.getWifiDirectNeighbors();
        for (i = 0; i < wifiDirectNeighbors.length; i++)
            if (name.equals(wifiDirectNeighbors[i]))
                break;

        if (i == wifiDirectNeighbors.length) {
            Log.i(TAG, "Connected device (%s) is not my neighbor, colse connection!", name);
            getThreadTCPConnectionManager().closeSocketWrapper(socketWrapper);
            return;
        }

        synchronized (this) {
            if (socketWrapper != currentSocket) return;
            connectedName = name;
        }
        Log.i(TAG, "Connected to %s, %s", name, socketWrapper);
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                new Intent(LinkLayer.ACTION_LINK_CHANGED).
                        putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                        putExtra(LinkLayer.EXTRA_CONNECTED, true));
    }

    @Override
    public void onSocketWrapperClosed(@NonNull SocketWrapper socketWrapper) {
        // If I'm a client and I know who I am, and the user still wants to connect
        String name = connectedName;
        Log.v(TAG, "onSocketWrapperClosed: %s, currentSocket: %s name=%s, reconnect=%b", socketWrapper, currentSocket, name, reconnect);
        synchronized (this) {
            // not closing the current socket, ignore
            if (socketWrapper != currentSocket) return;
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
            Log.i(TAG, "Closed socketWrapper=%s, reconnect in %dms", socketWrapper, SOCKET_RCONNECT_DURATION_MS);
            DelayRunner.getDefaultInstance().postDelayed(SOCKET_RCONNECT_DURATION_MS, this::establishConnection);
        } else {
            Log.i(TAG, "Closed socketWrapper=%s", socketWrapper);
        }

    }

    @Override
    public void onSocketWrapperCloseFailed(@NonNull SocketWrapper socketWrapper) {
        Log.e(TAG, "Failed in closing a remote socket, socketWrapper=%s", socketWrapper);
    }


    @Override
    public void onSocketWrapperDataReceived(@NonNull SocketWrapper socketWrapper, @NonNull byte[] data) {
        // from a neighbor that does not exist? how can that be?
        if (connectedName == null) return;
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                new Intent(LinkLayer.ACTION_DATA_RECEIVED)
                        .putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(connectedName))
                        .putExtra(LinkLayer.EXTRA_DATA, data));

    }

}
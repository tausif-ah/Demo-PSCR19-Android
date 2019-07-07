package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import java.util.HashMap;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.DelayRunner;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

public abstract class BluetoothThreadTCPConnectionManager {
    static final long SERVER_SOCKET_RETRY_DURATION_MS = 2000;

    static BluetoothThreadTCPConnectionManager createBluetoothTCPConnectionManager(ThreadTCPConnectionManager threadTCPConnectionManager) {
        if (Constants.getBluetoothNeighbors().length == 0)
            return new BluetoothThreadTCPConnectionManagerDoNothing(threadTCPConnectionManager);
        return new BluetoothThreadTCPConnectionManagerNormal(threadTCPConnectionManager);
    }

    @NonNull
    private final ThreadTCPConnectionManager threadTCPConnectionManager;

    BluetoothThreadTCPConnectionManager(@NonNull ThreadTCPConnectionManager threadTCPConnectionManager) {
        this.threadTCPConnectionManager = threadTCPConnectionManager;
    }

    @NonNull
    ThreadTCPConnectionManager getThreadTCPConnectionManager() {
        return threadTCPConnectionManager;
    }

    public abstract boolean isDeviceTCPConnected(@NonNull String name);

    public abstract void modifyConnection(@NonNull BluetoothDevice device, boolean establish);

    public abstract boolean closeConnection(@NonNull String name);

    public abstract boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len);
}

class BluetoothThreadTCPConnectionManagerDoNothing extends BluetoothThreadTCPConnectionManager {

    BluetoothThreadTCPConnectionManagerDoNothing(@NonNull ThreadTCPConnectionManager threadTCPConnectionManager) {
        super(threadTCPConnectionManager);
    }

    @Override
    public boolean isDeviceTCPConnected(@NonNull String name) {
        return false;
    }

    @Override
    public void modifyConnection(@NonNull BluetoothDevice device, boolean establish) {
    }

    @Override
    public boolean closeConnection(@NonNull String name) {
        return false;
    }

    @Override
    public boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        Helper.checkValidSendDataParams(data, start, len);
        return false;
    }
}

class BluetoothThreadTCPConnectionManagerNormal extends BluetoothThreadTCPConnectionManager implements ThreadTCPConnectionManager.ServerSocketWrapperEventHandler, ThreadTCPConnectionManager.SocketWrapperEventHandler {
    private static final String TAG = "BluetoothThreadTCPConnectionManagerNormal";

    private final HashMap<String, SocketWrapper> connectedNeighbors = new HashMap<>();
    private final HashMap<SocketWrapper, String> connectedNeighborsReverse = new HashMap<>();

    BluetoothThreadTCPConnectionManagerNormal(@NonNull ThreadTCPConnectionManager threadTCPConnectionManager) {
        super(threadTCPConnectionManager);
        DelayRunner.getDefaultInstance().post(this::startServerSocket);
    }

    @Override
    public boolean isDeviceTCPConnected(@NonNull String name) {
        return connectedNeighbors.containsKey(name);
    }

    @Override
    public void modifyConnection(@NonNull BluetoothDevice device, boolean establish) {
        if (establish) {
            Helper.notifyUser(LogType.Info, "Creating connection to %s...", device.getName());
            SocketWrapper socketWrapper = getThreadTCPConnectionManager().addSocketWrapperBluetooth(device, Constants.BLUETOOTH_SDP_UUID, this);
            if (socketWrapper == null) {
                Log.e(TAG, "Failed in creating connection to %s", device.getName());
                Helper.notifyUser(LogType.Info, "Failed in creating connection to %s", device.getName());
            } else {
                socketWrapper.start();
                Log.d(TAG, "created connection to %s, socketWrapper=%s", device.getName(), socketWrapper);
            }

        } else {
            String name = device.getName();
            SocketWrapper socketWrapper = connectedNeighbors.get(name);
            if (socketWrapper != null) {
                socketWrapper.close();
                Log.d(TAG, "trying to close connection to %s, socketWrapper=%s", name, socketWrapper);
            }
        }
    }

    @Override
    public boolean closeConnection(@NonNull String name) {
        SocketWrapper socketWrapper = connectedNeighbors.get(name);
        if (socketWrapper != null) {
            socketWrapper.close();
            Log.d(TAG, "trying to close connection to %s, socketWrapper=%s", name, socketWrapper);
            return true;
        }
        return false;
    }

    @Override
    public boolean sendData(@NonNull NeighborID id, @NonNull byte[] data, int start, int len) {
        Helper.checkValidSendDataParams(data, start, len);
        SocketWrapper socketWrapper = connectedNeighbors.get(id.getName());
        if (socketWrapper == null) return false;
        byte[] buf = new byte[len];
        if (len > 0) System.arraycopy(data, start, buf, 0, len);
        return getThreadTCPConnectionManager().writeToSocket(socketWrapper, buf);
    }

    private void startServerSocket() {
        ServerSocketWrapper serverSocketWrapper = getThreadTCPConnectionManager().addServerSocketWrapperBluetooth(Constants.BLUETOOTH_SDP_NAME, Constants.BLUETOOTH_SDP_UUID, this);
        if (serverSocketWrapper == null) {
            Log.i(TAG, "Failed in listening to bluetooth name=%s, uuid=%s, retry in %dms.", Constants.BLUETOOTH_SDP_NAME, Constants.BLUETOOTH_SDP_UUID, SERVER_SOCKET_RETRY_DURATION_MS);
            Helper.notifyUser(LogType.Info, "Failed in listening to bluetooth name=%s, uuid=%s, retry in %dms.", Constants.BLUETOOTH_SDP_NAME, Constants.BLUETOOTH_SDP_UUID, SERVER_SOCKET_RETRY_DURATION_MS);
            DelayRunner.getDefaultInstance().postDelayed(SERVER_SOCKET_RETRY_DURATION_MS, this::startServerSocket);
        } else {
            serverSocketWrapper.start();
            Log.i(TAG, "Succeed in listening to bluetooth name=%s, uuid=%s, serverSocketWrapper=%s", Constants.BLUETOOTH_SDP_NAME, Constants.BLUETOOTH_SDP_UUID, serverSocketWrapper);
            Helper.notifyUser(LogType.Info, "Succeed in listening to bluetooth name=%s, uuid=%s, serverSocketWrapper=%s", Constants.BLUETOOTH_SDP_NAME, Constants.BLUETOOTH_SDP_UUID, serverSocketWrapper);
        }
    }


    @Override
    public void onServerSocketWrapperAcceptFailed(@NonNull ServerSocketWrapper serverSocketWrapper) {
        Log.e(TAG, "Failed in accepting SocketWrapper, serverSocketWrapper=%s, retry in %dms", serverSocketWrapper, SERVER_SOCKET_RETRY_DURATION_MS);
        Helper.notifyUser(LogType.Info, "Failed in accepting SocketWrapper, serverSocketWrapper=%s, retry in %dms", serverSocketWrapper, SERVER_SOCKET_RETRY_DURATION_MS);
        DelayRunner.getDefaultInstance().postDelayed(SERVER_SOCKET_RETRY_DURATION_MS, this::startServerSocket);
    }

    @NonNull
    @Override
    public ThreadTCPConnectionManager.SocketWrapperEventHandler getSocketWrapperEventHandler() {
        return this;
    }

    @Override
    public void onSocketConnected(@NonNull SocketWrapper socketWrapper) {
        Log.i(TAG, "Connected to a socket, socketWrapper=%s", socketWrapper);
    }

    @Override
    public void onSocketConnectFailed(@NonNull SocketWrapper socketWrapper) {
        Log.i(TAG, "onSocketConnectFailed, socketWrapper=%s", socketWrapper);
    }

    @Override
    public void onSocketWrapperNameReceived(@NonNull SocketWrapper socketWrapper, @NonNull String name) {
        int i;
        String[] bluetoothNeighbors = Constants.getBluetoothNeighbors();
        for (i = 0; i < bluetoothNeighbors.length; i++) {
            if (name.equals(bluetoothNeighbors[i])) {
                break;
            }
        }
        if (i == bluetoothNeighbors.length) {
            Log.i(TAG, "Connected device (%s) is not my neighbor, close connection!", name);
            getThreadTCPConnectionManager().closeSocketWrapper(socketWrapper);
            return;
        }

        SocketWrapper origiSocketWrapper;
        synchronized (connectedNeighbors) {
            origiSocketWrapper = connectedNeighbors.put(name, socketWrapper);
            Log.i(TAG, "Received name=%s, originalSocketWrapper=%s", name, origiSocketWrapper);
            if (origiSocketWrapper != null) {
                connectedNeighborsReverse.remove(origiSocketWrapper);
            }
            connectedNeighborsReverse.put(socketWrapper, name);
        }
        if (origiSocketWrapper != null) {
            getThreadTCPConnectionManager().closeSocketWrapper(origiSocketWrapper);
        } else {
            Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).sendBroadcast(
                    new Intent(LinkLayer.ACTION_LINK_CHANGED).
                            putExtra(LinkLayer.EXTRA_NEIGHBOR_ID, new NeighborID(name)).
                            putExtra(LinkLayer.EXTRA_CONNECTED, true));
        }
        Log.i(TAG, "onSocketWrapperNameReceived, socketWrapper=%s, %s", socketWrapper, name);
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
    }

    @Override
    public void onSocketWrapperCloseFailed(@NonNull SocketWrapper socketWrapper) {
        Log.i(TAG, "Failed in closing a remote socket, socketWrapper=%s", socketWrapper);
    }
}
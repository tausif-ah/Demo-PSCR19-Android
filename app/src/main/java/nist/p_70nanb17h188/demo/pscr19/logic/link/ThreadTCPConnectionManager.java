package nist.p_70nanb17h188.demo.pscr19.logic.link;


import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

class ThreadTCPConnectionManager {
    private static final String TAG = "ThreadTCPConnectionManager";

    interface ServerSocketWrapperEventHandler {
        /**
         * Callback when the ServerSocketWrapper fails to accept a socket.
         *
         * @param serverSocketWrapper The ServerSocketWrapper failed to accept a socket.
         */
        void onServerSocketWrapperAcceptFailed(@NonNull ServerSocketWrapper serverSocketWrapper);

        /**
         * Gets the SocketWrapperEventHandler that handles the SocketWrappers accepted.
         *
         * @return The SocketWrapperEventHandler that handles the SocketWrappers accepted.
         */
        @NonNull
        SocketWrapperEventHandler getSocketWrapperEventHandler();
    }

    interface SocketWrapperEventHandler {

        /**
         * Callback when the SocketWrapper is successfully connected.
         *
         * @param socketWrapper The created SocketWrapper.
         */
        void onSocketConnected(@NonNull SocketWrapper socketWrapper);

        /**
         * Callback when the SocketWrapper fails to create.
         */
        void onSocketConnectFailed(@NonNull SocketWrapper socketWrapper);

        /**
         * Callback when the name is received. It only happens once.
         *
         * @param socketWrapper The SocketWrapper received the name.
         * @param name          The name received.
         */
        void onSocketWrapperNameReceived(@NonNull SocketWrapper socketWrapper, @NonNull String name);

        /**
         * Callback when a piece of data is received. It always happen after name is received.
         *
         * @param socketWrapper The SocketWrapper the data is received.
         * @param data          The data received.
         */
        void onSocketWrapperDataReceived(@NonNull SocketWrapper socketWrapper, @NonNull byte[] data);

        /**
         * Callback when the SocketWrapper is closed.
         *
         * @param socketWrapper The closed SocketWrapper.
         */
        void onSocketWrapperClosed(@NonNull SocketWrapper socketWrapper);

        /**
         * Callback when a close operation on a SocketWrapper fails.
         *
         * @param socketWrapper The SocketWrapper that failed to close.
         */
        void onSocketWrapperCloseFailed(@NonNull SocketWrapper socketWrapper);
    }

    ThreadTCPConnectionManager() {

    }

    // do nothing
    void start() {
    }

    /**
     * Listen to a local port.
     *
     * @param port                            The local port to listen to.
     * @param serverSocketWrapperEventHandler The event handler that deals with ServerSocketWrapper.
     * @return The created ServerSocketWrapper. Null if failed in creation.
     */
    @Nullable
    ServerSocketWrapper addServerSocketWrapperTCP(int port, @NonNull ServerSocketWrapperEventHandler serverSocketWrapperEventHandler) {
        try {
            return new ServerSocketWrapperTCP(port, serverSocketWrapperEventHandler);
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in creating ServerSocketWrapperTCP");
            return null;
        }
    }

    /**
     * Listen to a bluetooth server socket.
     *
     * @param name                            The name to start a bluetooth server socket.
     * @param uuid                            The UUID to start a bluetooth server socket.
     * @param serverSocketWrapperEventHandler The event handler that deals with ServerSocketWrapper
     * @return The created ServerSocketWrapper. Null if failed in creation.
     */
    @Nullable
    ServerSocketWrapper addServerSocketWrapperBluetooth(@NonNull String name, @NonNull UUID uuid, @NonNull ServerSocketWrapperEventHandler serverSocketWrapperEventHandler) {
        try {
            return new ServerSocketWrapperBluetooth(name, uuid, serverSocketWrapperEventHandler);
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in creating ServerSocketWrapperBluetooth");
            return null;
        }
    }

    /**
     * Start a TCP socket.
     *
     * @param remoteAddress             The remote address to connect to.
     * @param socketWrapperEventHandler The event handler that deals with SocketWrapper.
     * @return The created SocketWrapper. Null if failed in creation.
     */
    @Nullable
    SocketWrapper addSocketWrapperTCP(@NonNull InetSocketAddress remoteAddress, @NonNull SocketWrapperEventHandler socketWrapperEventHandler) {
        try {
            return new SocketWrapperTCP(remoteAddress.getAddress(), remoteAddress.getPort(), socketWrapperEventHandler);
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in creating SocketWrapperTCP");
            return null;
        }
    }

    /**
     * Start a Bluetooth socket.
     *
     * @param remoteDevice              The remote device to connect to.
     * @param socketWrapperEventHandler The event handler that deals with SocketWrapper.
     * @return The create SocketWrapper. Null if failed in creation.
     */
    @Nullable
    SocketWrapper addSocketWrapperBluetooth(@NonNull BluetoothDevice remoteDevice, @NonNull UUID uuid, @NonNull SocketWrapperEventHandler socketWrapperEventHandler) {
        try {
            return new SocketWrapperBluetooth(remoteDevice, uuid, socketWrapperEventHandler);
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in creating addSocketWrapperBluetooth");
            return null;
        }
    }

    /**
     * Close a socket wrapper.
     *
     * @param socketWrapper The SocketWrapper to close.
     */
    void closeSocketWrapper(@NonNull SocketWrapper socketWrapper) {
        socketWrapper.close();
    }

    boolean writeToSocket(@NonNull SocketWrapper socketWrapper, @NonNull byte[] data) {
        socketWrapper.write(data);
        return true;
    }
}

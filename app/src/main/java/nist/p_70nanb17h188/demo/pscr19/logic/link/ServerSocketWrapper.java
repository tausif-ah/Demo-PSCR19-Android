package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

abstract class ServerSocketWrapper implements AutoCloseable, Closeable, Runnable {
    private static final String TAG = "ServerSocketWrapper";
    private final ThreadTCPConnectionManager.ServerSocketWrapperEventHandler serverSocketWrapperEventHandler;

    @NonNull
    protected abstract SocketWrapper accept(ThreadTCPConnectionManager.SocketWrapperEventHandler socketWrapperEventHandler) throws IOException;

    protected boolean isConnected() {
        return !isClosed();
    }

    protected abstract boolean isClosed();

    ServerSocketWrapper(ThreadTCPConnectionManager.ServerSocketWrapperEventHandler serverSocketWrapperEventHandler) {
        this.serverSocketWrapperEventHandler = serverSocketWrapperEventHandler;
    }

    public void start() {
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void run() {
        while (isConnected()) {
            try {
                SocketWrapper wrapper = accept(serverSocketWrapperEventHandler.getSocketWrapperEventHandler());
                serverSocketWrapperEventHandler.getSocketWrapperEventHandler().onSocketConnected(wrapper);
                wrapper.start();
                Log.d(TAG, "Accepted a socket: %s", wrapper);
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, e, "Failed in accepting socket, serverSocket: %s", this);
                serverSocketWrapperEventHandler.onServerSocketWrapperAcceptFailed(this);
            }
        }
        Log.d(TAG, "ServerSocketWrapper %s finished!", this);
    }
}

class ServerSocketWrapperTCP extends ServerSocketWrapper {
    private final ServerSocket serverSocket;

    ServerSocketWrapperTCP(int port, ThreadTCPConnectionManager.ServerSocketWrapperEventHandler serverSocketWrapperEventHandler) throws IOException {
        super(serverSocketWrapperEventHandler);
        serverSocket = new ServerSocket(port);
    }

    @NonNull
    @Override
    protected SocketWrapper accept(ThreadTCPConnectionManager.SocketWrapperEventHandler socketWrapperEventHandler) throws IOException {
        Socket socket = serverSocket.accept();
        return new SocketWrapperTCP(socket, socketWrapperEventHandler);
    }

    @Override
    protected boolean isClosed() {
        return serverSocket.isClosed();
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }
}

class ServerSocketWrapperBluetooth extends ServerSocketWrapper {
    private final BluetoothServerSocket serverSocket;
    private boolean closed = false;

    ServerSocketWrapperBluetooth(String name, UUID uuid, ThreadTCPConnectionManager.ServerSocketWrapperEventHandler serverSocketWrapperEventHandler) throws IOException {
        super(serverSocketWrapperEventHandler);
        serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord(name, uuid);
    }

    @NonNull
    @Override
    protected SocketWrapper accept(ThreadTCPConnectionManager.SocketWrapperEventHandler socketWrapperEventHandler) throws IOException {
        try {
            BluetoothSocket bluetoothSocket = serverSocket.accept();
            return new SocketWrapperBluetooth(bluetoothSocket, socketWrapperEventHandler);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    @Override
    protected boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        closed = true;
    }
}

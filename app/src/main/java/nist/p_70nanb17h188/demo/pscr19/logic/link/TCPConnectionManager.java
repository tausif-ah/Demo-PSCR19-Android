package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.app.Application;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;

import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;


class TCPConnectionManager {

    private static final String TAG = "TCPConnectionManager";
    private static TCPConnectionManager DEFAULT_INSTANCE = null;
    private final Application application;
    private final Handler handler;
    private final Selector selector;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);

    private TCPConnectionManager(@NonNull Application application) throws IOException {
        this.application = application;
        this.handler = new Handler(application.getApplicationContext().getMainLooper());
        selector = SelectorProvider.provider().openSelector();
        new Thread(this::mainLoop, TAG).start();
    }

    static TCPConnectionManager getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    static void init(@NonNull Application application) {
        if (DEFAULT_INSTANCE == null) {
            try {
                DEFAULT_INSTANCE = new TCPConnectionManager(application);
            } catch (Exception e) {
                Toast.makeText(application.getApplicationContext(), "Failed in initing TCPConnectionManager!", Toast.LENGTH_LONG).show();
                Log.e(TAG, e, "Failed in initing TCPConnectionManager!");
            }
        }
    }

    private static void bindServerSocketChannelAddress(@NonNull ServerSocketChannel serverSocketChannel, @NonNull InetSocketAddress address) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) serverSocketChannel.bind(address);
        else serverSocketChannel.socket().bind(address);
    }

    static SocketAddress getSocketChannelRemoteAddress(@NonNull SocketChannel socketChannel) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) return socketChannel.getRemoteAddress();
        else return socketChannel.socket().getRemoteSocketAddress();
    }

    /**
     * Listen to a local address.
     *
     * @param addr                            The local address.
     * @param serverSocketChannelEventHandler The event handler that deals with ServerSocketChannel.
     * @return The created ServerSocketChannel. Null if failed in creation.
     */
    ServerSocketChannel addServerSocketChannel(@NonNull InetSocketAddress addr, @Nullable ServerSocketChannelEventHandler serverSocketChannelEventHandler) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            bindServerSocketChannelAddress(serverSocketChannel, addr);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, serverSocketChannelEventHandler);
            return serverSocketChannel;
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in adding Server Socket Channel!");
            return null;
        }
    }

    /**
     * Stop listening from a ServerSocketChannel
     *
     * @param serverSocketChannel The server socket channel to stop listen from.
     */
    void closeServerSocketChannel(@NonNull ServerSocketChannel serverSocketChannel) {
        SelectionKey key = serverSocketChannel.keyFor(selector);
        // did not add to the selector, not my responsibility
        if (key == null) return;
        ServerSocketChannelEventHandler serverSocketChannelEventHandler = (ServerSocketChannelEventHandler) key.attachment();
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in closing serverSocketChannel (%s)!", serverSocketChannel);
            if (serverSocketChannelEventHandler != null)
                serverSocketChannelEventHandler.onServerSocketChannelCloseFailed(serverSocketChannel);
        }
        key.cancel();
        if (serverSocketChannelEventHandler != null)
            serverSocketChannelEventHandler.onServerSocketChannelClosed(serverSocketChannel);
    }

    /**
     * Create a connection to a remote address.
     *
     * @param remoteAddress             The remote address to connect to.
     * @param socketChannelEventHandler The handler that deals with events related to the socket.
     * @return The created SocketChannel. Null on failure.
     */
    SocketChannel addSocketChannel(@NonNull InetSocketAddress remoteAddress, SocketChannelEventHandler socketChannelEventHandler) {
        SelectionKey key = null;
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            key = socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT, new SocketChannelBufferHandler(socketChannelEventHandler));
            socketChannel.connect(remoteAddress);
            return socketChannel;
        } catch (IOException e) {
            if (key != null) key.cancel();
            Log.e(TAG, e, "Failed in adding Socket Channel!");
            return null;
        }
    }

    /**
     * Stop connection of a SocketChannel.
     *
     * @param socketChannel The SocketChannel to stop listen from.
     */
    void closeSocketChannel(@NonNull SocketChannel socketChannel) {
        SelectionKey key = socketChannel.keyFor(selector);
        // did not add to selector, not my responsibility
        if (key == null) return;
        SocketChannelBufferHandler socketChannelBufferHandler = (SocketChannelBufferHandler) key.attachment();
        try {
            socketChannel.close();
            // the key.cancel and fire onClose should happen in read
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in removing Socket Channel!");
            if (socketChannelBufferHandler.socketChannelEventHandler != null)
                socketChannelBufferHandler.socketChannelEventHandler.onSocketChannelCloseFailed(socketChannel);
        }
    }

    void sendKeepAlive(@NonNull SocketChannel socketChannel) {
        // TODO:
    }

    void writeToSocket(@NonNull SocketChannel socketChannel, @NonNull byte[] data) {
        // TODO:
    }

    private void mainLoop() {
        while (true) {
            try {
                this.selector.select(1000);
                for (SelectionKey selectedKey : selector.selectedKeys()) {
                    if (selectedKey == null) continue;
                    if (selectedKey.isValid()) {
                        if (selectedKey.isAcceptable()) {
                            accept(selectedKey);
                        } else if (selectedKey.isConnectable()) {
                            connect(selectedKey);
                        } else if (selectedKey.isReadable()) {
                            read(selectedKey);
                        } else if (selectedKey.isWritable()) {
//                            System.out.printf("Write: %s%n", selectedKey.channel());
//                            Thread.sleep(5000);
//                            selectedKey.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }
                selector.selectedKeys().clear();
            } catch (IOException | RuntimeException ex) {
                Log.e(TAG, ex, "Failed in mainloop.");
            }
        }
    }

    private void accept(@NonNull SelectionKey key) {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        ServerSocketChannelEventHandler serverSocketChannelEventHandler = (ServerSocketChannelEventHandler) key.attachment();
        SocketChannelEventHandler socketChannelEventHandler = serverSocketChannelEventHandler == null ? null : serverSocketChannelEventHandler.getSocketChannelEventHandler();
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ, new SocketChannelBufferHandler(socketChannelEventHandler));
            if (socketChannelEventHandler != null)
                socketChannelEventHandler.onSocketConnected(socketChannel);
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in accepting socket channel, serverSocketChannel=%s", serverSocketChannel);
            if (serverSocketChannelEventHandler != null)
                serverSocketChannelEventHandler.onServerSocketChannelAcceptFailed(serverSocketChannel);
        }
    }

    private void connect(@NonNull SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        SocketChannelBufferHandler socketChannelBufferHandler = (SocketChannelBufferHandler) key.attachment();
        try {
            if (socketChannel.finishConnect()) {
                if (socketChannelBufferHandler.socketChannelEventHandler != null)
                    socketChannelBufferHandler.socketChannelEventHandler.onSocketConnected(socketChannel);
                return;
            }
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in finishing connect a socket channel, socketChannel=%s", socketChannel);
        }
        try {
            socketChannel.close();
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in closing a socket channel [in connect], socketChannel=%s", socketChannel);
        }
        key.cancel();
        if (socketChannelBufferHandler.socketChannelEventHandler != null)
            socketChannelBufferHandler.socketChannelEventHandler.onSocketConnectFailed(socketChannel);
    }

    private void read(@NonNull SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        SocketChannelBufferHandler socketChannelBufferHandler = (SocketChannelBufferHandler) key.attachment();

        readBuffer.clear();
        int numRead;
        try {
            numRead = socketChannel.read(readBuffer);
            if (numRead >= 0) {
                socketChannelBufferHandler.bufferBytes(readBuffer, numRead);
                return;
            }
        } catch (IOException e) {
            Log.e(TAG, e, "Error in reading! %s", socketChannel);
        }
        key.cancel();
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (socketChannelBufferHandler.socketChannelEventHandler != null)
            socketChannelBufferHandler.socketChannelEventHandler.onSocketChannelClosed(socketChannel);
    }

    interface ServerSocketChannelEventHandler {
        /**
         * Callback when a ServerSocketChannel is closed.
         *
         * @param serverSocketChannel The closed ServerSocketChannel.
         */
        void onServerSocketChannelClosed(ServerSocketChannel serverSocketChannel);

        /**
         * Callback when a ServerSocketChannel fails to create.
         *
         * @param serverSocketChannel The ServerSocketChannel failed to create.
         */
        void onServerSocketChannelCloseFailed(ServerSocketChannel serverSocketChannel);

        /**
         * Callback when the ServerSocketChannel fails to accept a socket.
         *
         * @param serverSocketChannel The ServerSocketChannel failed to accept a socket.
         */
        void onServerSocketChannelAcceptFailed(ServerSocketChannel serverSocketChannel);

        /**
         * Gets the SocketChannelEventHandler that handles the SocketChannels accepted.
         *
         * @return The SocketChannelEventHandler that handles the SocketChannels accepted.
         */
        SocketChannelEventHandler getSocketChannelEventHandler();
    }

    interface SocketChannelEventHandler {
        /**
         * Callback when the SocketChannel is successfully connected.
         *
         * @param socketChannel The created SocketChannel.
         */
        void onSocketConnected(@NonNull SocketChannel socketChannel);

        /**
         * Callback when the SocketChannel fails to create.
         */
        void onSocketConnectFailed(@NonNull SocketChannel socketChannel);

        void onSocketChannelNameReceived(@NonNull SocketChannel socketChannel, String name);

        void onSocketChannelDataReceived(@NonNull SocketChannel socketChannel, byte[] data);

        void onSocketChannelClosed(@NonNull SocketChannel socketChannel);

        void onSocketChannelCloseFailed(@NonNull SocketChannel socketChannel);
    }

    static class SocketChannelBufferHandler {

        final SocketChannelEventHandler socketChannelEventHandler;

        SocketChannelBufferHandler(SocketChannelEventHandler socketChannelEventHandler) {
            this.socketChannelEventHandler = socketChannelEventHandler;
        }

        void bufferBytes(ByteBuffer buffer, int size) {
            if (size == 0) return;
            buffer.rewind();
            byte[] buf = new byte[size];
            buffer.get(buf);
            Log.i(TAG, "Read (%d)%n%s", size, Arrays.toString(buf));
        }
    }
}

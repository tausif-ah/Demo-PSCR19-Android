package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import nist.p_70nanb17h188.demo.pscr19.logic.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

class TCPConnectionManager {
    private static final String TAG = "TCPConnectionManager";
    private static final int DEFAULT_READ_BUFFER_SIZE = 8192;
    // if the value is too small, a lot of computation overhead
    // if the value is too large, keep-alive will not be checked properly.
    private static final long SELECTOR_SELECT_TIMEOUT_MS = 500;
    private static TCPConnectionManager DEFAULT_INSTANCE = null;

    @NonNull
    private final Selector selector;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
    private final ArrayList<PendingSocketChannel> toConnects = new ArrayList<>();

    private TCPConnectionManager() throws IOException {
        selector = SelectorProvider.provider().openSelector();
        Thread t = new Thread(this::mainLoop, TAG);
        t.setDaemon(true);
        t.start();
    }

    static TCPConnectionManager getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    static TCPConnectionManager init() {
        if (DEFAULT_INSTANCE == null) {
            try {
                DEFAULT_INSTANCE = new TCPConnectionManager();
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, e, "Failed in initing TCPConnectionManager!");
            }
        }
        return DEFAULT_INSTANCE;
    }

    private static void bindServerSocketChannelAddress(@NonNull ServerSocketChannel serverSocketChannel, @NonNull InetSocketAddress address) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            serverSocketChannel.bind(address);
        } else {
            serverSocketChannel.socket().bind(address);
        }
    }

    static SocketAddress getSocketChannelRemoteAddress(@NonNull SocketChannel socketChannel) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return socketChannel.getRemoteAddress();
        } else {
            return socketChannel.socket().getRemoteSocketAddress();
        }
    }

    private static void setSocketChannelOptions(@NonNull SocketChannel socketChannel) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        } else {
            socketChannel.socket().setTcpNoDelay(true);
            socketChannel.socket().setKeepAlive(true);
        }
    }

    /**
     * Listen to a local address.
     *
     * @param addr                            The local address.
     * @param serverSocketChannelEventHandler The event handler that deals with
     *                                        ServerSocketChannel.
     * @return The created ServerSocketChannel. Null if failed in creation.
     */
    @Nullable
    ServerSocketChannel addServerSocketChannel(@NonNull InetSocketAddress addr, @Nullable ServerSocketChannelEventHandler serverSocketChannelEventHandler) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            bindServerSocketChannelAddress(serverSocketChannel, addr);
            selector.wakeup();
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
        if (key == null) {
            Log.e(TAG, "Cannot fine key for ServerSocketChannel (%s), not registered!", serverSocketChannel);
            return;
        }
        ServerSocketChannelEventHandler serverSocketChannelEventHandler = (ServerSocketChannelEventHandler) key.attachment();
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in closing serverSocketChannel (%s)!", serverSocketChannel);
            if (serverSocketChannelEventHandler != null) {
                serverSocketChannelEventHandler.onServerSocketChannelCloseFailed(serverSocketChannel);
            }
        }
        key.cancel();
        if (serverSocketChannelEventHandler != null) {
            serverSocketChannelEventHandler.onServerSocketChannelClosed(serverSocketChannel);
        }
    }

    /**
     * Create a connection to a remote address.
     *
     * @param remoteAddress             The remote address to connect to.
     * @param socketChannelEventHandler The handler that deals with events
     *                                  related to the socket.
     * @return The created SocketChannel. Null on failure.
     */
    @Nullable
    SocketChannel addSocketChannel(@NonNull InetSocketAddress remoteAddress, @Nullable SocketChannelEventHandler socketChannelEventHandler) {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            setSocketChannelOptions(socketChannel);
            selector.wakeup();
            socketChannel.register(selector, SelectionKey.OP_CONNECT, new SocketChannelBufferHandler(socketChannel, socketChannelEventHandler));
            synchronized (toConnects) {
                toConnects.add(new PendingSocketChannel(socketChannel, remoteAddress));
            }
            return socketChannel;
        } catch (IOException e) {
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
        if (key == null) {
            Log.e(TAG, "Cannot fine key for SocketChannel (%s), not registered!", socketChannel);
            return;
        }
        SocketChannelBufferHandler socketChannelBufferHandler = (SocketChannelBufferHandler) key.attachment();
        innerCloseSocketChannel(key, socketChannelBufferHandler);
    }

    void writeToSocket(@NonNull SocketChannel socketChannel, @NonNull byte[] data) {
        SelectionKey key = socketChannel.keyFor(selector);
        // did not add to selector, not my responsibility
        if (key == null) {
            Log.e(TAG, "Cannot fine key for SocketChannel (%s), not registered!", socketChannel);
            return;
        }
        SocketChannelBufferHandler socketChannelBufferHandler = (SocketChannelBufferHandler) key.attachment();
        socketChannelBufferHandler.writeData(data);
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();
    }

    private void mainLoop() {
        while (true) {
            synchronized (toConnects) {
                for (PendingSocketChannel toConnect : toConnects) {
                    try {
                        toConnect.channel.connect(toConnect.remoteAddress);
                    } catch (IOException e) {
                        Log.e(TAG, e, "Failed in connecting to address: %s", toConnect.remoteAddress);
                    }
                }
                toConnects.clear();
            }

            try {
                selector.select(SELECTOR_SELECT_TIMEOUT_MS);
            } catch (IOException | RuntimeException ex) {
                Log.e(TAG, ex, "Failed in selecting selector!");
                continue;
            }
            for (SelectionKey selectedKey : selector.selectedKeys()) {
                if (selectedKey == null || !selectedKey.isValid()) {
                    continue;
                }
                try {
//                    Log.d(TAG, "mainLoop, key.readyOps=%d", selectedKey.readyOps());
                    if (selectedKey.isAcceptable()) {
                        accept(selectedKey);
                    } else if (selectedKey.isConnectable()) {
                        connect(selectedKey);
                    } else if (selectedKey.isReadable()) {
                        read(selectedKey);
                    } else if (selectedKey.isWritable()) {
                        write(selectedKey);
                    }
                } catch (RuntimeException ex) {
                    Log.e(TAG, ex, "Failed in handling key %s.", selectedKey);
                }
            }
            selector.selectedKeys().clear();
//            Log.d(TAG, "key size: %d", selector.keys().size());
            for (SelectionKey key : selector.keys()) {
                if (!key.isValid()) {
                    continue;
                }
                try {
                    Object attachment = key.attachment();
                    if (attachment instanceof SocketChannelBufferHandler) {
                        SocketChannelBufferHandler socketChannelBufferHandler = (SocketChannelBufferHandler) attachment;
                        int ret = socketChannelBufferHandler.tryKeepAlive();
                        if (ret > 0) {
                            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        } else if (ret < 0) {
                            innerCloseSocketChannel(key, socketChannelBufferHandler);
                        }
                    }
                } catch (RuntimeException ex) {
                    Log.e(TAG, ex, "Failed in handling key %s.", key);
                }
            }
        }
    }


    private void innerCloseSocketChannel(@NonNull SelectionKey key, @NonNull SocketChannelBufferHandler socketChannelBufferHandler) {
        SocketChannel socketChannel = socketChannelBufferHandler.socketChannel;
        try {
            socketChannel.close();
            if (socketChannelBufferHandler.socketChannelEventHandler != null) {
                socketChannelBufferHandler.socketChannelEventHandler.onSocketChannelClosed(socketChannel);
            }
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in removing Socket Channel!");
            if (socketChannelBufferHandler.socketChannelEventHandler != null) {
                socketChannelBufferHandler.socketChannelEventHandler.onSocketChannelCloseFailed(socketChannel);
            }
        }
        key.cancel();
    }

    private void accept(@NonNull SelectionKey key) {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        ServerSocketChannelEventHandler serverSocketChannelEventHandler = (ServerSocketChannelEventHandler) key.attachment();
        SocketChannelEventHandler socketChannelEventHandler = serverSocketChannelEventHandler == null ? null : serverSocketChannelEventHandler.getSocketChannelEventHandler();
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            setSocketChannelOptions(socketChannel);
            SocketChannelBufferHandler socketChannelBufferHandler = new SocketChannelBufferHandler(socketChannel, socketChannelEventHandler);
            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, socketChannelBufferHandler);
            socketChannelBufferHandler.setConnected();
            if (socketChannelEventHandler != null) {
                socketChannelEventHandler.onSocketConnected(socketChannel);
            }
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in accepting socket channel, serverSocketChannel=%s", serverSocketChannel);
            if (serverSocketChannelEventHandler != null) {
                serverSocketChannelEventHandler.onServerSocketChannelAcceptFailed(serverSocketChannel);
            }
        }
    }

    private void connect(@NonNull SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Log.v(TAG, "connect(), channel=%s", socketChannel);
        SocketChannelBufferHandler socketChannelBufferHandler = (SocketChannelBufferHandler) key.attachment();
        try {
            if (socketChannel.finishConnect()) {
                socketChannelBufferHandler.setConnected();
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                if (socketChannelBufferHandler.socketChannelEventHandler != null) {
                    socketChannelBufferHandler.socketChannelEventHandler.onSocketConnected(socketChannel);
                }
                return;
            }
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in finishing connect a socket channel, socketChannel=%s", socketChannel);
        }
        if (socketChannelBufferHandler.socketChannelEventHandler != null) {
            socketChannelBufferHandler.socketChannelEventHandler.onSocketConnectFailed(socketChannel);
        }
    }

    private void read(@NonNull SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        SocketChannelBufferHandler socketChannelBufferHandler = (SocketChannelBufferHandler) key.attachment();

        readBuffer.clear();
        int numRead;
        try {
            numRead = socketChannel.read(readBuffer);
            if (numRead >= 0) {
                readBuffer.rewind();
                if (socketChannelBufferHandler.bufferBytes(readBuffer.array(), numRead)) {
                    return;
                }
                // we believe that the other side is not acting as the program requires. kill the socket.
            }
        } catch (IOException e) {
            Log.e(TAG, e, "Error in reading! %s", socketChannel);
        }
        innerCloseSocketChannel(key, socketChannelBufferHandler);
    }

    private void write(@NonNull SelectionKey key) {
        SocketChannelBufferHandler socketChannelBufferHandler = (SocketChannelBufferHandler) key.attachment();

        try {
            if (socketChannelBufferHandler.performWrite()) {
                key.interestOps(SelectionKey.OP_READ);
//                Log.d(TAG, "perform write returned true");
            } else {
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }
            return;
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        innerCloseSocketChannel(key, socketChannelBufferHandler);

    }

    private static class PendingSocketChannel {
        final SocketChannel channel;
        final InetSocketAddress remoteAddress;

        PendingSocketChannel(SocketChannel channel, InetSocketAddress remoteAddress) {
            this.channel = channel;
            this.remoteAddress = remoteAddress;
        }
    }

    interface ServerSocketChannelEventHandler {

        /**
         * Callback when a ServerSocketChannel is closed.
         *
         * @param serverSocketChannel The closed ServerSocketChannel.
         */
        void onServerSocketChannelClosed(@NonNull ServerSocketChannel serverSocketChannel);

        /**
         * Callback when a ServerSocketChannel fails to create.
         *
         * @param serverSocketChannel The ServerSocketChannel failed to create.
         */
        void onServerSocketChannelCloseFailed(@NonNull ServerSocketChannel serverSocketChannel);

        /**
         * Callback when the ServerSocketChannel fails to accept a socket.
         *
         * @param serverSocketChannel The ServerSocketChannel failed to accept a
         *                            socket.
         */
        void onServerSocketChannelAcceptFailed(@NonNull ServerSocketChannel serverSocketChannel);

        /**
         * Gets the SocketChannelEventHandler that handles the SocketChannels
         * accepted.
         *
         * @return The SocketChannelEventHandler that handles the SocketChannels
         * accepted.
         */
        @NonNull
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

        /**
         * Callback when the name is received. It only happens once.
         *
         * @param socketChannel The socket channel received the name.
         * @param name          The name received.
         */
        void onSocketChannelNameReceived(@NonNull SocketChannel socketChannel, @NonNull String name);

        /**
         * Callback when a piece of data is received. It always happen after
         * name is received.
         *
         * @param socketChannel The socket channel the data is received.
         * @param data          The data received.
         */
        void onSocketChannelDataReceived(@NonNull SocketChannel socketChannel, @NonNull byte[] data);

        /**
         * Callback when the socketChannel is closed.
         *
         * @param socketChannel The closed socket channel.
         */
        void onSocketChannelClosed(@NonNull SocketChannel socketChannel);

        /**
         * Callback when a close operation on a socketChannel happens.
         *
         * @param socketChannel The socketChannel that failed to close.
         */
        void onSocketChannelCloseFailed(@NonNull SocketChannel socketChannel);
    }

    private static class SocketChannelBufferHandler {

        private static final int MAGIC = 0xdeadbeef;
        private static final byte TYPE_NAME = 1;
        private static final byte TYPE_KEEP_ALIVE = 2;
        private static final byte TYPE_DATA = 3;
        private static final long KEEP_ALIVE_DURATION_MS = 2000;
        private static final long KEEP_ALIVE_TIMEOUT_MS = 10000;
        private static final long GET_NAME_TIMEOUT_MS = 5000;
        private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
        private static final int STATE_READ_NAME_MAGIC_TYPE = 0;
        private static final int STATE_READ_NAME_LENGTH = 1;
        private static final int STATE_READ_NAME_CONTENT = 2;
        private static final int STATE_READ_MAGIC_TYPE = 3;
        private static final int STATE_READ_LENGTH = 4;
        private static final int STATE_READ_PAYLOAD = 5;
        @Nullable
        final SocketChannelEventHandler socketChannelEventHandler;
        @NonNull
        final SocketChannel socketChannel;
        private final ByteBuffer headerBuffer = ByteBuffer.allocate(Helper.INTEGER_SIZE + 1);
        private final ByteBuffer sizeBuffer = ByteBuffer.allocate(Helper.INTEGER_SIZE);
        private final Queue<ByteBuffer> toSend = new LinkedList<>();
        private long connectedTime;
        private long lastReadTime = 0, lastWriteTime = 0;
        private String name;
        private int state = STATE_READ_NAME_MAGIC_TYPE;
        private ByteBuffer contentBuffer;

        SocketChannelBufferHandler(@NonNull SocketChannel socketChannel, @Nullable SocketChannelEventHandler socketChannelEventHandler) {
            this.socketChannel = socketChannel;
            this.socketChannelEventHandler = socketChannelEventHandler;
        }

        void setConnected() {
//            Log.d(TAG, "Connected!");
            lastWriteTime = lastReadTime = connectedTime = System.currentTimeMillis();
            writeName();
        }

        void writeName() {
            byte[] data = Device.getName().getBytes(DEFAULT_CHARSET);
            writeVariableLengthData(TYPE_NAME, data);
        }

        void writeData(@NonNull byte[] data) {
            writeVariableLengthData(TYPE_DATA, data);
        }

        private void writeVariableLengthData(byte type, @NonNull byte[] data) {
            ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE * 2 + 1);
            buffer.putInt(MAGIC);
            buffer.put(type);
            buffer.putInt(data.length);
            buffer.rewind();
            ByteBuffer buffer2 = ByteBuffer.wrap(data);
            synchronized (this) {
                toSend.offer(buffer);
                toSend.offer(buffer2);
            }
        }

        /**
         * Try to send keepalive packet.
         *
         * @return # of bytes to send. < 0 if the socket needs to be closed.
         */
        int tryKeepAlive() {
            // if not connected, do nothing
            if (connectedTime == 0) {
                return 0;
            }
            // check if name is not received in a period of time, close the socket! and return false.
            long now = System.currentTimeMillis();
            if (name == null && now - connectedTime > GET_NAME_TIMEOUT_MS) {
                Log.i(TAG, "Cannot get the name of the other side. close the socket: %s", socketChannel);
                return -1;
            }

            // Didn't receive anything from the other side for too long, discard the connection, maybe the connection is down.
            if (now - lastReadTime > KEEP_ALIVE_TIMEOUT_MS) {
                Log.i(TAG, "Didn't receive data from socket %s for %dms, close!", socketChannel, KEEP_ALIVE_TIMEOUT_MS);
                return -1;
            }
            // If I have already written something in a period of time, don't have to send keep-alive.
            if (now - lastWriteTime < KEEP_ALIVE_DURATION_MS)
                return 0;
            // If we still have data to write, we don't have to send yet another keep-alive message
            synchronized (this) {
                if (!toSend.isEmpty()) {
                    return 0;
                }
            }
            ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE + 1);
            buffer.putInt(MAGIC);
            buffer.put(TYPE_KEEP_ALIVE);
            buffer.rewind();
            synchronized (this) {
                toSend.offer(buffer);
            }
            return buffer.remaining();
        }

        /**
         * Performs write.
         *
         * @return true if no data pending.
         * @throws IOException If failed in writing in the socket.
         */
        boolean performWrite() throws IOException {
//            Log.d(TAG, "performWrite!");
            synchronized (this) {
                if (toSend.isEmpty()) {
                    return true;
                }
                lastWriteTime = System.currentTimeMillis();
                while (!toSend.isEmpty()) {
                    ByteBuffer buffer = toSend.peek();
                    socketChannel.write(buffer);
                    if (buffer.remaining() > 0) {
                        return false;
                    }
                    toSend.poll();
                }

            }
            return true;
        }

        /**
         * Buffers some data into the read buffer.
         *
         * @param buffer  The buffer to be added.
         * @param numRead The number of bytes in the buffer available.
         * @return True if read is OK. On false, the manager should close the
         * socket.
         */
        boolean bufferBytes(@NonNull byte[] buffer, int numRead) {
            if (numRead == 0) {
                return true;
            }
            lastReadTime = System.currentTimeMillis();
            int start = 0, remaining = numRead;
            for (; ; ) {
                if (remaining == 0) {
                    return true;
                }
                switch (state) {
                    case STATE_READ_NAME_MAGIC_TYPE: {
                        int require = headerBuffer.remaining();
                        // not enough bytes, read and keep buffer
                        if (require > remaining) {
                            headerBuffer.put(buffer, start, remaining);
                            return true;
                        }
                        // have enough bytes, read magic and type
                        headerBuffer.put(buffer, start, require);
                        start += require;
                        remaining -= require;
                        headerBuffer.rewind();
                        int magic = headerBuffer.getInt();
                        // first 4 bytes has to be MAGIC
                        if (magic != MAGIC) {
                            return false;
                        }
                        Log.v(TAG, "Got MAGIC!");
                        byte type = headerBuffer.get();
                        // type has to be name
                        if (type != TYPE_NAME) {
                            return false;
                        }
                        Log.v(TAG, "type is NAME!");
                        state = STATE_READ_NAME_LENGTH;
                        headerBuffer.rewind();
                        continue;
                    }
                    case STATE_READ_NAME_LENGTH: {
                        int require = sizeBuffer.remaining();
                        if (require > remaining) {
                            sizeBuffer.put(buffer, start, remaining);
                            return true;
                        }
                        // have enough bytes, read magic and type
                        sizeBuffer.put(buffer, start, require);
                        start += require;
                        remaining -= require;
                        sizeBuffer.rewind();
                        int size = sizeBuffer.getInt();
                        sizeBuffer.rewind();
                        Log.v(TAG, "size=%d", size);
                        contentBuffer = ByteBuffer.allocate(size);
                        state = STATE_READ_NAME_CONTENT;
                        continue;
                    }
                    case STATE_READ_NAME_CONTENT: {
                        int require = contentBuffer.remaining();
                        if (require > remaining) {
                            contentBuffer.put(buffer, start, remaining);
                            return true;
                        }
                        contentBuffer.put(buffer, start, require);
                        start += require;
                        remaining -= require;
                        contentBuffer.rewind();
                        name = new String(contentBuffer.array(), DEFAULT_CHARSET);
                        Log.v(TAG, "name=%s", name);
                        if (socketChannelEventHandler != null)
                            socketChannelEventHandler.onSocketChannelNameReceived(socketChannel, name);
                        state = STATE_READ_MAGIC_TYPE;
                        continue;
                    }
                    case STATE_READ_MAGIC_TYPE: {
                        int require = headerBuffer.remaining();
                        // not enough bytes, read and keep buffer
                        if (require > remaining) {
                            headerBuffer.put(buffer, start, remaining);
                            return true;
                        }
                        // have enough bytes, read magic and type
                        headerBuffer.put(buffer, start, require);
                        start += require;
                        remaining -= require;
                        headerBuffer.rewind();
                        int magic = headerBuffer.getInt();
                        // first 4 bytes has to be MAGIC
                        if (magic != MAGIC) {
                            return false;
                        }
                        Log.v(TAG, "Got MAGIC!");
                        byte type = headerBuffer.get();
                        headerBuffer.rewind();
                        switch (type) {
                            case TYPE_DATA:
                                // should now read data length
                                state = STATE_READ_LENGTH;
                                Log.v(TAG, "type is DATA!");
                                continue;
                            case TYPE_KEEP_ALIVE:
                                Log.v(TAG, "type is KEEP_ALIVE!");
                                state = STATE_READ_MAGIC_TYPE;
                                continue;
                                // case TYPE_NAME:
                            default:
                                // Should not do type_name or have an unknown type! close socket
                                return false;
                        }
                    }
                    case STATE_READ_LENGTH: {
                        int require = sizeBuffer.remaining();
                        if (require > remaining) {
                            sizeBuffer.put(buffer, start, remaining);
                            return true;
                        }
                        // have enough bytes, read magic and type
                        sizeBuffer.put(buffer, start, require);
                        start += require;
                        remaining -= require;
                        sizeBuffer.rewind();
                        int size = sizeBuffer.getInt();
                        sizeBuffer.rewind();
                        Log.v(TAG, "size=%d", size);
                        contentBuffer = ByteBuffer.allocate(size);
                        state = STATE_READ_PAYLOAD;
                        continue;
                    }
                    case STATE_READ_PAYLOAD: {
                        int require = contentBuffer.remaining();
                        if (require > remaining) {
                            contentBuffer.put(buffer, start, remaining);
                            return true;
                        }
                        contentBuffer.put(buffer, start, require);
                        start += require;
                        remaining -= require;
                        contentBuffer.rewind();
                        Log.v(TAG, "finished reading bytes length=%d", contentBuffer.remaining());
                        if (socketChannelEventHandler != null)
                            socketChannelEventHandler.onSocketChannelDataReceived(socketChannel, contentBuffer.array());
                        state = STATE_READ_MAGIC_TYPE;
                        continue;
                    }
                    default:
                        return false;

                }
            }
        }
    }
}

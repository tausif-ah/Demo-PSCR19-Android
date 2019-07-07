package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public abstract class SocketWrapper implements AutoCloseable, Closeable {
    private static final int DEFAULT_READ_BUFFER_SIZE = 8192;
    private static final String TAG = "SocketWrapper";
    private static final int MAGIC = 0xdeadbeef;
    private static final byte TYPE_NAME = 1;
    private static final byte TYPE_KEEP_ALIVE = 2;
    private static final byte TYPE_DATA = 3;
    private static final long KEEP_ALIVE_DURATION_MS = 2000;
    private static final long KEEP_ALIVE_TIMEOUT_MS = 10000;
    private static final long GET_NAME_TIMEOUT_MS = 5000;
    private static final int STATE_READ_NAME_MAGIC_TYPE = 0;
    private static final int STATE_READ_NAME_LENGTH = 1;
    private static final int STATE_READ_NAME_CONTENT = 2;
    private static final int STATE_READ_MAGIC_TYPE = 3;
    private static final int STATE_READ_LENGTH = 4;
    private static final int STATE_READ_PAYLOAD = 5;

    @NonNull
    protected abstract InputStream getInputStream() throws IOException;

    @NonNull
    protected abstract OutputStream getOutputStream() throws IOException;

    protected abstract boolean isConnected();

    protected abstract void innerClose() throws IOException;

//    boolean isClosed() {
//        return !isConnected();
//    }

    private final ThreadTCPConnectionManager.SocketWrapperEventHandler socketWrapperEventHandler;
    private Handler sendThreadHandler;

    SocketWrapper(ThreadTCPConnectionManager.SocketWrapperEventHandler socketWrapperEventHandler) {
        this.socketWrapperEventHandler = socketWrapperEventHandler;
    }

    @Override
    public void close() {
        try {
            innerClose();
            socketWrapperEventHandler.onSocketWrapperClosed(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed in closing socket: %s", this);
            socketWrapperEventHandler.onSocketWrapperCloseFailed(this);
        }
    }

    void start() {
        lastReadTime = lastWriteTime = System.currentTimeMillis();
        Thread listenThread = new Thread(this::listenThread);
        listenThread.setDaemon(true);
        listenThread.start();
        Thread sendThread = new Thread(this::sendThread);
        sendThread.setDaemon(true);
        sendThread.start();
        while (sendThreadHandler == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        writeName();
        nameTimeoutTime = System.currentTimeMillis() + GET_NAME_TIMEOUT_MS;
        checkWorker();
    }

    private final ByteBuffer headerBuffer = ByteBuffer.allocate(Helper.INTEGER_SIZE + 1);
    private final ByteBuffer sizeBuffer = ByteBuffer.allocate(Helper.INTEGER_SIZE);
    private ByteBuffer contentBuffer;
    private final ArrayList<byte[]> toSend = new ArrayList<>();
    private long lastWriteTime = Long.MIN_VALUE;
    private long lastReadTime = Long.MIN_VALUE;
    private long nameTimeoutTime;
    private String name;
    private int state = STATE_READ_NAME_MAGIC_TYPE;

    void write(byte[] data) {
        int headerSize = Helper.INTEGER_SIZE * 2 + 1; // magic, type, length
        ByteBuffer header = ByteBuffer.allocate(headerSize);
        header.putInt(MAGIC);
        header.put(TYPE_DATA);
        header.putInt(data.length);
        queueData(new byte[][]{header.array(), data});
    }

    private void writeName() {
        byte[] nameData = Device.getName().getBytes(Helper.DEFAULT_CHARSET);
        int headerSize = Helper.INTEGER_SIZE * 2 + 1; // magic, type, length
        ByteBuffer header = ByteBuffer.allocate(headerSize);
        header.putInt(MAGIC);
        header.put(TYPE_NAME);
        header.putInt(nameData.length);
        queueData(new byte[][]{header.array(), nameData});
    }


    private void writeKeepAlive() {
        ByteBuffer header = ByteBuffer.allocate(Helper.INTEGER_SIZE + 1); // magic, type
        header.putInt(MAGIC);
        header.put(TYPE_KEEP_ALIVE);
        queueData(new byte[][]{header.array()});
    }

    private void queueData(byte[][] toQueues) {
        boolean needStartSendWorker;
        synchronized (toSend) {
            needStartSendWorker = toSend.isEmpty();
            toSend.addAll(Arrays.asList(toQueues));
        }
        if (needStartSendWorker) sendThreadHandler.post(this::sendWorker);
    }

    private void checkWorker() {
        if (!isConnected()) return;
        long nextEventTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        // no name exchanged yet
        if (name == null) {
            if (now > nameTimeoutTime) {
                Log.i(TAG, "Cannot get the name of the other side. close the socket: %s", this);
                close();
                return;
            }
            Log.d(TAG, "Next timeout time = nameTimeoutTime (%d)", nameTimeoutTime);
            nextEventTime = nameTimeoutTime;
        }
        if (now >= lastReadTime + KEEP_ALIVE_TIMEOUT_MS) {
            Log.i(TAG, "Didn't receive data from socket %s for %dms, close!", this, KEEP_ALIVE_TIMEOUT_MS);
            close();
            return;
        }
        nextEventTime = Math.min(nextEventTime, lastReadTime + KEEP_ALIVE_TIMEOUT_MS);

        synchronized (toSend) {
            // if the sender is sending, wait for a keepalive duration
            Log.i(TAG, "toSend empty: %b, %d, %d", toSend.isEmpty(), now + KEEP_ALIVE_DURATION_MS, lastWriteTime + KEEP_ALIVE_DURATION_MS);
            if (!toSend.isEmpty())
                nextEventTime = Math.min(nextEventTime, now + KEEP_ALIVE_DURATION_MS);
            else {
                if (now >= lastWriteTime + KEEP_ALIVE_DURATION_MS) { // keep alive due
                    writeKeepAlive();
                    nextEventTime = Math.min(nextEventTime, now + KEEP_ALIVE_DURATION_MS);
                } else {
                    nextEventTime = Math.min(nextEventTime, lastWriteTime + KEEP_ALIVE_DURATION_MS);
                }
            }
        }
        Log.d(TAG, "nextEventTime=%d", nextEventTime);
        sendThreadHandler.postDelayed(this::checkWorker, nextEventTime - now);
    }

    private void sendWorker() {
        // do not try to send if it is closed.
        if (!isConnected()) return;
        OutputStream outputStream;
        try {
            outputStream = getOutputStream();
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, e, "Failed in getting output stream, discard all pending data. %s", this);
            synchronized (toSend) {
                toSend.clear();
            }
            if (isConnected()) close();
            return;
        }
        // send the first one , remove it from the tosend list after send finishes
        byte[] toSendChunk;
        while (isConnected()) {
            synchronized (toSend) {
                if (toSend.isEmpty()) break;
                toSendChunk = toSend.get(0);
            }
            boolean sent = false;
            try {
                outputStream.write(toSendChunk);
                outputStream.flush();
                sent = true;
            } catch (IOException e) {
                Log.e(TAG, e, "Failed in writing chunk, discard. %s", this);
            }
            synchronized (toSend) {
                toSend.remove(0);
                if (sent) lastWriteTime = System.currentTimeMillis();
                if (toSend.isEmpty()) break;
            }
        }
    }

    private void listenThread() {
        InputStream inputStream;
        try {
            inputStream = getInputStream();
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, e, "Failed in getting inputStream, %s", this);
            if (isConnected()) close();
            return;
        }
        byte[] buf = new byte[DEFAULT_READ_BUFFER_SIZE];
        int read;
        while (isConnected()) {
            try {
                read = inputStream.read(buf);
                if (read < 0 && isConnected()) {
                    close();
                    break;
                }
                if (!bufferBytes(buf, read) && isConnected()) {
                    close();
                    break;
                }
            } catch (IOException e) {
                Log.e(TAG, e, "Failed in reading from inputStream, %s", this);
                if (isConnected()) close();
                break;
            }
        }
    }

    /**
     * Buffers some data into the read buffer.
     *
     * @param buffer  The buffer to be added.
     * @param numRead The number of bytes in the buffer available.
     * @return True if read is OK. On false, the manager should close the
     * socket.
     */
    private boolean bufferBytes(@NonNull byte[] buffer, int numRead) {
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
                        Log.e(TAG, "magic=0x%x, wrong state=STATE_READ_NAME_MAGIC_TYPE", magic);
                        return false;
                    }
                    // Log.v(TAG, "Got MAGIC!");
                    byte type = headerBuffer.get();
                    // type has to be name
                    if (type != TYPE_NAME) {
                        Log.e(TAG, "type=%d != TYPE_NAME, state=STATE_READ_NAME_MAGIC_TYPE", type);
                        return false;
                    }
                    // Log.v(TAG, "type is NAME!");
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
                    // Log.v(TAG, "size=%d", size);
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
                    name = new String(contentBuffer.array(), Helper.DEFAULT_CHARSET);
                    Log.v(TAG, "name=%s", name);
                    socketWrapperEventHandler.onSocketWrapperNameReceived(this, name);
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
                        Log.e(TAG, "magic=0x%x, wrong state=STATE_READ_MAGIC_TYPE", state);
                        return false;
                    }
                    // Log.v(TAG, "Got MAGIC!");
                    byte type = headerBuffer.get();
                    headerBuffer.rewind();
                    switch (type) {
                        case TYPE_DATA:
                            // should now read data length
                            state = STATE_READ_LENGTH;
                            //Log.v(TAG, "type is DATA!");
                            continue;
                        case TYPE_KEEP_ALIVE:
                            Log.v(TAG, "got KEEP_ALIVE!");
                            state = STATE_READ_MAGIC_TYPE;
                            continue;
                            // case TYPE_NAME:
                        default:
                            Log.e(TAG, "error type: %d state=%d", state);
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
                    //Log.v(TAG, "size=%d", size);
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
//                    Log.v(TAG, "finished reading buffer: ", contentBuffer.array());
                    Log.v(TAG, "finished reading bytes length=%d", contentBuffer.remaining());
                    socketWrapperEventHandler.onSocketWrapperDataReceived(this, contentBuffer.array());
                    state = STATE_READ_MAGIC_TYPE;
                    continue;
                }
                default:
                    Log.e(TAG, "wrong state=%d", state);

                    return false;

            }
        }
    }

    private void sendThread() {
        Looper.prepare();
        sendThreadHandler = new Handler();
        Looper.loop();
    }
}

class SocketWrapperTCP extends SocketWrapper {
    private final Socket socket;

    SocketWrapperTCP(InetAddress host, int port, ThreadTCPConnectionManager.SocketWrapperEventHandler socketWrapperEventHandler) throws IOException {
        this(new Socket(host, port), socketWrapperEventHandler);
    }

    SocketWrapperTCP(Socket socket, ThreadTCPConnectionManager.SocketWrapperEventHandler socketWrapperEventHandler) throws IOException {
        super(socketWrapperEventHandler);
        this.socket = socket;
        this.socket.setKeepAlive(true);
        this.socket.setTcpNoDelay(true);
    }

    @NonNull
    @Override
    protected InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @NonNull
    @Override
    protected OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    protected boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    protected void innerClose() throws IOException {
        socket.close();
    }

//    @NonNull
//    public Socket getSocket() {
//        return socket;
//    }

    @NonNull
    @Override
    public String toString() {
        return "SocketWrapperTCP{" + "socket=" + socket + '}';
    }
}

class SocketWrapperBluetooth extends SocketWrapper {
    private final BluetoothSocket socket;

    SocketWrapperBluetooth(BluetoothDevice device, UUID uuid, ThreadTCPConnectionManager.SocketWrapperEventHandler socketWrapperEventHandler) throws IOException {
        this(device.createRfcommSocketToServiceRecord(uuid), socketWrapperEventHandler);
    }

    SocketWrapperBluetooth(BluetoothSocket socket, ThreadTCPConnectionManager.SocketWrapperEventHandler socketWrapperEventHandler) {
        super(socketWrapperEventHandler);
        this.socket = socket;
    }

    @NonNull
    @Override
    protected InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @NonNull
    @Override
    protected OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    protected boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    protected void innerClose() throws IOException {
        socket.close();
    }

//    @NonNull
//    public BluetoothSocket getSocket() {
//        return socket;
//    }

    @NonNull
    @Override
    public String toString() {
        return "SocketWrapperBluetooth{" + "socket=" + socket + '}';
    }
}
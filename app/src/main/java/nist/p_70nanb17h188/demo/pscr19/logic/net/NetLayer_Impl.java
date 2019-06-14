package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.icu.text.DateIntervalFormat;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer_Impl;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class NetLayer_Impl {
    private static final int MAGIC = 0x12345678;
    public static final int MAX_SEND_SIZE = 2000000;
    public static final int MAX_SHOW_SIZE = 40;

    /**
     * The context for wifi NetLayer_Impl events.
     */
    public static final String CONTEXT_NET_LAYER_IMPL = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl";

    /**
     * Broadcast intent action indicating that a neighbor list has changed.
     * One extra {@link #EXTRA_NEIGHBORS} ({@link NeighborID}[]) indicates the neighbors that ar connected.
     * <p>
     * The values are also available with function {@link #getConnectNeighbors()} .
     */
    public static final String ACTION_NEIGHBOR_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl.neighborChanged";
    public static final String EXTRA_NEIGHBORS = "neighbors";
    public static final String TAG = "NetLayer_Impl";
    private static final int DIGEST_SIZE = 40;
    private static final byte TYPE_DATA = 1;
    private static final byte TYPE_SV1 = -128; // SV of what I have
    private static final byte TYPE_SV2 = -127; // SV of what I want
    private static final byte TYPE_M = -126; // Message Content
    private static final int BUFFER_CAPACITY = 6; // # of items

    private static final HashSet<NeighborID> connectedNeighbors = new HashSet<>();
    private final HashMap<Name, HashSet<DataReceivedHandler>> dataHandlers = new HashMap<>();

    private final static HashMap<String, byte []> messageBuffer = new HashMap<>();
    private final static Queue<String> messageBufferQueue = new LinkedList<>();
    private static HashSet<String> request = new HashSet<>();


    private static byte[] V1_toBytes() {
        int num_of_items = messageBuffer.size();
        ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE * 2 + 1 + num_of_items*DIGEST_SIZE);// MAGIC (int) - TYPE ()byte - #of items (int) - content
        buffer.putInt(MAGIC);
        buffer.put(TYPE_SV1);
        buffer.putInt(num_of_items);
        for(String key : messageBuffer.keySet()) {
            byte [] thisKey = key.getBytes();
            buffer.put(thisKey);
        }
        return buffer.array();
    }

    @NonNull
    private static byte[] V1_toBytes(@NonNull String key) {//single item sumamry vector
        int num_of_items = 1;
        ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE * 2 + 1 + DIGEST_SIZE);// MAGIC (int) - TYPE ()byte - #of items (int) - content
        buffer.putInt(MAGIC);
        buffer.put(TYPE_SV1);
        buffer.putInt(num_of_items);
        byte [] thisKey = key.getBytes();
        buffer.put(thisKey);
        return buffer.array();
    }

    @Nullable
    private static byte [] V_fromBytes(@NonNull byte[] data) {
        if (data.length < Helper.INTEGER_SIZE * 2 + 1) return null;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int magic_read;
        int type_read;
        if ((magic_read = buffer.getInt()) != MAGIC) return null;
        type_read = buffer.get();
        if ((type_read) != TYPE_SV1 && (type_read) != TYPE_SV2) return null;
        int numElements = buffer.getInt();
        int length = numElements*DIGEST_SIZE;
        if (length != buffer.remaining()) return null;
        byte[] content = new byte[length];
        buffer.get(content);
        if(type_read== TYPE_SV1)
            Log.v(NetLayer_Impl.TAG, "Read SV (what other has) --- magic: "+magic_read+" type: "+type_read+" num: "+numElements);
        if(type_read== TYPE_SV2)
            Log.v(NetLayer_Impl.TAG, "Read SV (what other wants) --- magic: "+magic_read+" type: "+type_read+" num: "+numElements);
        return content;
    }

    @NonNull
    private static byte[] V2_toBytes() {
        int num_of_items = request.size();
        ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE * 2 + 1 + num_of_items*DIGEST_SIZE);// MAGIC (int) - TYPE ()byte - #of items (int) - content
        buffer.putInt(MAGIC);
        buffer.put(TYPE_SV2);
        buffer.putInt(num_of_items);
        for(String key : request) {
            byte [] thisKey = key.getBytes();
            buffer.put(thisKey);
        }
        return buffer.array();
    }

    @Nullable
    private static byte[] M_toBytes(@NonNull String key) {
        byte [] msg = messageBuffer.get(key);
        if(msg==null)
            return null;
        int msgLength = msg.length;
        ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE * 2 + 1 + DIGEST_SIZE+ msg.length);// MAGIC (int) - TYPE ()byte - digest + ConLength - msgContent
        buffer.putInt(MAGIC);
        buffer.put(TYPE_M);
        byte [] digest = key.getBytes();
        buffer.put(digest);
        buffer.putInt(msgLength);
        buffer.put(msg);

        return buffer.array();
    }

    private static void M_fromBytes(@NonNull byte[] data, NeighborID neighbor) {
        if (data.length < Helper.INTEGER_SIZE * 2 + 1) return;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int magic_read;
        int type_read;
        if ((magic_read = buffer.getInt()) != MAGIC) return;
        if ((type_read = buffer.get()) != TYPE_M) return;
        byte [] digest = new byte[DIGEST_SIZE];
        buffer.get(digest);
        String digest_str = (new String(digest, StandardCharsets.US_ASCII));
        int length = buffer.getInt();
        if (length != buffer.remaining()) return;
        byte[] content = new byte[length];
        buffer.get(content);
        String calculatedDigest = getSHA(content);
        String content_str = (new String(content, StandardCharsets.US_ASCII));
        Log.v(NetLayer_Impl.TAG, "Read Content --- magic: "+magic_read+" type: "+type_read+ " \ndigest: "+digest_str+
                " \ncontent: "+ content_str+ " \ncalculated digest: "+calculatedDigest + " \ncompare digests: "+(digest_str.equals(calculatedDigest)));
        if(!digest_str.equals(calculatedDigest)){
            Log.v(NetLayer_Impl.TAG, "Calculated digest different from provided digest. Discarding!");
            return;
        }
        //add to buffer
        addBufferEntry(digest_str, content, neighbor);
    }

    NetLayer_Impl() {
        Context.getContext(LinkLayer.CONTEXT_LINK_LAYER).registerReceiver((context, intent) -> {
                    switch (intent.getAction()) {
                        case LinkLayer.ACTION_LINK_CHANGED:
                            onLinkChanged(intent);
                            break;
                        case LinkLayer.ACTION_DATA_RECEIVED:
                            onDataReceived(intent);
                            break;
                    }
                },
                new IntentFilter()
                        .addAction(LinkLayer.ACTION_LINK_CHANGED)
                        .addAction(LinkLayer.ACTION_DATA_RECEIVED)
        );
    }

    private void onLinkChanged(@NonNull Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        Boolean connected = intent.getExtra(LinkLayer.EXTRA_CONNECTED);
        assert neighborID != null && connected != null;
        boolean changed;
        if (connected) changed = connectedNeighbors.add(neighborID);
        else changed = connectedNeighbors.remove(neighborID);

        if (changed)
            Context.getContext(CONTEXT_NET_LAYER_IMPL).sendBroadcast(new Intent(ACTION_NEIGHBOR_CHANGED).putExtra(EXTRA_NEIGHBORS, connectedNeighbors.toArray(new NeighborID[0])));

        if(connected){
        //exchange summary vectors

            Log.v(NetLayer_Impl.TAG, "I have this message buffer to exchange with "+ neighborID.getName()+":\n"+printMessageBuffer());
            byte [] Data = V1_toBytes();

            LinkLayer.sendData(neighborID, Data, 0, Data.length);

        }
    }

    private void onDataReceived(@NonNull Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        byte[] data = intent.getExtra(LinkLayer.EXTRA_DATA);
        assert data != null;


        ByteBuffer buffer = ByteBuffer.wrap(data);
        Log.v(NetLayer_Impl.TAG, "received data with "+ data.length +" bytes from "+neighborID.getName());
        // read magic
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            return;
        }
        int magic = buffer.getInt();
        if (magic != MAGIC) return;
        // read type
        byte type = buffer.get();
        switch (type) {
            case TYPE_DATA:
                if (!readData(buffer))
                    return;
                break;
            case TYPE_SV1:
                buffer.rewind();
                //Log.v(NetLayer_Impl.TAG, "rcvd content "+V_fromBytes(data)+ " str:"+ Arrays.toString(V_fromBytes(data)));

                byte [] content_rcvd = V_fromBytes(data);
                if(content_rcvd==null)
                    return;
                request = new HashSet<>();
                int rcv_num = content_rcvd.length/DIGEST_SIZE;
                for(int i=0; i<rcv_num; i++){
                    byte [] thisContent = new byte[DIGEST_SIZE];
                    for(int j=0; j<DIGEST_SIZE; j++){
                        thisContent[j] = content_rcvd[i*DIGEST_SIZE + j];
                    }
                    String thisContentString = new String(thisContent, StandardCharsets.US_ASCII);
                    if(!messageBuffer.containsKey(thisContentString)) {
                        request.add(thisContentString);
                    }
                }
                byte [] wish = V2_toBytes();
                Log.v(NetLayer_Impl.TAG, "I need "+request+" ... send this request to "+neighborID.getName());
                LinkLayer.sendData(neighborID, wish, 0, wish.length);
                break;
            case TYPE_SV2:
                buffer.rewind();
                //Log.v(NetLayer_Impl.TAG, "rcvd content "+V_fromBytes(data)+ " str:"+ Arrays.toString(V_fromBytes(data)));
                byte [] content_rcvd2 = V_fromBytes(data);
                if(content_rcvd2==null)
                    return;
                HashSet <String> otherWants = new HashSet<>();
                int rcv_num2 = content_rcvd2.length/DIGEST_SIZE;
                for(int i=0; i<rcv_num2; i++){
                    byte [] thisContent = new byte[DIGEST_SIZE];
                    for(int j=0; j<DIGEST_SIZE; j++){
                        thisContent[j] = content_rcvd2[i*DIGEST_SIZE + j];
                    }
                    String thisContentString = new String(thisContent, StandardCharsets.US_ASCII);
                    otherWants.add(thisContentString);
                }
                Log.v(NetLayer_Impl.TAG, neighborID.getName()+" wants these from me!:\n"+otherWants);
                //iterate through the Queue; on any match with otherWants -> send to content other
                for(String element: messageBufferQueue){
                    if(otherWants.contains(element)){
                        byte [] msg_to_send = M_toBytes(element);
                        if(msg_to_send==null)
                            return;
                        Log.v(NetLayer_Impl.TAG, "send this content: "+(new String(messageBuffer.get(element), StandardCharsets.US_ASCII)));
                        LinkLayer.sendData(neighborID, msg_to_send, 0, msg_to_send.length);
                    }
                }
                break;
            case TYPE_M:
                //Log.v(NetLayer_Impl.TAG, "Data msg recvd");
                M_fromBytes(data, neighborID);
                //Log.v(NetLayer_Impl.TAG, " buffer : "+getMessageBuffer());
                break;
            default:
                return;
        }
    }


    public static void addBufferEntry(String key, byte [] value, @Nullable NeighborID src){
        if(messageBuffer.size()>=BUFFER_CAPACITY){
            String toRemove = messageBufferQueue.remove();
            messageBuffer.remove(toRemove);
            Log.v(NetLayer_Impl.TAG, "Buffer capacity exceeded! Item %s removed!", toRemove);
        }
        messageBuffer.put(key, value);
        if(!messageBufferQueue.contains(key))
            messageBufferQueue.add(key);
        //check if any neighbors
        if(connectedNeighbors.isEmpty()){
            Log.v(NetLayer_Impl.TAG, "no neighbors to send %s to now!", key);
        }
        else if(connectedNeighbors.size()==1 && connectedNeighbors.contains(src)){
            Log.v(NetLayer_Impl.TAG, "no OTHER neighbors to send %s to now!", key);
        }
        else {
            if(src!=null)
                Log.v(NetLayer_Impl.TAG, "need to send to these neighbors now! "+connectedNeighbors+ "except "+src.getName());
            else
                Log.v(NetLayer_Impl.TAG, "need to send to these neighbors now! "+connectedNeighbors);

            for (NeighborID neighbor : connectedNeighbors) {
                if(!neighbor.equals(src)) {
                    byte[] V1_to_send = V1_toBytes(key);
                    LinkLayer.sendData(neighbor, V1_to_send, 0, V1_to_send.length);
                }
            }
        }
        Log.v(NetLayer_Impl.TAG, "buffer at "+Device.getName()+" : "+printMessageBuffer());
    }

    @NonNull
    private static HashMap<String, byte []> getMessageBufferHashTable(){
        return messageBuffer;
    }

    @NonNull
    private static Queue<String> getMessageBufferQueu(){
        return messageBufferQueue;
    }

    @NonNull
    private static String printMessageBuffer(){
        StringBuilder resultString = new StringBuilder();
        resultString.append("Hash Table:\n");
        for(String key: messageBuffer.keySet()){
            resultString.append(key).append("\t").append(new String(messageBuffer.get(key), StandardCharsets.US_ASCII)).append("\n");
        }
        resultString.append("\nQueue:\n");
        for(String element: messageBufferQueue){
            resultString.append(element).append("\t").append(new String(messageBuffer.get(element), StandardCharsets.US_ASCII)).append("\n");
        }
        return resultString.toString();
    }

    @Nullable
    public static String getSHA(byte [] input)
    {

        try {

            // Static getInstance method is called with hashing SHA
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // digest() method called
            // to calculate message digest of an input
            // and return array of byte
            byte[] messageDigest = md.digest(input);

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            byte [] hashtext_bytes = hashtext.getBytes();
            int hashtext_bytes_length = hashtext_bytes.length;
            if(hashtext_bytes_length<DIGEST_SIZE){
                for(int i=hashtext_bytes_length; i<DIGEST_SIZE; i++){
                    hashtext= hashtext+ "0";
                }
            }
            Log.v(NetLayer_Impl.TAG , "Digest is: "+hashtext+ " numBytes: "+hashtext.getBytes().length);
            return hashtext;
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            System.out.println("Exception thrown"
                    + " for incorrect algorithm: " + e);

            return null;
        }
    }






















    public NeighborID[] getConnectNeighbors() {
        return connectedNeighbors.toArray(new NeighborID[0]);
    }


    private boolean readData(@NonNull ByteBuffer buffer) {
        Name src = Name.read(buffer);
        if (src == null) return false;

        Name dst = Name.read(buffer);
        if (dst == null) return false;

        if (buffer.remaining() < Helper.INTEGER_SIZE) return false;
        int len = buffer.getInt();

        if (buffer.remaining() != len) return false;
        byte[] data = new byte[len];
        buffer.get(data);

        synchronized (dataHandlers) {
            HashSet<DataReceivedHandler> handlers = dataHandlers.get(dst);
            if (handlers != null) {
                for (DataReceivedHandler h : handlers) {
                    h.dataReceived(src, dst, data);
                }
            }
        }
        return true;
    }

    boolean sendData(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, int start, int len) {
        if (len < 0 || start < 0 || start + len > data.length)
            throw new IllegalArgumentException("Error start, len value");
        int size = Helper.INTEGER_SIZE  // magic
                + 1                     // type
                + Name.WRITE_SIZE * 2   // src, dst
                + Helper.INTEGER_SIZE   // len
                + len;                  // data
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.putInt(MAGIC);
        buffer.put(TYPE_DATA);
        boolean result = src.write(buffer);
        if (!result) return false;
        result = dst.write(buffer);
        if (!result) return false;
        buffer.putInt(len);
        buffer.put(data, start, len);

        // also send the data to local subscribers
        onDataReceived(new Intent(LinkLayer.ACTION_DATA_RECEIVED).putExtra(LinkLayer.EXTRA_DATA, buffer.array()));

        return true;
    }


    boolean subscribe(Name n, DataReceivedHandler h) {
        synchronized (dataHandlers) {
            HashSet<DataReceivedHandler> handlers = dataHandlers.get(n);
            if (handlers == null) {
                dataHandlers.put(n, handlers = new HashSet<>());
            }
            return handlers.add(h);
        }
    }

    boolean unSubscribe(Name n, DataReceivedHandler h) {
        synchronized (dataHandlers) {
            HashSet<DataReceivedHandler> handlers = dataHandlers.get(n);
            if (handlers == null) return false;
            if (handlers.remove(h)) {
                if (handlers.isEmpty()) {
                    dataHandlers.remove(n);
                }
                return true;
            }
            return false;
        }
    }
}

package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.icu.text.DateIntervalFormat;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;
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

    public static final String CONTEXT_NET_LAYER_IMPL = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl";
    public static final String ACTION_NEIGHBOR_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl.neighborChanged";
    public static final String EXTRA_NEIGHBORS = "neighbors";
    public static final String TAG = "NetLayer_Impl";
    public static final int DIGEST_SIZE = 40;
    private static final byte TYPE_DATA = 1;
    private static final byte TYPE_SV1 = -128; // SV of what I have
    private static final byte TYPE_SV2 = -127; // SV of what I want
    private static final byte TYPE_M = -126; // Message


    private final HashSet<NeighborID> connectedNeighbors = new HashSet<>();
    private final HashMap<Name, HashSet<DataReceivedHandler>> dataHandlers = new HashMap<>();

    private final static HashMap<String, byte []> messageBuffer = new HashMap<>();
    private static HashSet<String> request = new HashSet<>();


    public static byte[] V1_toBytes() {
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

    @Nullable
    public static byte [] V_fromBytes(@NonNull byte[] data) {
        if (data.length < Helper.INTEGER_SIZE * 2 + 1) return null;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int magic_read =0;
        int type_read = 0;
        if ((magic_read = buffer.getInt()) != MAGIC) return null;
        type_read = buffer.get();
        if ((type_read) != TYPE_SV1 && (type_read) != TYPE_SV2) return null;
        int numElements = buffer.getInt();
        int length = numElements*DIGEST_SIZE;
        if (length != buffer.remaining()) return null;
        byte[] content = new byte[length];
        buffer.get(content);
        Log.e(NetLayer_Impl.TAG, "Read magic: "+magic_read+" type: "+type_read+" num: "+numElements+" content "+content);
        return content;
    }

    public static byte[] V2_toBytes() {
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

    public static byte[] M_toBytes(String key) {
        byte [] msg = messageBuffer.get(key);
        int msgLentgh = msg.length;
        ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE * 2 + 1 + DIGEST_SIZE+ msg.length);// MAGIC (int) - TYPE ()byte - digest + ConLength - msgContent
        buffer.putInt(MAGIC);
        buffer.put(TYPE_M);
        byte [] digest = key.getBytes();
        buffer.put(digest);
        buffer.putInt(msgLentgh);
        buffer.put(msg);

        return buffer.array();
    }

    @Nullable
    public static void M_fromBytes(@NonNull byte[] data) {
        if (data.length < Helper.INTEGER_SIZE * 2 + 1) return;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int magic_read =0;
        int type_read = 0;
        if ((magic_read = buffer.getInt()) != MAGIC) return;
        if ((type_read = buffer.get()) != TYPE_M) return;
        byte [] digest = new byte[DIGEST_SIZE];
        buffer.get(digest);
        String digest_str = (new String(digest, StandardCharsets.US_ASCII));
        int length = buffer.getInt();
        if (length != buffer.remaining()) return;
        byte[] content = new byte[length];
        buffer.get(content);
        String content_str = (new String(content, StandardCharsets.US_ASCII));
        Log.e(NetLayer_Impl.TAG, "Read magic: "+magic_read+" type: "+type_read+ "digest_str:"+digest_str+" len: "+length+
                " content "+content+ " content str"+ content_str);
        //add to buffer
        addBufferEntry(digest_str, content);
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
            String thisName = Device.getName();
            String theirName = neighborID.getName();
            //if(!messageBuffer.isEmpty()){

                Log.e(NetLayer_Impl.TAG, "I have ["+messageBuffer+"] to exchange with "+ neighborID.getName());
                byte [] Data = V1_toBytes();
                //String summaryvector = TYPE_SV1+" ";
                //for(String messageKey: messageBuffer.keySet()) {
                //summaryvector = summaryvector + messageKey + " ";
                //}
                //Log.e(NetLayer_Impl.TAG, "I have summary vector ["+summaryvector+"] to exchange with "+ neighborID.getName());
                //byte [] Data = summaryvector.getBytes();
                Log.e(NetLayer_Impl.TAG, "SV to send: "+ Data);

                LinkLayer.sendData(neighborID, Data, 0, Data.length);

                //Log.e(NetLayer_Impl.TAG, "check: "+V_fromBytes(Data));


            //}
        }
    }

    private void onDataReceived(@NonNull Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        byte[] data = intent.getExtra(LinkLayer.EXTRA_DATA);
        assert data != null;


        ByteBuffer buffer = ByteBuffer.wrap(data);
        Log.e(NetLayer_Impl.TAG, "received ["+ data+ "] with "+ data.length +" bytes from "+neighborID.getName());
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
                Log.e(NetLayer_Impl.TAG, "rcvd content "+V_fromBytes(data)+ " str:"+ Arrays.toString(V_fromBytes(data)));

                byte [] content_rcvd = V_fromBytes(data);

                request = new HashSet<>();
                int rcv_num = content_rcvd.length/DIGEST_SIZE;
                for(int i=0; i<rcv_num; i++){
                    byte [] thisContent = new byte[DIGEST_SIZE];
                    for(int j=0; j<DIGEST_SIZE; j++){
                        thisContent[j] = content_rcvd[i*DIGEST_SIZE + j];
                    }
                    String thisContentString = new String(thisContent, StandardCharsets.US_ASCII);
                    // Arrays.toString(thisContent);
                    if(!messageBuffer.containsKey(thisContentString)) {
                        request.add(thisContentString);
                    }
                }
                byte [] wish = V2_toBytes();
                Log.e(NetLayer_Impl.TAG, "I need "+request + "to send: "+wish);
                LinkLayer.sendData(neighborID, wish, 0, wish.length);
                break;
            case TYPE_SV2:
                buffer.rewind();
                Log.e(NetLayer_Impl.TAG, "rcvd content "+V_fromBytes(data)+ " str:"+ Arrays.toString(V_fromBytes(data)));

                byte [] content_rcvd2 = V_fromBytes(data);

                int rcv_num2 = content_rcvd2.length/DIGEST_SIZE;
                for(int i=0; i<rcv_num2; i++){
                    byte [] thisContent = new byte[DIGEST_SIZE];
                    for(int j=0; j<DIGEST_SIZE; j++){
                        thisContent[j] = content_rcvd2[i*DIGEST_SIZE + j];
                    }
                    String thisContentString = new String(thisContent, StandardCharsets.US_ASCII);
                    Log.e(NetLayer_Impl.TAG, neighborID.getName()+" wants "+thisContentString+" from me!");
                    byte [] msg_to_send = M_toBytes(thisContentString);
                    LinkLayer.sendData(neighborID, msg_to_send, 0, msg_to_send.length);
                }
                break;
            case TYPE_M:
                Log.e(NetLayer_Impl.TAG, "Data msg recvd");
                M_fromBytes(data);
                Log.e(NetLayer_Impl.TAG, " buffer : "+getMessageBuffer());
                break;
            default:
                return;
        }

        // Dummy implementation, send it to all neighbors except the "from" interface
        // but forward only valid net layer packets
        for (NeighborID neighbor : connectedNeighbors) {
            if (!neighbor.equals(neighborID))
                LinkLayer.sendData(neighbor, data, 0, data.length);
        }

    }




    public static void addBufferEntry(String key, byte [] value){
        messageBuffer.put(key, value);
    }

    public static HashMap<String, byte []> getMessageBuffer(){
        return messageBuffer;
    }

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

            Log.e(NetLayer_Impl.TAG , "Digest of "+input+" is "+hashtext+ " numBytes: "+hashtext.getBytes().length+ " (md:)"+ messageDigest.length);
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

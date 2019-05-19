package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;

import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

public class NetLayer_Impl {
    public static final int MAX_SEND_SIZE = 2000000;
    public static final int MAX_SHOW_SIZE = 40;

    public static final String CONTEXT_NET_LAYER_IMPL = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl";
    public static final String ACTION_NEIGHBOR_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl.neighborChanged";
    public static final String EXTRA_NEIGHBORS = "neighbors";
    public static final String TAG = "NetLayer_Impl";

    private static NetLayer_Impl defaultInstance;
    @NonNull
    private final HashSet<NeighborID> connectedNeighbors = new HashSet<>();

    NetLayer_Impl() {
        defaultInstance = this;

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

    public static NetLayer_Impl getDefaultInstance() {
        return defaultInstance;
    }

    private void onLinkChanged(Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        Boolean connected = intent.getExtra(LinkLayer.EXTRA_CONNECTED);
        assert neighborID != null && connected != null;
        boolean changed;
        if (connected) changed = connectedNeighbors.add(neighborID);
        else changed = connectedNeighbors.remove(neighborID);

        if (changed)
            Context.getContext(CONTEXT_NET_LAYER_IMPL).sendBroadcast(new Intent(ACTION_NEIGHBOR_CHANGED).putExtra(EXTRA_NEIGHBORS, connectedNeighbors.toArray(new NeighborID[0])));
    }

    public NeighborID[] getConnectNeighbors() {
        return connectedNeighbors.toArray(new NeighborID[0]);
    }

    private void onDataReceived(Intent intent) {
        NeighborID neighborID = intent.getExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        byte[] data = intent.getExtra(LinkLayer.EXTRA_DATA);
        assert neighborID != null && data != null;

        String str = new String(data);
        if (data.length <= MAX_SHOW_SIZE) {
            Log.d(TAG, "Got from %s, buf_size=%d, text=%n%s", neighborID.getName(), data.length, str);
            Helper.notifyUser(LogType.Info, "Got from %s, buf_size=%d, text=%n%s", neighborID.getName(), data.length, str);
        } else {
            Log.d(TAG, "Got from %s, buf_size=%d, text_len=%d", neighborID.getName(), data.length, str.length());
            Helper.notifyUser(LogType.Info, "Got from %s, buf_size=%d, text_len=%d", neighborID.getName(), data.length, str.length());
        }
    }


}

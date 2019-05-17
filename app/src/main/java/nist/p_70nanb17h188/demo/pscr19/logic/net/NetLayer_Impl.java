package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;

public class NetLayer_Impl {
    public static final String ACTION_NEIGHBOR_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.neighborChanged";

    private static NetLayer_Impl defaultInstance;
    @NonNull
    private final Application application;
    private final HashSet<NeighborID> connectedNeighbors = new HashSet<>();

    NetLayer_Impl(@NonNull Application application) {
        this.application = application;
        defaultInstance = this;

        Context context = application.getApplicationContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction(LinkLayer.ACTION_LINK_CHANGED);
        filter.addAction(LinkLayer.ACTION_DATA_RECEIVED);
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == null) return;
                switch (intent.getAction()) {
                    case LinkLayer.ACTION_LINK_CHANGED:
                        onLinkChanged(intent);
                        break;
                    case LinkLayer.ACTION_DATA_RECEIVED:
                        onDataReceived(intent);
                        break;
                }
            }
        }, filter);
    }

    public static NetLayer_Impl getDefaultInstance() {
        return defaultInstance;
    }

    private void onLinkChanged(Intent intent) {
        NeighborID neighborID = intent.getParcelableExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        boolean connected = intent.getBooleanExtra(LinkLayer.EXTRA_CONNECTED, false);
        boolean changed;
        if (connected) changed = connectedNeighbors.add(neighborID);
        else changed = connectedNeighbors.remove(neighborID);
        if (changed)
            application.getApplicationContext().sendBroadcast(new Intent(ACTION_NEIGHBOR_CHANGED));
    }

    public NeighborID[] getConnectNeighbors() {
        return connectedNeighbors.toArray(new NeighborID[0]);
    }

    private void onDataReceived(Intent intent) {
        NeighborID neighborID = intent.getParcelableExtra(LinkLayer.EXTRA_NEIGHBOR_ID);
        byte[] data = intent.getByteArrayExtra(LinkLayer.EXTRA_DATA);
        String str = new String(data);
        if (data.length <= 40) {
            Toast.makeText(application.getApplicationContext(), String.format(Locale.US, "Got from %s, text=%s, buf_size=%d", neighborID.name, str, data.length), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(application.getApplicationContext(), String.format(Locale.US, "Got from %s, text_len=%d, buf_size=%d", neighborID.name, str.length(), data.length), Toast.LENGTH_LONG).show();
        }
    }


}

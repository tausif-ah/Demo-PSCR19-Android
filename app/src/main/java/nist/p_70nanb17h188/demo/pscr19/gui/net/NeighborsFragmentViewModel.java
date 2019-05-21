package nist.p_70nanb17h188.demo.pscr19.gui.net;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl;

public class NeighborsFragmentViewModel extends ViewModel {
    final MutableLiveData<NeighborID[]> connectedNeighbors = new MutableLiveData<>();
    private final BroadcastReceiver broadcastReceiver = (context, intent) -> {
        if (!NetLayer_Impl.ACTION_NEIGHBOR_CHANGED.equals(intent.getAction()))
            return;
        NeighborID[] neighbors = intent.getExtra(NetLayer_Impl.EXTRA_NEIGHBORS);
        assert neighbors != null;
        connectedNeighbors.postValue(neighbors);
    };

    public NeighborsFragmentViewModel() {
        connectedNeighbors.setValue(new NeighborID[0]);
        Context.getContext(NetLayer_Impl.CONTEXT_NET_LAYER_IMPL).registerReceiver(
                broadcastReceiver,
                new IntentFilter()
                        .addAction(NetLayer_Impl.ACTION_NEIGHBOR_CHANGED)
        );
        synchronizeData();
    }

    private void synchronizeData() {
        connectedNeighbors.postValue(NetLayer_Impl.getDefaultInstance().getConnectNeighbors());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Context.getContext(NetLayer_Impl.CONTEXT_NET_LAYER_IMPL).unregisterReceiver(broadcastReceiver);
    }
}

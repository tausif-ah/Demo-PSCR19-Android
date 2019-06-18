package nist.p_70nanb17h188.demo.pscr19.gui.net;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;

import java.util.Arrays;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.net.GossipModule;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

public class GossipFragmentViewModel extends ViewModel {
    private final MutableLiveData<Boolean> fixed = new MutableLiveData<>();
    private final MutableLiveData<Boolean> store = new MutableLiveData<>();
    private final MutableLiveData<String> connectedNeighbors = new MutableLiveData<>();


    private final BroadcastReceiver gossipReceiver = (context, intent) -> {
        if (GossipModule.ACTION_NEIGHBOR_CHANGED.equals(intent.getAction()))
            setConnectedNeighbors();
    };


    public GossipFragmentViewModel() {
        fixed.setValue(true);
        store.setValue(true);
        setConnectedNeighbors();
        Context.getContext(GossipModule.CONTEXT_GOSSIP_MODULE).registerReceiver(gossipReceiver,
                new IntentFilter().addAction(GossipModule.ACTION_NEIGHBOR_CHANGED));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Context.getContext(GossipModule.CONTEXT_GOSSIP_MODULE).unregisterReceiver(gossipReceiver);
    }

    @NonNull
    LiveData<Boolean> getFixed() {
        return fixed;
    }

    @NonNull
    LiveData<Boolean> getStore() {
        return store;
    }

    @NonNull
    LiveData<String> getConnectedNeighbors() {
        return connectedNeighbors;
    }

    void setFixed(boolean fixed) {
        Boolean orig = getFixed().getValue();
        assert orig != null;
        if (orig == fixed) return;
        this.fixed.postValue(fixed);


    }

    void setStore(boolean store) {
        Boolean orig = getStore().getValue();
        assert orig != null;
        if (orig == store) return;
        this.store.postValue(store);
    }

    void addClicked() {
        Boolean fixed = getFixed().getValue();
        Boolean store = getStore().getValue();
        assert fixed != null && store != null;
        NetLayer.getDefaultInstance().getGossipModule().addMessage(
                fixed ? "Hello World!".getBytes() : Helper.getRandomString(10, 20, Helper.CANDIDATE_CHARSET_LETTERS_NUMBERS).getBytes(),
                store
        );

    }

    private void setConnectedNeighbors() {
        connectedNeighbors.postValue(String.format("Neighbors: %s", Arrays.toString(NetLayer.getDefaultInstance().getGossipModule().getConnectNeighbors())));
    }
}

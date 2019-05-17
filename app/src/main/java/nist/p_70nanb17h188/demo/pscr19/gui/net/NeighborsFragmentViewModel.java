package nist.p_70nanb17h188.demo.pscr19.gui.net;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl;

class NeighborsFragmentViewModel extends ViewModel {
    final MutableLiveData<NeighborID[]> connectedNeighbors = new MutableLiveData<>();
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !NetLayer_Impl.ACTION_NEIGHBOR_CHANGED.equals(intent.getAction()))
                return;
            synchronizeData();
        }
    };
    private Application application;

    public NeighborsFragmentViewModel() {
        connectedNeighbors.setValue(new NeighborID[0]);
    }

    void setApplication(@NonNull Application application) {
        synchronized (this) {
            if (this.application != null) return;
            this.application = application;
            synchronizeData();

            IntentFilter filter = new IntentFilter();
            filter.addAction(NetLayer_Impl.ACTION_NEIGHBOR_CHANGED);
            application.getApplicationContext().registerReceiver(broadcastReceiver, filter);
        }
    }

    private void synchronizeData() {
        connectedNeighbors.postValue(NetLayer_Impl.getDefaultInstance().getConnectNeighbors());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        application.getApplicationContext().unregisterReceiver(broadcastReceiver);
    }
}

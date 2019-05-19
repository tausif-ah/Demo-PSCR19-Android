package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

//import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

abstract class Link {
    @NonNull
    public final String name;
    final MutableLiveData<LinkStatus> status = new MutableLiveData<>();
    final MutableLiveData<Boolean> establishConnection = new MutableLiveData<>();
    private boolean tcpConnected;

    Link(@NonNull String name) {
        this.name = name;
        this.establishConnection.postValue(false);
        this.status.postValue(LinkStatus.NotFound);
    }

//    boolean isEstablishConnection() {
//        Boolean ret = establishConnection.getValue();
//        assert ret != null;
//        return ret;
//    }

    boolean isTcpConnected() {
        return tcpConnected;
    }

    LiveData<Boolean> getEstablishConnection() {
        return establishConnection;
    }

    LiveData<LinkStatus> getStatus() {
        return status;
    }

    void setTCPConnected(boolean tcpConnected) {
        this.tcpConnected = tcpConnected;
//        Log.d("Link", "setTCPConnected %s %b, %b", name, tcpConnected, this.tcpConnected);
    }

    abstract void onEstablishConnectionClick();

    public enum LinkStatus {
        NotFound, NotConnected, Invited, Connected, TCPEstablished
    }
}

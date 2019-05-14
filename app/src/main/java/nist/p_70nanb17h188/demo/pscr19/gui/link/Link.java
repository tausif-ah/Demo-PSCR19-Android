package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

abstract class Link {
    public final String name;
    final MutableLiveData<LinkStatus> status = new MutableLiveData<>();
    final MutableLiveData<Boolean> establishConnection = new MutableLiveData<>();

    Link(String name) {
        this.name = name;
        this.establishConnection.setValue(false);
        this.status.setValue(LinkStatus.NotFound);
    }

    boolean isEstablishConnection() {
        Boolean ret = establishConnection.getValue();
        assert ret != null;
        return ret;
    }

    LiveData<Boolean> getEstablishConnection() {
        return establishConnection;
    }

    LiveData<LinkStatus> getStatus() {
        return status;
    }

    abstract void onEstablishConnectionClick();

    public enum LinkStatus {
        NotFound, NotConnected, Invited, Connected, TCPEstablished
    }
}

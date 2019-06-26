package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import nist.p_70nanb17h188.demo.pscr19.R;

class Constants {
    private Constants() {
    }

    @NonNull
    static Link[] getConnections() {
        String[] wifiDirectNeighbors = nist.p_70nanb17h188.demo.pscr19.logic.link.Constants.getWifiDirectNeighbors();
        String[] bluetoothNeighbors = nist.p_70nanb17h188.demo.pscr19.logic.link.Constants.getBluetoothNeighbors();
        Link[] ret = new Link[wifiDirectNeighbors.length + bluetoothNeighbors.length];
        for (int i = 0; i < wifiDirectNeighbors.length; i++) {
            ret[i] = new LinkWifiDirect(wifiDirectNeighbors[i]);
        }
        for (int i = 0; i < bluetoothNeighbors.length; i++) {
            ret[i + wifiDirectNeighbors.length] = new LinkBluetooth(bluetoothNeighbors[i]);
        }
        return ret;
    }

    static int getLinkTypeColorResource(Class<? extends Link> type) {
        if (type == LinkWifiDirect.class) return R.color.colorLinkWifiDirect;
        if (type == LinkBluetooth.class) return R.color.colorLinkBluetooth;
        return R.color.colorLinkUnknown;
    }

    static int getLinkStatusImageResource(@Nullable Link.LinkStatus status) {
        if (status == null) return R.drawable.ic_circle_red;
        switch (status) {
            default:
            case NotFound:
                return R.drawable.ic_circle_red;
            case NotConnected:
                return R.drawable.ic_circle_yellow;
            case Invited:
                return R.drawable.ic_circle_magenta;
            case Connected:
                return R.drawable.ic_circle_blue;
            case TCPEstablished:
                return R.drawable.ic_circle_green;
        }
    }

    static int getDiscoverStatusImageResource(Boolean isDiscovering) {
        return (isDiscovering == null || !isDiscovering) ? R.drawable.ic_circle_red : R.drawable.ic_circle_green;
    }

    static int getEstablishActionImageResource(Boolean isEstablished) {
        return (isEstablished == null || !isEstablished) ? R.drawable.ic_link_connect : R.drawable.ic_link_disconnect;
    }

}

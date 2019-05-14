package nist.p_70nanb17h188.demo.pscr19.gui.link;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.Device;

class Constants {
    private Constants() {
    }

    @NonNull
    static Link[] getConnections() {
        switch (Device.getName()) {
            case Device.NAME_M1:
                return new Link[]{new LinkWifiDirect(Device.NAME_PC1), new LinkWifiDirect(Device.NAME_M2), new LinkWifiDirect(Device.NAME_MULE), new LinkBluetooth(Device.NAME_S11), new LinkBluetooth(Device.NAME_S13)};
            case Device.NAME_M2:
                return new Link[]{new LinkWifiDirect(Device.NAME_M1), new LinkBluetooth(Device.NAME_S21)};
            case Device.NAME_S11:
                return new Link[]{new LinkBluetooth(Device.NAME_S12), new LinkBluetooth(Device.NAME_M1)};
            case Device.NAME_S12:
                return new Link[]{new LinkBluetooth(Device.NAME_S11), new LinkBluetooth(Device.NAME_S13)};
            case Device.NAME_S13:
                return new Link[]{new LinkBluetooth(Device.NAME_M1), new LinkBluetooth(Device.NAME_S12)};
            case Device.NAME_S21:
                return new Link[]{new LinkBluetooth(Device.NAME_M2)};
            case Device.NAME_PC1:
                return new Link[]{new LinkWifiDirect(Device.NAME_M1)};
            case Device.NAME_MULE:
                return new Link[]{new LinkWifiDirect(Device.NAME_M1), new LinkWifiDirect(Device.NAME_ROUTER)};
            case Device.NAME_PC2:
                return new Link[]{new LinkWifiDirect(Device.NAME_ROUTER)};
            case Device.NAME_ROUTER:
                return new Link[]{new LinkWifiDirect(Device.NAME_MULE), new LinkWifiDirect(Device.NAME_PC2)};
        }
        return new Link[0];
    }

    static int getLinkTypeColorResource(Class<? extends Link> type) {
        if (type == LinkWifiDirect.class) return R.color.colorWifiDirect;
        if (type == LinkBluetooth.class) return R.color.colorBluetooth;
        return R.color.coloUnknown;
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

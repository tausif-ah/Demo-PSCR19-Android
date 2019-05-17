package nist.p_70nanb17h188.demo.pscr19.logic.link;

import android.support.annotation.NonNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.logic.Device;

public class Constants {
    static final int MAX_GROUP_OWNER_INTENT = 15;
    static final int MIN_GROUP_OWNER_INTENT = 0;
    static final int WIFI_DIRECT_SERVER_LISTEN_PORT = 10305;
    static final int BLUETOOTH_SERVER_LISTEN_PORT = 10307;
    static final InetAddress WIFI_DIRECT_SERVER_ADDRESS;

    private static final HashSet<String> WIFI_DIRECT_GROUP_OWNERS = new HashSet<>(Arrays.asList(Device.NAME_M1, Device.NAME_ROUTER));


    static {
        InetAddress tmp = null;
        try {
            tmp = InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 49, 1});
        } catch (UnknownHostException e) {
            e.printStackTrace(System.err);
        }
        WIFI_DIRECT_SERVER_ADDRESS = tmp;
    }


    private Constants() {
    }

    static boolean isWifiDirectGroupOwner() {
        return WIFI_DIRECT_GROUP_OWNERS.contains(Device.getName());
    }

    @NonNull
    public static String[] getWifiDirectNeighbors() {
        switch (Device.getName()) {
            case Device.NAME_M1:
                return new String[]{Device.NAME_PC1, Device.NAME_M2, Device.NAME_MULE};
            case Device.NAME_M2:
            case Device.NAME_PC1:
                return new String[]{Device.NAME_M1};
            case Device.NAME_MULE:
                return new String[]{Device.NAME_M1, Device.NAME_ROUTER};
            case Device.NAME_PC2:
                return new String[]{Device.NAME_ROUTER};
            case Device.NAME_ROUTER:
                return new String[]{Device.NAME_MULE, Device.NAME_PC2};
            default:
                return new String[0];
        }
    }

    @NonNull
    public static NeighborID getNeighborID(String name) {
        return new NeighborID(name);
    }


    @NonNull
    public static String[] getBluetoothNeighbors() {
        switch (Device.getName()) {
            case Device.NAME_M1:
                return new String[]{Device.NAME_S11, Device.NAME_S13};
            case Device.NAME_M2:
                return new String[]{Device.NAME_S21};
            case Device.NAME_S11:
                return new String[]{Device.NAME_M1, Device.NAME_S12};
            case Device.NAME_S12:
                return new String[]{Device.NAME_S11, Device.NAME_S13};
            case Device.NAME_S13:
                return new String[]{Device.NAME_M1, Device.NAME_S12};
            case Device.NAME_S21:
                return new String[]{Device.NAME_M2};
            default:
                return new String[0];
        }
    }
}

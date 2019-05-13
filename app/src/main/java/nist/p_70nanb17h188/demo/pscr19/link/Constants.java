package nist.p_70nanb17h188.demo.pscr19.link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.R;

public class Constants {
    public enum LinkType {
        WiFiDirect(R.color.colorWifiDirect),
        Bluetooth(R.color.colorBluetooth);

        private final int colorResource;

        LinkType(int colorResource) {
            this.colorResource = colorResource;
        }

        public int getColorResource() {
            return colorResource;
        }
    }

    public static class Connection {
        private final String destinationName;
        private final LinkType type;
        private final boolean establishedByDefault;

        Connection(String destinationName, LinkType type, boolean establishedByDefault) {
            this.destinationName = destinationName;
            this.type = type;
            this.establishedByDefault = establishedByDefault;
        }


        public String getDestinationName() {
            return destinationName;
        }

        public LinkType getType() {
            return type;
        }

        public boolean isEstablishedByDefault() {
            return establishedByDefault;
        }
    }

    private static final HashSet<String> WIFI_DIRECT_GROUP_OWNERS;
    private static final HashMap<String, Connection[]> CONNECTIONS;

    static {
        WIFI_DIRECT_GROUP_OWNERS = new HashSet<>(Arrays.asList(Device.NAME_M1, Device.NAME_PC2));
        CONNECTIONS = new HashMap<>();
        CONNECTIONS.put(Device.NAME_M1, new Connection[]{
                new Connection(Device.NAME_PC1, LinkType.WiFiDirect, false),
                new Connection(Device.NAME_M2, LinkType.WiFiDirect, false),
                new Connection(Device.NAME_MULE, LinkType.WiFiDirect, false),
                new Connection(Device.NAME_S11, LinkType.Bluetooth, true),
                new Connection(Device.NAME_S13, LinkType.Bluetooth, true)});
        CONNECTIONS.put(Device.NAME_M2, new Connection[]{
                new Connection(Device.NAME_S21, LinkType.Bluetooth, false),
                new Connection(Device.NAME_M1, LinkType.WiFiDirect, true)});
        CONNECTIONS.put(Device.NAME_S11, new Connection[]{
                new Connection(Device.NAME_S12, LinkType.Bluetooth, true),
                new Connection(Device.NAME_M1, LinkType.Bluetooth, true)});
        CONNECTIONS.put(Device.NAME_S12, new Connection[]{
                new Connection(Device.NAME_S11, LinkType.Bluetooth, true),
                new Connection(Device.NAME_S13, LinkType.Bluetooth, true)});
        CONNECTIONS.put(Device.NAME_S13, new Connection[]{
                new Connection(Device.NAME_M1, LinkType.Bluetooth, true),
                new Connection(Device.NAME_S12, LinkType.Bluetooth, true)});
        CONNECTIONS.put(Device.NAME_S21, new Connection[]{
                new Connection(Device.NAME_M2, LinkType.Bluetooth, false)});
        CONNECTIONS.put(Device.NAME_PC1, new Connection[]{
                new Connection(Device.NAME_M1, LinkType.WiFiDirect, true)});
        CONNECTIONS.put(Device.NAME_MULE, new Connection[]{
                new Connection(Device.NAME_M1, LinkType.WiFiDirect, false),
                new Connection(Device.NAME_PC2, LinkType.WiFiDirect, true)});
        CONNECTIONS.put(Device.NAME_PC2, new Connection[]{
                new Connection(Device.NAME_MULE, LinkType.WiFiDirect, true)});
    }

    public static boolean isWifiDirectGroupowner() {
        return WIFI_DIRECT_GROUP_OWNERS.contains(Device.getName());
    }

    public static List<Connection> getConnections() {
        Connection[] ret = CONNECTIONS.get(Device.getName());
        return ret == null ? new ArrayList<>() : Arrays.asList(ret);
    }


}

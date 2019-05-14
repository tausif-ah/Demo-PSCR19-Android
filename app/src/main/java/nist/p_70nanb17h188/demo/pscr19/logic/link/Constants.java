package nist.p_70nanb17h188.demo.pscr19.logic.link;

import java.util.Arrays;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.logic.Device;

class Constants {
    static final int MAX_GROUP_OWNER_INTENT = 15;
    static final int MIN_GROUP_OWNER_INTENT = 0;
    private static final HashSet<String> WIFI_DIRECT_GROUP_OWNERS = new HashSet<>(Arrays.asList(Device.NAME_M1, Device.NAME_ROUTER));

    private Constants() {
    }

    static boolean isWifiDirectGroupOwner() {
        return WIFI_DIRECT_GROUP_OWNERS.contains(Device.getName());
    }
}

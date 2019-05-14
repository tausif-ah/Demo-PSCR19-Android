package nist.p_70nanb17h188.demo.pscr19.logic;

import java.util.Arrays;
import java.util.HashSet;

public class Device {

    public static final String NAME_PC1 = "PC1";
    public static final String NAME_PC2 = "PC2";
    public static final String NAME_M1 = "M1";
    public static final String NAME_M2 = "M2";
    public static final String NAME_ROUTER = "Router";
    public static final String NAME_MULE = "Mule";
    public static final String NAME_S11 = "S11";
    public static final String NAME_S12 = "S12";
    public static final String NAME_S13 = "S13";
    public static final String NAME_S21 = "S21";
    private static final HashSet<String> ALL_NAMES = new HashSet<>(Arrays.asList(NAME_PC1, NAME_PC2, NAME_ROUTER, NAME_M1, NAME_M2, NAME_MULE, NAME_S11, NAME_S12, NAME_S13, NAME_S21));
    private static final HashSet<String> PHONES = new HashSet<>(Arrays.asList(NAME_M1, NAME_M2, NAME_MULE, NAME_S11, NAME_S12, NAME_S13, NAME_S21, NAME_ROUTER));

    private static String _name;

    /**
     * Set the name of the current device.
     *
     * @return the name of the current device.
     */
    public static String getName() {
        return _name;
    }

    /**
     * Get the name of the current device.
     *
     * @param name the name of the current device.
     */
    public static void setName(String name) {
        _name = name;
    }

    /**
     * Get all the device names in the demo.
     *
     * @return the names of devices.
     */
    public static String[] getExistingNames() {
        String[] ret = new String[ALL_NAMES.size()];
        return ALL_NAMES.toArray(ret);
    }

    /**
     * Checks if the specified device is a phone.
     *
     * @param name the name of the device.
     * @return if the device is a phone.
     */
    public static boolean isPhone(String name) {
        return PHONES.contains(name);
    }

    /**
     * Checks if the name is a known device.
     *
     * @param name the name to be checked.
     * @return true if the device is known.
     */
    public static boolean isNameExists(String name) {
        return ALL_NAMES.contains(name);
    }
}

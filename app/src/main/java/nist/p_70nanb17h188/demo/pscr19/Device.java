package nist.p_70nanb17h188.demo.pscr19;

public class Device {

    public static final String NAME_PC1 = "PC1";
    public static final String NAME_PC2 = "PC2";
    public static final String NAME_M1 = "M1";
    public static final String NAME_M2 = "M2";
    public static final String NAME_MULE = "Mule";
    public static final String NAME_S11 = "S11";
    public static final String NAME_S12 = "S12";
    public static final String NAME_S13 = "S13";
    public static final String NAME_S21 = "S21";

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
        return new String[]{
                NAME_PC1,
                NAME_PC2,
                NAME_M1,
                NAME_M2,
                NAME_MULE,
                NAME_S11,
                NAME_S12,
                NAME_S13,
                NAME_S21
        };
    }
}

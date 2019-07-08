package nist.p_70nanb17h188.demo.pscr19.gui.work_offload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class Constants {
    static boolean isWorkOffloadMaster() {
        return Device.getName().equals(Device.NAME_M1);
    }

    @NonNull
    static Name getMulticastName() {
        return new Name(-1);
    }

    @Nullable
    public static Name getName() {
        switch (Device.getName()) {
            case Device.NAME_M1:
                return new Name(1);
//            case Device.NAME_M2:
//                return new Name(2);
//            case Device.NAME_MULE:
//                return new Name(3);
            case Device.NAME_S11:
                return new Name(4);
//            case Device.NAME_S12:
//                return new Name(5);
            case Device.NAME_S13:
                return new Name(6);
            case Device.NAME_S21:
                return new Name(7);
        }
        return null;
    }


    private Constants() {
    }
}

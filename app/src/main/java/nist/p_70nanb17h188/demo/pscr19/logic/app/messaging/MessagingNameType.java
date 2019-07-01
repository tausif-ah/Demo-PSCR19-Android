package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public enum MessagingNameType {
    Administrative((byte) 0, "AD"),
    Incident((byte) 1, "IN");
    private final byte represent;
    @NonNull
    private final String abbrv;

    MessagingNameType(byte represent, @NonNull String abbrv) {
        this.represent = represent;
        this.abbrv = abbrv;
    }

    public byte getRepresent() {
        return represent;
    }

    @NonNull
    public String getAbbrv() {
        return abbrv;
    }

    @Nullable
    public static MessagingNameType fromByte(byte val) {
        switch (val) {
            case 0:
                return MessagingNameType.Administrative;
            case 1:
                return MessagingNameType.Incident;
            default:
                return null;
        }
    }

}

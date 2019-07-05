package nist.p_70nanb17h188.demo.pscr19.gui.net;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNameType;

public class Constants {
    public static int getNameTypeColorResource(MessagingNameType type) {
        if (type == null) return R.color.colorNameNull;
        switch (type) {
            case Administrative:
                return R.color.colorNameAdministrative;
            case Incident:
                return R.color.colorNameIncident;
            default:
                return R.color.colorNameUnknown;
        }
    }
}

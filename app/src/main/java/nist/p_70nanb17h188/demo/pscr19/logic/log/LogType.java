package nist.p_70nanb17h188.demo.pscr19.logic.log;

import android.support.annotation.NonNull;

public enum LogType {
    Verbose(2, "V"), Debug(3, "D"), Info(4, "I"), Warn(5, "W"), Error(6, "E");

    private final int val;
    @NonNull
    private final String acry;

    LogType(int val, @NonNull String acry) {
        this.val = val;
        this.acry = acry;
    }

    public int getVal() {
        return val;
    }

    @NonNull
    public String getAcry() {
        return acry;
    }
}

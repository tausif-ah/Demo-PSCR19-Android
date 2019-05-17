package nist.p_70nanb17h188.demo.pscr19.logic.log;

public enum LogType {
    Verbose(2, "V"), Debug(3, "D"), Info(4, "I"), Warn(5, "W"), Error(6, "E");

    public final int val;
    public final String acry;

    LogType(int val, String acry) {
        this.val = val;
        this.acry = acry;
    }
}

package nist.p_70nanb17h188.demo.pscr19.logic.net;

public class NetLayer {
    private static NetLayer_Impl defaultInstance;

    public static void init() {
        defaultInstance = new NetLayer_Impl();
    }
}

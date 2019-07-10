package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;

public class Constants {

    public static Name[] knownNames() {
        return new Name[]{
                new Name(1),
                new Name(2),
                new Name(3),
                new Name(4),
                new Name(5),
                new Name(6),
                new Name(7),
        };
    }

    @Nullable
    public static NeighborID getNameDestination(@NonNull Name n) {

        switch ((int) n.getValue()) {
            case 1:
                return getNeighborID(Device.NAME_M1);
            case 2:
                return getNeighborID(Device.NAME_M2);
            case 3:
                return getNeighborID(Device.NAME_MULE);
            case 4:
                return getNeighborID(Device.NAME_S11);
            case 5:
                return getNeighborID(Device.NAME_S12);
            case 6:
                return getNeighborID(Device.NAME_S13);
            case 7:
                return getNeighborID(Device.NAME_S21);
            default:
                return null;
        }
    }

    // key: hostId, value.key: dstId, value.value: nextHopId
    private static final HashMap<NeighborID, HashMap<NeighborID, NeighborID>> ROUTING_TABLE = new HashMap<>();

    @NonNull
    static NeighborID getNeighborID(String neighborName) {
        return nist.p_70nanb17h188.demo.pscr19.logic.link.Constants.getNeighborID(neighborName);
    }

    private static void addRouting(@NonNull HashMap<NeighborID, NeighborID> localRouting, @NonNull String dst, @NonNull String nextHop) {
        localRouting.put(getNeighborID(dst), getNeighborID(nextHop));
    }

    static {
        { // S11
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
//            addRouting(localRouting, Device.NAME_S11, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S12, Device.NAME_S12);
            addRouting(localRouting, Device.NAME_S13, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S21, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M2, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_PC1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_MULE, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_ROUTER);
//            addRouting(localRouting, Device.NAME_PC2, Device.NAME_M1);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_S11), localRouting);
        }
        { // S12
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
            addRouting(localRouting, Device.NAME_S11, Device.NAME_S11);
//            addRouting(localRouting, Device.NAME_S12, Device.NAME_S11);
            addRouting(localRouting, Device.NAME_S13, Device.NAME_S11);
            addRouting(localRouting, Device.NAME_S21, Device.NAME_S11);
            addRouting(localRouting, Device.NAME_M1, Device.NAME_S11);
            addRouting(localRouting, Device.NAME_M2, Device.NAME_S11);
            addRouting(localRouting, Device.NAME_PC1, Device.NAME_S11);
            addRouting(localRouting, Device.NAME_MULE, Device.NAME_S11);
//            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_ROUTER);
//            addRouting(localRouting, Device.NAME_PC2, Device.NAME_M1);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_S12), localRouting);
        }
        { // S13
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
            addRouting(localRouting, Device.NAME_S11, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S12, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_S13, Device.NAME_S11);
            addRouting(localRouting, Device.NAME_S21, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M2, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_PC1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_MULE, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_ROUTER);
//            addRouting(localRouting, Device.NAME_PC2, Device.NAME_M1);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_S13), localRouting);
        }
        { // S21
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
            addRouting(localRouting, Device.NAME_S11, Device.NAME_M2);
            addRouting(localRouting, Device.NAME_S12, Device.NAME_M2);
            addRouting(localRouting, Device.NAME_S13, Device.NAME_M2);
//            addRouting(localRouting, Device.NAME_S21, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M1, Device.NAME_M2);
            addRouting(localRouting, Device.NAME_M2, Device.NAME_M2);
            addRouting(localRouting, Device.NAME_PC1, Device.NAME_M2);
            addRouting(localRouting, Device.NAME_MULE, Device.NAME_M2);
//            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_ROUTER);
//            addRouting(localRouting, Device.NAME_PC2, Device.NAME_M1);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_S21), localRouting);
        }
        { // M1
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
            addRouting(localRouting, Device.NAME_S11, Device.NAME_S11);
            addRouting(localRouting, Device.NAME_S12, Device.NAME_S11);
            addRouting(localRouting, Device.NAME_S13, Device.NAME_S13);
            addRouting(localRouting, Device.NAME_S21, Device.NAME_M2);
//            addRouting(localRouting, Device.NAME_M1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M2, Device.NAME_M2);
            addRouting(localRouting, Device.NAME_PC1, Device.NAME_PC1);
            addRouting(localRouting, Device.NAME_MULE, Device.NAME_MULE);
//            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_ROUTER);
//            addRouting(localRouting, Device.NAME_PC2, Device.NAME_M1);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_M1), localRouting);
        }
        { // M2
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
            addRouting(localRouting, Device.NAME_S11, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S12, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S13, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S21, Device.NAME_S21);
            addRouting(localRouting, Device.NAME_M1, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_M2, Device.NAME_M2);
            addRouting(localRouting, Device.NAME_PC1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_MULE, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_ROUTER);
//            addRouting(localRouting, Device.NAME_PC2, Device.NAME_M1);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_M2), localRouting);
        }
        { // PC1
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
            addRouting(localRouting, Device.NAME_S11, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S12, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S13, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S21, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M2, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_PC1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_MULE, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_PC2, Device.NAME_M1);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_PC1), localRouting);
        }
        { // Mule
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
            addRouting(localRouting, Device.NAME_S11, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S12, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S13, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_S21, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_M2, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_PC1, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_MULE, Device.NAME_M2);
            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_ROUTER);
            addRouting(localRouting, Device.NAME_PC2, Device.NAME_ROUTER);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_MULE), localRouting);
        }
        { // Router
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
//            addRouting(localRouting, Device.NAME_S11, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_S12, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_S13, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_S21, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_M1, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_M2, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_PC1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_MULE, Device.NAME_MULE);
//            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_ROUTER);
            addRouting(localRouting, Device.NAME_PC2, Device.NAME_PC2);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_ROUTER), localRouting);
        }
        { // PC2
            HashMap<NeighborID, NeighborID> localRouting = new HashMap<>();
//            addRouting(localRouting, Device.NAME_S11, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_S12, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_S13, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_S21, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_M1, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_M2, Device.NAME_M1);
//            addRouting(localRouting, Device.NAME_PC1, Device.NAME_M1);
            addRouting(localRouting, Device.NAME_MULE, Device.NAME_ROUTER);
            addRouting(localRouting, Device.NAME_ROUTER, Device.NAME_ROUTER);
//            addRouting(localRouting, Device.NAME_PC2, Device.NAME_M1);
            ROUTING_TABLE.put(getNeighborID(Device.NAME_PC2), localRouting);
        }
    }

    @Nullable
    static NeighborID getNextHopNeighborID(@NonNull NeighborID hostId, @NonNull NeighborID dstId) {
        HashMap<NeighborID, NeighborID> localRouting = ROUTING_TABLE.get(hostId);
        if (localRouting == null) return null;
        return localRouting.get(dstId);

    }
}

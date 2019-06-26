package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import java.util.ArrayList;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.logic.Tuple2;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class Constants {
    static Name getDefaultListenName() {
        return new Name(-99);
    }

    static Name getIncidentRoot() {
        return new Name(-102);
    }

    static Name getDispatcherRoot() {
        return new Name(-108);
    }

    static HashSet<MessagingNamespace.MessagingName> getInitialNamespaceNames() {
        HashSet<MessagingNamespace.MessagingName> ret = new HashSet<>();
        ret.add(new MessagingNamespace.MessagingName(new Name(-100), "New Jersey", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-101), "First Response", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-102), "Incidents", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-103), "Middlesex County", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-104), "Union County", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-105), "EMS", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-106), "Police", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-107), "Fire", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-108), "Dispatcher", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-109), "Incident Commander", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-110), "Middlesex Fire", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-111), "Middlesex Police", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-112), "Middlesex EMS", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-113), "Union Police", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-114), "Union Fire", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-115), "Avenel F.D.", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-116), "Bridgewater Twp", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-117), "Patrol Division", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-118), "Rahway F.D.", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-119), "5-1 Pumper", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-120), "5-2 Rescue", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-121), "Ambulance 1", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-122), "Ambulance 2", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-123), "Patrol Car", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-124), "TAC1", MessagingNamespace.MessagingNameType.Administrative));
        ret.add(new MessagingNamespace.MessagingName(new Name(-125), "Rescue 1", MessagingNamespace.MessagingNameType.Administrative));

        ret.add(new MessagingNamespace.MessagingName(new Name(-126), "Irma", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-127), "Task Force Leaders", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-128), "Safety Officers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-129), "Search Team", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-130), "Search Team Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-131), "Canine Search Specialist", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-132), "Technical Search Specialist", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-133), "Rescue Teams", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-134), "Rescue Team Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-135), "Rescue Squad 1", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-136), "Rescue Squad 2", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-137), "Rescue Squad 1 Officer", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-138), "Rescue Squad 2 Officer", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-139), "Rescue Squad 2 Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-140), "Rescue Squad 1 Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-141), "Haz Mat Team", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-142), "Haz Mat Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-143), "Haz Mat Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-144), "Heavy Equipment Rigging Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-145), "Medical Team", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-146), "Medical Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-147), "Medical Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-148), "Logistic Team", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-149), "Logistic Team Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-150), "Logistic Team Specialists", MessagingNamespace.MessagingNameType.Incident));

        return ret;
    }

    static ArrayList<Tuple2<Name, Name>> getInitialNamespaceRelationship() {
        ArrayList<Tuple2<Name,Name>> ret = new ArrayList<>();

        ret.add(new Tuple2<>(new Name(-100), new Name(-103)));
        ret.add(new Tuple2<>(new Name(-100), new Name(-104)));
        ret.add(new Tuple2<>(new Name(-101), new Name(-105)));
        ret.add(new Tuple2<>(new Name(-101), new Name(-106)));
        ret.add(new Tuple2<>(new Name(-101), new Name(-107)));
        ret.add(new Tuple2<>(new Name(-101), new Name(-108)));
        ret.add(new Tuple2<>(new Name(-101), new Name(-109)));
        ret.add(new Tuple2<>(new Name(-103), new Name(-110)));
        ret.add(new Tuple2<>(new Name(-103), new Name(-111)));
        ret.add(new Tuple2<>(new Name(-103), new Name(-112)));
        ret.add(new Tuple2<>(new Name(-104), new Name(-113)));
        ret.add(new Tuple2<>(new Name(-104), new Name(-114)));
        ret.add(new Tuple2<>(new Name(-105), new Name(-112)));
        ret.add(new Tuple2<>(new Name(-106), new Name(-111)));
        ret.add(new Tuple2<>(new Name(-106), new Name(-113)));
        ret.add(new Tuple2<>(new Name(-107), new Name(-110)));
        ret.add(new Tuple2<>(new Name(-107), new Name(-114)));
        ret.add(new Tuple2<>(new Name(-110), new Name(-115)));
        ret.add(new Tuple2<>(new Name(-111), new Name(-117)));
        ret.add(new Tuple2<>(new Name(-112), new Name(-116)));
        ret.add(new Tuple2<>(new Name(-114), new Name(-118)));
        ret.add(new Tuple2<>(new Name(-115), new Name(-119)));
        ret.add(new Tuple2<>(new Name(-115), new Name(-120)));
        ret.add(new Tuple2<>(new Name(-116), new Name(-121)));
        ret.add(new Tuple2<>(new Name(-116), new Name(-122)));
        ret.add(new Tuple2<>(new Name(-117), new Name(-123)));
        ret.add(new Tuple2<>(new Name(-118), new Name(-124)));
        ret.add(new Tuple2<>(new Name(-118), new Name(-125)));

        ret.add(new Tuple2<>(new Name(-102), new Name(-126)));
        ret.add(new Tuple2<>(new Name(-126), new Name(-127)));
        ret.add(new Tuple2<>(new Name(-126), new Name(-128)));
        ret.add(new Tuple2<>(new Name(-126), new Name(-129)));
        ret.add(new Tuple2<>(new Name(-126), new Name(-133)));
        ret.add(new Tuple2<>(new Name(-126), new Name(-141)));
        ret.add(new Tuple2<>(new Name(-126), new Name(-145)));
        ret.add(new Tuple2<>(new Name(-126), new Name(-148)));
        ret.add(new Tuple2<>(new Name(-129), new Name(-130)));
        ret.add(new Tuple2<>(new Name(-129), new Name(-131)));
        ret.add(new Tuple2<>(new Name(-129), new Name(-132)));
        ret.add(new Tuple2<>(new Name(-133), new Name(-134)));
        ret.add(new Tuple2<>(new Name(-133), new Name(-135)));
        ret.add(new Tuple2<>(new Name(-133), new Name(-136)));
        ret.add(new Tuple2<>(new Name(-135), new Name(-137)));
        ret.add(new Tuple2<>(new Name(-135), new Name(-140)));
        ret.add(new Tuple2<>(new Name(-136), new Name(-138)));
        ret.add(new Tuple2<>(new Name(-136), new Name(-139)));
        ret.add(new Tuple2<>(new Name(-141), new Name(-142)));
        ret.add(new Tuple2<>(new Name(-141), new Name(-143)));
        ret.add(new Tuple2<>(new Name(-141), new Name(-144)));
        ret.add(new Tuple2<>(new Name(-145), new Name(-146)));
        ret.add(new Tuple2<>(new Name(-145), new Name(-147)));
        ret.add(new Tuple2<>(new Name(-148), new Name(-149)));
        ret.add(new Tuple2<>(new Name(-148), new Name(-150)));

        ret.add(new Tuple2<>(new Name(-148), new Name(-149)));
        ret.add(new Tuple2<>(new Name(-148), new Name(-150)));
        ret.add(new Tuple2<>(new Name(-127), new Name(-109)));
        ret.add(new Tuple2<>(new Name(-147), new Name(-116)));
        ret.add(new Tuple2<>(new Name(-137), new Name(-123)));
        ret.add(new Tuple2<>(new Name(-140), new Name(-119)));
        ret.add(new Tuple2<>(new Name(-140), new Name(-120)));

        return ret;
    }
}

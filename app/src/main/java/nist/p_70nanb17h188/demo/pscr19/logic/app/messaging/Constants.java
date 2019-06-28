package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.logic.Tuple2;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class Constants {
    @NonNull
    static Name getDefaultListenName() {
        return new Name(-99);
    }

    @NonNull
    static Name getIncidentRoot() {
        return new Name(-102);
    }

    @NonNull
    static Name getDispatcherRoot() {
        return new Name(-108);
    }

    @NonNull
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
        ret.add(new MessagingNamespace.MessagingName(new Name(-126), "Field Officer", MessagingNamespace.MessagingNameType.Administrative));

        ret.add(new MessagingNamespace.MessagingName(new Name(-200), "Irma", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-201), "Task Force Leaders", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-202), "Safety Officers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-203), "Search Team", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-204), "Search Team Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-205), "Canine Search Specialist", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-206), "Technical Search Specialist", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-207), "Rescue Teams", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-208), "Rescue Team Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-209), "Rescue Squad 1", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-210), "Rescue Squad 2", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-211), "Rescue Squad 1 Officer", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-212), "Rescue Squad 2 Officer", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-213), "Rescue Squad 2 Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-214), "Rescue Squad 1 Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-215), "Haz Mat Team", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-216), "Haz Mat Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-217), "Haz Mat Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-218), "Heavy Equipment Rigging Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-219), "Medical Team", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-220), "Medical Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-221), "Medical Specialists", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-222), "Logistic Team", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-223), "Logistic Team Managers", MessagingNamespace.MessagingNameType.Incident));
        ret.add(new MessagingNamespace.MessagingName(new Name(-224), "Logistic Team Specialists", MessagingNamespace.MessagingNameType.Incident));


        return ret;
    }

    @NonNull
    static ArrayList<Tuple2<Name, Name>> getInitialNamespaceRelationship() {
        ArrayList<Tuple2<Name, Name>> ret = new ArrayList<>();

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
        ret.add(new Tuple2<>(new Name(-117), new Name(-126)));

        ret.add(new Tuple2<>(new Name(-102), new Name(-200)));
        ret.add(new Tuple2<>(new Name(-200), new Name(-201)));
        ret.add(new Tuple2<>(new Name(-200), new Name(-202)));
        ret.add(new Tuple2<>(new Name(-200), new Name(-203)));
        ret.add(new Tuple2<>(new Name(-200), new Name(-207)));
        ret.add(new Tuple2<>(new Name(-200), new Name(-215)));
        ret.add(new Tuple2<>(new Name(-200), new Name(-219)));
        ret.add(new Tuple2<>(new Name(-200), new Name(-222)));
        ret.add(new Tuple2<>(new Name(-203), new Name(-204)));
        ret.add(new Tuple2<>(new Name(-203), new Name(-205)));
        ret.add(new Tuple2<>(new Name(-203), new Name(-206)));
        ret.add(new Tuple2<>(new Name(-207), new Name(-208)));
        ret.add(new Tuple2<>(new Name(-207), new Name(-209)));
        ret.add(new Tuple2<>(new Name(-207), new Name(-210)));
        ret.add(new Tuple2<>(new Name(-209), new Name(-211)));
        ret.add(new Tuple2<>(new Name(-209), new Name(-214)));
        ret.add(new Tuple2<>(new Name(-210), new Name(-212)));
        ret.add(new Tuple2<>(new Name(-210), new Name(-213)));
        ret.add(new Tuple2<>(new Name(-215), new Name(-216)));
        ret.add(new Tuple2<>(new Name(-215), new Name(-217)));
        ret.add(new Tuple2<>(new Name(-215), new Name(-218)));
        ret.add(new Tuple2<>(new Name(-219), new Name(-220)));
        ret.add(new Tuple2<>(new Name(-219), new Name(-221)));
        ret.add(new Tuple2<>(new Name(-222), new Name(-223)));
        ret.add(new Tuple2<>(new Name(-222), new Name(-224)));

        ret.add(new Tuple2<>(new Name(-201), new Name(-109)));
        ret.add(new Tuple2<>(new Name(-221), new Name(-116)));
        ret.add(new Tuple2<>(new Name(-211), new Name(-123)));
        ret.add(new Tuple2<>(new Name(-214), new Name(-119)));
        ret.add(new Tuple2<>(new Name(-214), new Name(-120)));


        return ret;
    }
}

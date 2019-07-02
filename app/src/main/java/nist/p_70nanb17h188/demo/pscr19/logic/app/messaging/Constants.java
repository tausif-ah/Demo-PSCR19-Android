package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.Device;
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

    @Nullable
    static Name getDefaultSubscription() {
        switch (Device.getName()) {
            case Device.NAME_M1:
                return new Name(-125); // Rescue 1
            case Device.NAME_M2:
                return new Name(-119); // 5-1 Pumper
            case Device.NAME_S11:
                return new Name(-124); // TAC 1
            case Device.NAME_S12:
                return new Name(-122); // Ambulance 2
            case Device.NAME_S13:
                return new Name(-121); // Ambulance 1
            case Device.NAME_S21:
                return new Name(-120); // 5-2 Rescue
            case Device.NAME_MULE:
                return new Name(-123); // Patrol Car
            // PCs will have subscriptions defined on web page parameters
            default:
                return null;
        }
    }

    @NonNull
    static HashSet<MessagingName> getInitialNamespaceNames() {
        HashSet<MessagingName> ret = new HashSet<>();
        ret.add(new MessagingName(new Name(-100), "New Jersey", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-101), "First Response", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-102), "Incidents", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-103), "Middlesex County", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-104), "Union County", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-105), "EMS", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-106), "Police", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-107), "Fire", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-108), "Dispatcher", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-109), "Incident Commander", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-110), "Middlesex Fire", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-111), "Middlesex Police", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-112), "Middlesex EMS", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-113), "Union Police", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-114), "Union Fire", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-115), "Avenel F.D.", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-116), "Bridgewater Twp", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-117), "Patrol Division", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-118), "Rahway F.D.", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-119), "5-1 Pumper", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-120), "5-2 Rescue", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-121), "Ambulance 1", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-122), "Ambulance 2", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-123), "Patrol Car", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-124), "TAC1", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-125), "Rescue 1", MessagingNameType.Administrative));
        ret.add(new MessagingName(new Name(-126), "Field Officer", MessagingNameType.Administrative));

        ret.add(new MessagingName(new Name(-200), "Irma", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-201), "Task Force Leaders", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-202), "Safety Officers", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-203), "Search Team", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-204), "Search Team Managers", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-205), "Canine Search Specialist", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-206), "Technical Search Specialist", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-207), "Rescue Teams", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-208), "Rescue Team Managers", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-209), "Rescue Squad 1", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-210), "Rescue Squad 2", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-211), "Rescue Squad 1 Officer", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-212), "Rescue Squad 2 Officer", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-213), "Rescue Squad 2 Specialists", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-214), "Rescue Squad 1 Specialists", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-215), "Haz Mat Team", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-216), "Haz Mat Managers", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-217), "Haz Mat Specialists", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-218), "Heavy Equipment Rigging Specialists", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-219), "Medical Team", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-220), "Medical Managers", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-221), "Medical Specialists", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-222), "Logistic Team", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-223), "Logistic Team Managers", MessagingNameType.Incident));
        ret.add(new MessagingName(new Name(-224), "Logistic Team Specialists", MessagingNameType.Incident));


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

    static void initTemplates() {

        // Template Structural Collapse
        {
            HashSet<MessagingName> allnames = new HashSet<>();
            ArrayList<Tuple2<Name, Name>> allRelationships = new ArrayList<>();

            allnames.add(new MessagingName(new Name(1), "Structural collapse", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(2), "Task force leader", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(3), "Safety officer", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(4), "Plans officer", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(5), "Search team", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(6), "Search team manager", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(7), "Canine search specialist T1", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(8), "Canine search specialist T2", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(9), "Technical search specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(10), "Rescue team", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(11), "Rescue team manager", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(12), "Rescue squad 1", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(13), "Rescue squad 2", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(14), "Rescue squad 3", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(15), "Rescue squad 4", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(16), "Rescue s1 officer", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(17), "Rescue s2 officer", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(18), "Rescue s3 officer", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(19), "Rescue s4 officer", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(20), "Rescue s4 specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(21), "Rescue s2 specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(22), "Rescue s3 specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(23), "Rescue s1 specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(24), "Medical team", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(25), "Medical team manager", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(26), "Medical team specialist", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(27), "Medical team specialist", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(28), "Technical team", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(29), "Technical team manager", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(30), "Structures specialist", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(31), "Haz Mat specialist", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(32), "Heavy rigging & equipment specialist", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(33), "Communications specialist", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(34), "Logistics specialist", MessagingNameType.Incident));

            allRelationships.add(new Tuple2<>(new Name(1), new Name(2)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(3)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(4)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(5)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(10)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(24)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(28)));
            allRelationships.add(new Tuple2<>(new Name(5), new Name(6)));
            allRelationships.add(new Tuple2<>(new Name(5), new Name(7)));
            allRelationships.add(new Tuple2<>(new Name(5), new Name(8)));
            allRelationships.add(new Tuple2<>(new Name(5), new Name(9)));
            allRelationships.add(new Tuple2<>(new Name(10), new Name(11)));
            allRelationships.add(new Tuple2<>(new Name(10), new Name(12)));
            allRelationships.add(new Tuple2<>(new Name(10), new Name(13)));
            allRelationships.add(new Tuple2<>(new Name(10), new Name(14)));
            allRelationships.add(new Tuple2<>(new Name(10), new Name(15)));
            allRelationships.add(new Tuple2<>(new Name(12), new Name(16)));
            allRelationships.add(new Tuple2<>(new Name(12), new Name(23)));
            allRelationships.add(new Tuple2<>(new Name(13), new Name(17)));
            allRelationships.add(new Tuple2<>(new Name(13), new Name(21)));
            allRelationships.add(new Tuple2<>(new Name(14), new Name(18)));
            allRelationships.add(new Tuple2<>(new Name(14), new Name(22)));
            allRelationships.add(new Tuple2<>(new Name(15), new Name(19)));
            allRelationships.add(new Tuple2<>(new Name(15), new Name(20)));
            allRelationships.add(new Tuple2<>(new Name(24), new Name(25)));
            allRelationships.add(new Tuple2<>(new Name(24), new Name(26)));
            allRelationships.add(new Tuple2<>(new Name(24), new Name(27)));
            allRelationships.add(new Tuple2<>(new Name(28), new Name(29)));
            allRelationships.add(new Tuple2<>(new Name(28), new Name(30)));
            allRelationships.add(new Tuple2<>(new Name(28), new Name(31)));
            allRelationships.add(new Tuple2<>(new Name(28), new Name(32)));
            allRelationships.add(new Tuple2<>(new Name(28), new Name(33)));
            allRelationships.add(new Tuple2<>(new Name(28), new Name(34)));


            new Template("Structural collapse", new Name(1), new Name(2), allnames, allRelationships);

        }

        // Template Structural Collapse
        {
            HashSet<MessagingName> allnames = new HashSet<>();
            ArrayList<Tuple2<Name, Name>> allRelationships = new ArrayList<>();

            allnames.add(new MessagingName(new Name(1), "Incident", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(2), "Task Force Leaders", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(3), "Safety Officers", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(4), "Search Team", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(5), "Search Team Managers", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(6), "Canine Search Specialist", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(7), "Technical Search Specialist", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(8), "Rescue Teams", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(9), "Rescue Team Managers", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(10), "Rescue Squad 1", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(11), "Rescue Squad 2", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(12), "Rescue Squad 1 Officer", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(13), "Rescue Squad 2 Officer", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(14), "Rescue Squad 2 Specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(15), "Rescue Squad 1 Specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(16), "Haz Mat Team", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(17), "Haz Mat Managers", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(18), "Haz Mat Specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(19), "Heavy Equipment Rigging Specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(20), "Medical Team", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(21), "Medical Managers", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(22), "Medical Specialists", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(23), "Logistic Team", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(24), "Logistic Team Managers", MessagingNameType.Incident));
            allnames.add(new MessagingName(new Name(25), "Logistic Team Specialists", MessagingNameType.Incident));

            allRelationships.add(new Tuple2<>(new Name(1), new Name(2)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(3)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(4)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(8)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(16)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(20)));
            allRelationships.add(new Tuple2<>(new Name(1), new Name(23)));
            allRelationships.add(new Tuple2<>(new Name(4), new Name(5)));
            allRelationships.add(new Tuple2<>(new Name(4), new Name(6)));
            allRelationships.add(new Tuple2<>(new Name(4), new Name(7)));
            allRelationships.add(new Tuple2<>(new Name(8), new Name(9)));
            allRelationships.add(new Tuple2<>(new Name(8), new Name(10)));
            allRelationships.add(new Tuple2<>(new Name(8), new Name(11)));
            allRelationships.add(new Tuple2<>(new Name(10), new Name(12)));
            allRelationships.add(new Tuple2<>(new Name(10), new Name(15)));
            allRelationships.add(new Tuple2<>(new Name(11), new Name(13)));
            allRelationships.add(new Tuple2<>(new Name(11), new Name(14)));
            allRelationships.add(new Tuple2<>(new Name(16), new Name(17)));
            allRelationships.add(new Tuple2<>(new Name(16), new Name(18)));
            allRelationships.add(new Tuple2<>(new Name(16), new Name(19)));
            allRelationships.add(new Tuple2<>(new Name(20), new Name(21)));
            allRelationships.add(new Tuple2<>(new Name(20), new Name(22)));
            allRelationships.add(new Tuple2<>(new Name(23), new Name(24)));
            allRelationships.add(new Tuple2<>(new Name(23), new Name(25)));

            new Template("Hurricane", new Name(1), new Name(2), allnames, allRelationships);

        }
    }
}

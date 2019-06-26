package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.Tuple2;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Namespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl;

/**
 * The namespace used by Messaging app.
 */
public class MessagingNamespace {
    private static final String TAG = "MessagingNamespace";

    private static MessagingNamespace defaultInstance;

    public static void init() {
        defaultInstance = new MessagingNamespace();

    }

    @NonNull
    public static MessagingNamespace getDefaultInstance() {
        return defaultInstance;
    }

    public enum MessagingNameType {
        Administrative(0, "AD"),
        Incident(1, "IN");
        private final int represent;
        @NonNull
        private final String abbrv;

        MessagingNameType(int represent, @NonNull String abbrv) {
            this.represent = represent;
            this.abbrv = abbrv;
        }

        public int getRepresent() {
            return represent;
        }

        @NonNull
        public String getAbbrv() {
            return abbrv;
        }

        @NonNull
        public static MessagingNameType fromInt(int val) {
            switch (val) {
                case 0:
                    return MessagingNameType.Administrative;
                case 1:
                    return MessagingNameType.Incident;
                default:
                    throw new AssertionError("Illegal MessagingNameType: " + val);
            }
        }

    }

    public static class MessagingName {

        @NonNull
        private final Name name;
        @NonNull
        private String appName;
        @NonNull
        private final MessagingNameType type;

        MessagingName(@NonNull Name name, @NonNull String appName, @NonNull MessagingNameType type) {
            this.name = name;
            this.appName = appName;
            this.type = type;
        }

        @NonNull
        public String getAppName() {
            return appName;
        }

        @NonNull
        public Name getName() {
            return name;
        }

        @NonNull
        public MessagingNameType getType() {
            return type;
        }

        public void setAppName(@NonNull String appName) {
            this.appName = appName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessagingName that = (MessagingName) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("%s [%s]", appName, type.getAbbrv());
        }
    }

    private final HashMap<Name, MessagingName> nameMappings = new HashMap<>();
    private HashMap<Name, HashSet<Name>> incidentMappings = null;
    // pending relationship list
//    private final 

    private MessagingNamespace() {

        for (MessagingName name : Constants.getInitialNamespaceNames())
            createNode(name.name, name.appName, name.type, NetLayer_Impl.INITIATOR_NET);
        for (Tuple2<Name, Name> entry : Constants.getInitialNamespaceRelationship())
            addNodeRelationship(entry.getV1(), entry.getV2(), NetLayer_Impl.INITIATOR_NET);

        // listen to events
        Context.getContext(Namespace.CONTEXT_NAMESPACE).registerReceiver((context, intent) -> {

        }, new IntentFilter()
                .addAction(Namespace.ACTION_NAME_CHANGED)
                .addAction(Namespace.ACTION_RELATIONSHIP_CHANGED));
        // register a unique name
        NetLayer.subscribe(Constants.getDefaultListenName(), (src, dst, data) -> {

        });
    }

    public Name getIncidentRoot() {
        return Constants.getIncidentRoot();
    }

    public Name getDispatcherRoot() {
        return Constants.getDispatcherRoot();
    }

    @Nullable
    public MessagingName getName(@NonNull Name name) {
        return nameMappings.get(name);
    }

    public Collection<MessagingName> getAllNodes() {
        return nameMappings.values();
    }

//    public Tuple3<Collection<GraphNode>, Integer, Integer> instantiateTemplate(int templateId, String incidentName) {
//        throw new UnsupportedOperationException();
//    }
//
//    public Collection<GraphNode> removeIncidentTree(int incidentRootId) {
//        throw new UnsupportedOperationException();
//    }
//

    MessagingName createNode(Name name, String appName, MessagingNameType type, @NonNull String initiator) {
        Namespace namespace = NetLayer.getDefaultInstance().getNamespace();
        if (namespace.hasName(name)) throw new AssertionError("Name" + name + "already exists!");
        namespace.addName(name, initiator);
        MessagingName an = new MessagingName(name, appName, type);
        nameMappings.put(name, an);
        Log.d(TAG, "createNode, %s create name %s->{%s, %s}", initiator, name, appName, type);
        return an;
    }

    private void clearNameIncidents() {
        synchronized (this) {
            incidentMappings = null;
        }
    }

    private void calculateNameIncidents() {
        synchronized (this) {
            if (incidentMappings != null) return;
            Namespace namespace = NetLayer.getDefaultInstance().getNamespace();
            incidentMappings = new HashMap<>();
            Name incidentRoot = Constants.getIncidentRoot();
            namespace.forEachChild(incidentRoot, incident ->
                    namespace.forEachDescendant(incident, descendant -> {
                        HashSet<Name> descendentIncidents = incidentMappings.get(descendant);
                        if (descendentIncidents == null) {
                            incidentMappings.put(descendant, descendentIncidents = new HashSet<>());
                            descendentIncidents.add(incident);
                        }
                    }));
        }
    }

    public String[] getNameIncidents(Name name) {
        synchronized (this) {
            if (incidentMappings == null)
                calculateNameIncidents();
            HashSet<Name> incidents = incidentMappings.get(name);
            if (incidents == null) return new String[0];
            String[] ret = new String[incidents.size()];
            int i = 0;
            for (Name n : incidents) {
                MessagingName incident = nameMappings.get(n);
                if (incident == null) ret[i++] = n.toString();
                else ret[i++] = incident.appName;
            }
            return ret;
        }
    }

//    public Name

//    public void removeNode(Name name) {
//
//    }

    //    public GraphNode removeNode(int id) {
//
//    }
//
//    public MessagingName createNode(String name, MessagingNameType type) {
//
//    }
//
//    public GraphNode createNodeWithParent(String name, GraphNode.GraphNodeType type, int parentId) {
//
//    }
//
//    public GraphNode removeNode(int id) {
//
//    }

    public void addNodeRelationship(Name parentName, Name childName, String initiator) {
        Namespace namespace = NetLayer.getDefaultInstance().getNamespace();
        namespace.addRelationship(parentName, childName, initiator);
    }

//    public GraphNode removeNodeRelationship(int parentId, int childId) {
//
//    }
//
//    public GraphNode updateNodeName(int id, String name) {
//
//    }
}

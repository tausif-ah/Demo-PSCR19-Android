package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.DelayRunner;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.logic.Consumer;
import nist.p_70nanb17h188.demo.pscr19.logic.Tuple2;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl;

/**
 * The namespace used by Messaging app.
 */
public class MessagingNamespace {

    /**
     * The context for messaging namespace events.
     */
    public static final String CONTEXT_MESSAGINGNAMESPACE = "nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace";

    /**
     * Broadcast intent action indicating that there are names added, removed, and relationships added, removed.
     * <p>
     * One extra {@link #EXTRA_NAMES_ADDED} (Collection&lt;{@link MessagingName}&gt;) indicates the names added.
     * Another extra {@link #EXTRA_NAMES_REMOVED} (Collection&lt;{@link Name}&gt;) indicates the names removed.
     * A third extra {@link #EXTRA_RELATIONSHIPS_ADDED} (Collection&lt;Tuple2&lt;{@link Name},{@link Name}&gt;&gt;) indicates the relationships added.
     * A fourth extra {@link #EXTRA_RELATIONSHIPS_REMOVED} (Collection&lt;Tuple2&lt;{@link Name},{@link Name}&gt;&gt;) indicates the relationships removed.
     */
    public static final String ACTION_NAMESPACE_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace.namespaceChanged";
    public static final String EXTRA_NAMES_ADDED = "na";
    public static final String EXTRA_NAMES_REMOVED = "nd";
    public static final String EXTRA_RELATIONSHIPS_ADDED = "ra";
    public static final String EXTRA_RELATIONSHIPS_REMOVED = "rd";

    /**
     * Broadcast intent action indicating that there are names added, removed, and relationships added, removed.
     * <p>
     * One extra {@link #EXTRA_NAME} ({@link MessagingName}) indicates the names whose appName is changed.
     */
    public static final String ACTION_APPNAME_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace.appNameChanged";
    public static final String EXTRA_NAME = "name";

    private static final byte DATA_TYPE_GRAPH_CHANGED = 1;
    private static final byte DATA_TYPE_APPNAME_CHANGED = 2;
    private static final long MESSAGE_BUFFER_FLUSH_DELAY = 1000;

    private static final String INITIATOR_INIT = NetLayer_Impl.INITIATOR_INIT;
    private static final String INITIATOR_NET = "nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace.net";
    private static final String INITIATOR_APP = "nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace.app";

    private static final String TAG = "MessagingNamespace";
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private static MessagingNamespace defaultInstance;

    public static void init() {
        defaultInstance = new MessagingNamespace();
    }

    @NonNull
    public static MessagingNamespace getDefaultInstance() {
        return defaultInstance;
    }

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

    public static class MessagingName {

        @NonNull
        private final Name name;
        @NonNull
        private String appName;
        @NonNull
        private final MessagingNameType type;

        @NonNull
        private final HashSet<MessagingName> parents = new HashSet<>();
        @NonNull
        private final HashSet<MessagingName> children = new HashSet<>();

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

        void setAppName(@NonNull String appName) {
            this.appName = appName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
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
            return String.format("%s(%s,%s)", appName, name, type);
        }
    }

    private final HashMap<Name, MessagingName> nameMappings = new HashMap<>();
    private HashMap<MessagingName, HashSet<MessagingName>> incidentMappings = null;

    private MessagingNamespace() {

        for (MessagingName name : Constants.getInitialNamespaceNames()) {
            innerCreateName(name.getName(), name.getAppName(), name.getType(), INITIATOR_INIT);
        }
        for (Tuple2<Name, Name> entry : Constants.getInitialNamespaceRelationship()) {
            innerCreateRelationship(entry.getV1(), entry.getV2(), INITIATOR_INIT);
        }

        // listen to a common channel for name updates
        NetLayer.subscribe(Constants.getDefaultListenName(), this::onMessageReceivedFromListenChannel);
    }

    @NonNull
    public Name getIncidentRoot() {
        return Constants.getIncidentRoot();
    }

    @NonNull
    public Name getDispatcherRoot() {
        return Constants.getDispatcherRoot();
    }

    @Nullable
    public synchronized MessagingName getName(@NonNull Name name) {
        return nameMappings.get(name);
    }

    public synchronized Collection<MessagingName> getAllNames() {
        return nameMappings.values();
    }

    public synchronized void forEachChild(@NonNull MessagingName parent, @NonNull Consumer<MessagingName> consumer) {
        for (MessagingName child : parent.children) {
            consumer.accept(child);
        }
    }

    public synchronized void forEachParent(@NonNull MessagingName child, @NonNull Consumer<MessagingName> consumer) {
        for (MessagingName parent : child.parents) {
            consumer.accept(parent);
        }
    }

    public synchronized void forEachDescendant(@NonNull MessagingName root, @NonNull Consumer<MessagingName> consumer) {
        ArrayDeque<MessagingName> todo = new ArrayDeque<>();
        HashSet<MessagingName> traversed = new HashSet<>();
        todo.add(root);

        while (!todo.isEmpty()) {
            MessagingName curr = todo.poll();
            if (traversed.contains(curr)) {
                continue;
            }
            traversed.add(curr);
            consumer.accept(curr);
            for (MessagingName child : curr.children) {
                todo.offer(child);
            }
        }
    }

    public synchronized void forEachAncestor(@NonNull MessagingName root, @NonNull Consumer<MessagingName> consumer) {
        ArrayDeque<MessagingName> todo = new ArrayDeque<>();
        HashSet<MessagingName> traversed = new HashSet<>();
        todo.add(root);

        while (!todo.isEmpty()) {
            MessagingName curr = todo.poll();
            if (traversed.contains(curr)) {
                continue;
            }
            traversed.add(curr);
            consumer.accept(curr);
            for (MessagingName parent : curr.parents) {
                todo.offer(parent);
            }
        }
    }

    public synchronized String[] getNameIncidents(Name name) {
        if (incidentMappings == null) {
            calculateNameIncidents();
        }
        MessagingName mn = getName(name);
        if (mn == null) {
            Log.e(TAG, "getNameIncidents: cannot find name %s", name);
            return new String[0];
        }
        HashSet<MessagingName> incidents = incidentMappings.get(mn);
        if (incidents == null) {
            // no incidents
            return new String[0];
        }
        String[] ret = new String[incidents.size()];
        int i = 0;
        for (MessagingName incident : incidents) {
            ret[i++] = incident.getAppName();
        }
        return ret;
    }


    //    public Tuple3<Collection<GraphNode>, Integer, Integer> instantiateTemplate(int templateId, String incidentName) {
//        throw new UnsupportedOperationException();
//    }
//
//    public Collection<GraphNode> removeIncidentTree(int incidentRootId) {
//        throw new UnsupportedOperationException();
//    }
//
    public synchronized MessagingName createName(@NonNull String appName, @NonNull MessagingNameType type, @NonNull String initiator) {
        MessagingName mn = innerCreateName(appName, type, initiator);
        ArrayList<MessagingName> nas = new ArrayList<>();
        nas.add(mn);
        notifyNamespaceEvent(nas, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), initiator);
        return mn;
    }

    public synchronized MessagingName createNameWithParent(@NonNull String appName, @NonNull MessagingNameType type, @NonNull Name parentName, @NonNull String initiator) {
        MessagingName mn = innerCreateName(appName, type, initiator);
        ArrayList<MessagingName> nas = new ArrayList<>();
        nas.add(mn);
        ArrayList<Tuple2<Name, Name>> ras = new ArrayList<>();
        if (innerCreateRelationship(parentName, mn.getName(), initiator)) {
            ras.add(new Tuple2<>(parentName, mn.getName()));
        }
        notifyNamespaceEvent(nas, new ArrayList<>(), ras, new ArrayList<>(), initiator);
        return mn;
    }

    public synchronized boolean createRelationship(@NonNull Name parentName, @NonNull Name childName, @NonNull String initiator) {
        if (innerCreateRelationship(parentName, childName, initiator)) {
            ArrayList<Tuple2<Name, Name>> ras = new ArrayList<>();
            ras.add(new Tuple2<>(parentName, childName));
            notifyNamespaceEvent(new ArrayList<>(), new ArrayList<>(), ras, new ArrayList<>(), initiator);
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    private MessagingName innerCreateName(@NonNull Name name, @NonNull String appName, @NonNull MessagingNameType type, @NonNull String initiator) {
        MessagingName mn = new MessagingName(name, appName, type);
        if (NetLayer.registerName(name, true, initiator)) {
            nameMappings.put(name, mn);
            Log.d(TAG, "%s create name %s", initiator, mn);
            return mn;
        } else {
            Log.e(TAG, "%s create name %s failed, net layer already has the name!", initiator, mn);
            Helper.notifyUser(LogType.Warn, "%s create name %s failed, net layer already has the name!", initiator, mn);
            return null;
        }
    }

    @NonNull
    private MessagingName innerCreateName(@NonNull String appName, @NonNull MessagingNameType type, @NonNull String initiator) {
        Name name = NetLayer.registerRandomName(initiator);
        MessagingName mn = new MessagingName(name, appName, type);
        nameMappings.put(name, mn);
        Log.d(TAG, "%s create name %s", initiator, mn);
        return mn;
    }

    private boolean innerCreateRelationship(@NonNull Name parentName, @NonNull Name childName, @NonNull String initiator) {
        MessagingName parent = nameMappings.get(parentName);
        if (parent == null) {
            Log.e(TAG, "%s create relationship %s->%s failed, parent does not exist", initiator, parentName, childName);
            Helper.notifyUser(LogType.Warn, "%s create relationship %s->%s failed, parent does not exist", initiator, parentName, childName);
            return false;
        }
        MessagingName child = nameMappings.get(childName);
        if (child == null) {
            Log.e(TAG, "%s create relationship %s->%s failed, child does not exist", initiator, parentName, childName);
            Helper.notifyUser(LogType.Warn, "%s create relationship %s->%s failed, child does not exist", initiator, parentName, childName);
            return false;
        }
        if (parent.children.add(child)) {
            child.parents.add(parent);
            NetLayer.registerRelationship(parentName, childName, true, initiator);
            clearNameIncidents();
            Log.d(TAG, "%s create relationship %s->%s", initiator, parent, child);
            return true;
        } else {
            Log.e(TAG, "%s create relationship %s->%s failed, relationship already exists", initiator, parent, child);
            Helper.notifyUser(LogType.Warn, "%s create relationship %s->%s failed, relationship already exists", initiator, parent, child);
            return false;
        }
    }

    public synchronized void removeName(@NonNull Name name, @NonNull String initiator) {
        if (innerRemoveName(name, initiator)) {
            ArrayList<Name> nds = new ArrayList<>();
            nds.add(name);
            notifyNamespaceEvent(new ArrayList<>(), nds, new ArrayList<>(), new ArrayList<>(), initiator);
        }
    }

    private boolean innerRemoveName(@NonNull Name name, @NonNull String initiator) {
        MessagingName mn = nameMappings.remove(name);
        if (mn == null) {
            Log.e(TAG, "%s remove name %s failed, name does not exist", initiator, name);
            Helper.notifyUser(LogType.Warn, "%s remove name %s failed, name does not exist", initiator, name);
            return false;
        }
        // clear relationships
        for (MessagingName parent : mn.parents) {
            parent.children.remove(mn);
        }
        for (MessagingName child : mn.children) {
            child.parents.remove(mn);
        }
        Log.d(TAG, "%s remove name %s", initiator, mn);
        NetLayer.registerName(name, false, initiator);
        clearNameIncidents();
        return true;
    }

    public synchronized void removeRelationship(@NonNull Name parentName, @NonNull Name childName, @NonNull String initiator) {
        if (innerRemoveRelationship(parentName, childName, initiator)) {
            ArrayList<Tuple2<Name, Name>> rds = new ArrayList<>();
            rds.add(new Tuple2<>(parentName, childName));
            notifyNamespaceEvent(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), rds, initiator);
        }
    }

    private boolean innerRemoveRelationship(@NonNull Name parentName, @NonNull Name childName, @NonNull String initiator) {
        MessagingName parent = nameMappings.get(parentName);
        if (parent == null) {
            Log.e(TAG, "%s remove relationship %s->%s failed, parent does not exist", initiator, parentName, childName);
            Helper.notifyUser(LogType.Warn, "%s remove relationship %s->%s failed, parent does not exist", initiator, parentName, childName);
            return false;
        }
        MessagingName child = nameMappings.get(childName);
        if (child == null) {
            Log.e(TAG, "%s remove relationship %s->%s failed, child does not exist", initiator, parentName, childName);
            Helper.notifyUser(LogType.Warn, "%s remove relationship %s->%s failed, child does not exist", initiator, parentName, childName);
            return false;
        }
        if (parent.children.remove(child)) {
            child.parents.remove(parent);
            NetLayer.registerRelationship(parentName, childName, false, initiator);
            clearNameIncidents();
            Log.d(TAG, "%s remove relationship %s->%s", initiator, parentName, parent, child);
            return true;
        } else {
            Log.e(TAG, "%s remove relationship %s->%s failed, relationship does not exist", initiator, parent, child);
            Helper.notifyUser(LogType.Warn, "%s remove relationship %s->%s failed, relationship does not exist", initiator, parent, child);
            return false;
        }
    }

    public synchronized MessagingName updateAppName(@NonNull Name name, @NonNull String appName, @NonNull String initiator) {
        MessagingName mn = nameMappings.get(name);
        if (mn == null) {
            Log.e(TAG, "%s updateName, %s(->%s) failed, name does not exist ", initiator, name, appName);
            return null;
        }
        String origAppName = mn.getAppName();
        if (origAppName.equals(appName)) {
            Log.d(TAG, "%s updateName, %s. orig name the same as new name, ignore", initiator, mn);
            return mn;
        }
        mn.setAppName(appName);
        Log.d(TAG, "%s update Name, %s, origName=%s", initiator, mn, origAppName);
        notifyAppNameEvent(mn, initiator);
        return mn;
    }

    private void clearNameIncidents() {
        incidentMappings = null;
    }

    private void calculateNameIncidents() {
        if (incidentMappings != null) {
            return;
        }
        incidentMappings = new HashMap<>();
        MessagingName incidentRoot = getName(Constants.getIncidentRoot());
        assert incidentRoot != null;
        forEachChild(incidentRoot, incident ->
                forEachDescendant(incident, descendant -> {
                    HashSet<MessagingName> descendentIncidents = incidentMappings.get(descendant);
                    if (descendentIncidents == null) {
                        incidentMappings.put(descendant, descendentIncidents = new HashSet<>());
                    }
                    descendentIncidents.add(incident);
                })
        );
    }

    private synchronized void onMessageReceivedFromListenChannel(@NonNull Name src, @NonNull Name dst, @NonNull byte[] buf, @NonNull String initiator) {
        // ignore the messages that are 1) not on the listen channel, or 2) sent by myself
        if (!dst.equals(Constants.getDefaultListenName()) || INITIATOR_APP.equals(initiator)) {
            return;
        }
        // should not use the functions that notifies the net layer, write our own logic here
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        byte type = buffer.get();
        switch (type) {
            case DATA_TYPE_GRAPH_CHANGED:
                onNamespaceEventGotFromNet(buffer);
                break;
            case DATA_TYPE_APPNAME_CHANGED:
                onAppNameEventGotFromNet(buffer);
                break;
            default:
                Log.e(TAG, "Unknown type %02X", type);
                break;
        }
    }

    private void notifyNamespaceEvent(@NonNull ArrayList<MessagingName> nas,
                                      @NonNull ArrayList<Name> nds,
                                      @NonNull ArrayList<Tuple2<Name, Name>> ras,
                                      @NonNull ArrayList<Tuple2<Name, Name>> rds, String initiator) {
        // notify application
        // skip notification when update buffer presents, it will be eventually added to the event buffer
        if (updateBuffer == null) {
            Context.getContext(CONTEXT_MESSAGINGNAMESPACE).sendBroadcast(
                    new Intent(ACTION_NAMESPACE_CHANGED)
                            .putExtra(EXTRA_NAMES_ADDED, nas)
                            .putExtra(EXTRA_NAMES_REMOVED, nds)
                            .putExtra(EXTRA_RELATIONSHIPS_ADDED, ras)
                            .putExtra(EXTRA_RELATIONSHIPS_REMOVED, rds)
            );
        }
        // save message
        if (!INITIATOR_INIT.equals(initiator) && !INITIATOR_NET.equals(initiator)) {
            byte[][] naNames = new byte[nas.size()][];
            int size = 1 // type
                    + Helper.INTEGER_SIZE * 4 // nas.length, nds.length, ras.length, rds.length
                    + Name.WRITE_SIZE * nds.size() // nds
                    + Name.WRITE_SIZE * 2 * ras.size() // ras
                    + Name.WRITE_SIZE * 2 * rds.size() // rds
                    + (Name.WRITE_SIZE + 1 + Helper.INTEGER_SIZE) * nas.size(); // na.names, na.types, na.nameLengths
            for (int i = 0; i < nas.size(); i++) {
                naNames[i] = nas.get(i).getAppName().getBytes(DEFAULT_CHARSET);
                size += naNames[i].length;
            }

            ByteBuffer buffer = ByteBuffer.allocate(size);
            buffer.put(DATA_TYPE_GRAPH_CHANGED);

            // write nas
            buffer.putInt(nas.size());
            for (int i = 0; i < nas.size(); i++) {
                MessagingName na = nas.get(i);
                na.getName().write(buffer);
                buffer.put(na.getType().getRepresent());
                buffer.putInt(naNames[i].length);
                buffer.put(naNames[i]);
            }

            // write nds
            buffer.putInt(nds.size());
            for (Name nd : nds) {
                nd.write(buffer);
            }

            // write ras
            buffer.putInt(ras.size());
            for (Tuple2<Name, Name> ra : ras) {
                ra.getV1().write(buffer);
                ra.getV2().write(buffer);
            }

            // write rds
            buffer.putInt(rds.size());
            for (Tuple2<Name, Name> rd : rds) {
                rd.getV1().write(buffer);
                rd.getV2().write(buffer);
            }
            NetLayer.sendData(Constants.getDefaultListenName(), Constants.getDefaultListenName(), buffer.array(), true, INITIATOR_APP);
        }
    }

    private void onNamespaceEventGotFromNet(@NonNull ByteBuffer buffer) {
        ArrayList<MessagingName> nas = new ArrayList<>();
        ArrayList<Name> nds = new ArrayList<>();
        ArrayList<Tuple2<Name, Name>> ras = new ArrayList<>();
        ArrayList<Tuple2<Name, Name>> rds = new ArrayList<>();
        // read nas
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "onNamespaceEventGotFromNet: buffer size too small for nas.size");
            return;
        }
        int naSize = buffer.getInt();
        for (int i = 0; i < naSize; i++) {
            Name name = Name.read(buffer);
            if (name == null) {
                Log.e(TAG, "onNamespaceEventGotFromNet: failed in reading name, i=%d, naSize=%d", i, naSize);
                return;
            }
            byte bType = buffer.get();
            MessagingNameType type = MessagingNameType.fromByte(bType);
            if (type == null) {
                Log.e(TAG, "onNamespaceEventGotFromNet: illegal messaging type %d, i=%d, naSize=%d", bType, i, naSize);
                return;
            }
            if (buffer.remaining() < Helper.INTEGER_SIZE) {
                Log.e(TAG, "onNamespaceEventGotFromNet: buffer size too small for na[%d].appName.size, naSize=%d", i, naSize);
                return;
            }
            int strLen = buffer.getInt();
            byte[] bAppName = new byte[strLen];
            buffer.get(bAppName);
            String appName = new String(bAppName, DEFAULT_CHARSET);
            nas.add(new MessagingName(name, appName, type));
        }

        // read nds
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "onNamespaceEventGotFromNet: buffer size too small for nds.size");
            return;
        }
        int ndSize = buffer.getInt();
        if (buffer.remaining() < Name.WRITE_SIZE * ndSize) {
            Log.e(TAG, "onNamespaceEventGotFromNet: ndSize=%d, remaining=%d < %d", ndSize, buffer.remaining(), Name.WRITE_SIZE * ndSize);
            return;
        }
        for (int i = 0; i < ndSize; i++) {
            nds.add(Name.read(buffer));
        }

        // read ras
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "onNamespaceEventGotFromNet: buffer size too small for ras.size");
            return;
        }
        int raSize = buffer.getInt();
        if (buffer.remaining() < Name.WRITE_SIZE * 2 * raSize) {
            Log.e(TAG, "onNamespaceEventGotFromNet: raSize=%d, remaining=%d < %d", ndSize, buffer.remaining(), Name.WRITE_SIZE * 2 * raSize);
            return;
        }
        for (int i = 0; i < raSize; i++) {
            Name parent = Name.read(buffer);
            Name child = Name.read(buffer);
            ras.add(new Tuple2<>(parent, child));
        }

        // read rds
        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "onNamespaceEventGotFromNet: buffer size too small for rds.size");
            return;
        }
        int rdSize = buffer.getInt();
        if (buffer.remaining() < Name.WRITE_SIZE * 2 * rdSize) {
            Log.e(TAG, "onNamespaceEventGotFromNet: rdSize=%d, remaining=%d < %d", ndSize, buffer.remaining(), Name.WRITE_SIZE * 2 * rdSize);
            return;
        }
        for (int i = 0; i < rdSize; i++) {
            Name parent = Name.read(buffer);
            Name child = Name.read(buffer);
            rds.add(new Tuple2<>(parent, child));
        }

        startBuffer();
        // update data structure
//        ArrayList<MessagingName> effectiveNas = new ArrayList<>();
//        ArrayList<Name> effectiveNds = new ArrayList<>();
//        ArrayList<Tuple2<Name, Name>> effectiveRas = new ArrayList<>();
//        ArrayList<Tuple2<Name, Name>> effectiveRds = new ArrayList<>();

        boolean needClearNameIncidents = false;

        for (MessagingName na : nas) {
            if (nameMappings.containsKey(na.getName())) {
                Log.e(TAG, "onNamespaceEventGotFromNet: Name %s already exists, cannot add", na);
                continue;
            }
            nameMappings.put(na.getName(), na);
            Log.d(TAG, "onNamespaceEventGotFromNet: add name %s", na);
//            effectiveNas.add(na);
        }

        for (Name nd : nds) {
            MessagingName mn = nameMappings.remove(nd);
            if (mn == null) {
                Log.e(TAG, "onNamespaceEventGotFromNet: Name %s does not exist, cannot remove", nd);
                continue;
            }
            // clear relationships
            for (MessagingName parent : mn.parents) {
                parent.children.remove(mn);
            }
            for (MessagingName child : mn.children) {
                child.parents.remove(mn);
            }
            needClearNameIncidents = true;
            Log.d(TAG, "onNamespaceEventGotFromNet: remove name %s", mn);
//            effectiveNds.add(nd);
        }

        for (Tuple2<Name, Name> ra : ras) {
            MessagingName parent = nameMappings.get(ra.getV1());
            if (parent == null) {
                Log.e(TAG, "onNamespaceEventGotFromNet: [ra] parent %s does not exist", ra.getV1());
                continue;
            }
            MessagingName child = nameMappings.get(ra.getV2());
            if (child == null) {
                Log.e(TAG, "onNamespaceEventGotFromNet: [ra] child %s does not exist", ra.getV2());
                continue;
            }
            if (!parent.children.add(child)) {
                Log.e(TAG, "onNamespaceEventGotFromNet: relationship %s->%s already exists", parent, child);
                continue;
            }
            child.parents.add(parent);
            needClearNameIncidents = true;
            Log.d(TAG, "onNamespaceEventGotFromNet: create relationship %s->%s", parent, child);
//            effectiveRas.add(ra);
        }

        for (Tuple2<Name, Name> rd : rds) {
            MessagingName parent = nameMappings.get(rd.getV1());
            if (parent == null) {
                Log.e(TAG, "onNamespaceEventGotFromNet: [rd] parent %s does not exist", rd.getV1());
                continue;
            }
            MessagingName child = nameMappings.get(rd.getV2());
            if (child == null) {
                Log.e(TAG, "onNamespaceEventGotFromNet: [rd] child %s does not exist", rd.getV2());
                continue;
            }
            if (!parent.children.remove(child)) {
                Log.e(TAG, "onNamespaceEventGotFromNet: [rd] relationship %s->%s does not exist", parent, child);
                continue;
            }
            child.parents.remove(parent);
            needClearNameIncidents = true;
            Log.d(TAG, "onNamespaceEventGotFromNet: remove relationship %s->%s", parent, child);
//            effectiveRas.add(rd);
        }

        if (needClearNameIncidents) {
            clearNameIncidents();
        }

        // notify the applications (no need to do it now, in the buffer)
//        Context.getContext(CONTEXT_MESSAGINGNAMESPACE).sendBroadcast(
//                new Intent(ACTION_NAMESPACE_CHANGED)
//                        .putExtra(EXTRA_NAMES_ADDED, effectiveNas)
//                        .putExtra(EXTRA_NAMES_REMOVED, effectiveNds)
//                        .putExtra(EXTRA_RELATIONSHIPS_ADDED, effectiveRas)
//                        .putExtra(EXTRA_RELATIONSHIPS_REMOVED, effectiveRds)
//        );
    }

    private void notifyAppNameEvent(@NonNull MessagingName name, @NonNull String initiator) {
        // notify application
        // skip notification when update buffer presents, it will be eventually added to the event buffer
        if (updateBuffer == null) {

            Context.getContext(CONTEXT_MESSAGINGNAMESPACE).sendBroadcast(
                    new Intent(ACTION_APPNAME_CHANGED)
                            .putExtra(EXTRA_NAME, name)
            );
        }

        // write to the network
        if (!INITIATOR_INIT.equals(initiator) && !INITIATOR_NET.equals(initiator)) {
            byte[] nameBytes = name.getAppName().getBytes(DEFAULT_CHARSET);
            int size = 1 // type
                    + Name.WRITE_SIZE // name
                    + Helper.INTEGER_SIZE // appName.length
                    + nameBytes.length; // appName            
            ByteBuffer buffer = ByteBuffer.allocate(size);
            buffer.put(DATA_TYPE_APPNAME_CHANGED);
            name.getName().write(buffer);
            buffer.putInt(nameBytes.length);
            buffer.put(nameBytes);
            NetLayer.sendData(Constants.getDefaultListenName(), Constants.getDefaultListenName(), buffer.array(), true, INITIATOR_APP);
        }
    }

    private void onAppNameEventGotFromNet(ByteBuffer buffer) {
        Name n = Name.read(buffer);
        if (n == null) {
            Log.e(TAG, "onAppNameEventGotFromNet: failed in reading name");
            return;
        }
        MessagingName mn = getName(n);
        if (mn == null) {
            Log.e(TAG, "onAppNameEventGotFromNet: name %s does not exist", n);
            return;
        }

        if (buffer.remaining() < Helper.INTEGER_SIZE) {
            Log.e(TAG, "onAppNameEventGotFromNet: failed in getting appName size");
            return;
        }
        int size = buffer.getInt();
        if (buffer.remaining() != size) {
            Log.e(TAG, "onAppNameEventGotFromNet: remaining (%d) != size (%d)", buffer.remaining(), size);
            return;
        }
        byte[] nameBytes = new byte[size];
        buffer.get(nameBytes);
        String newName = new String(nameBytes, DEFAULT_CHARSET);
        String origName = mn.getAppName();

        startBuffer();
        mn.setAppName(newName);
        Log.d(TAG, "onAppNameEventGotFromNet: name=%s, origName=%s", mn, origName);

        // notify the applications (no need to do it now, in the buffer)
//        Context.getContext(CONTEXT_MESSAGINGNAMESPACE).sendBroadcast(
//                new Intent(ACTION_APPNAME_CHANGED)
//                        .putExtra(EXTRA_NAME, mn)
//        );
    }

    private MessagingNamespaceBuffer updateBuffer = null;

    private synchronized void startBuffer() {
        if (updateBuffer != null) return;
        updateBuffer = new MessagingNamespaceBuffer();
        DelayRunner.getDefaultInstance().postDelayed(MESSAGE_BUFFER_FLUSH_DELAY, this::flushBuffer);
    }

    private synchronized void flushBuffer() {
        if (updateBuffer == null) return;
        updateBuffer.flush();
        updateBuffer = null;
    }
}

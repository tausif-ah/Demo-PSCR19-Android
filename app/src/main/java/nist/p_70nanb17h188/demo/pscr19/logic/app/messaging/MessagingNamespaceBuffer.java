package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.logic.Tuple2;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;


class MessagingNamespaceBuffer {
    private boolean flushed = false;
    private HashMap<Name, MessagingNamespace.MessagingName> namesBeforeBuffer = new HashMap<>();
    private HashMap<Name, HashSet<Name>> relationshipsBeforeBuffer = new HashMap<>();

    MessagingNamespaceBuffer() {
        MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
        for (MessagingNamespace.MessagingName mn : namespace.getAllNames()) {
            // add names
            namesBeforeBuffer.put(mn.getName(), new MessagingNamespace.MessagingName(mn.getName(), mn.getAppName(), mn.getType()));
            // add relationships
            HashSet<Name> children = new HashSet<>();
            relationshipsBeforeBuffer.put(mn.getName(), children);
            namespace.forEachChild(mn, child -> children.add(child.getName()));
        }
    }

    synchronized void flush() {
        if (flushed) return;
        flushed = true;

        MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();

        ArrayList<MessagingNamespace.MessagingName> nas = new ArrayList<>();
        ArrayList<Name> nds = new ArrayList<>();
        ArrayList<Tuple2<Name, Name>> ras = new ArrayList<>();
        ArrayList<Tuple2<Name, Name>> rds = new ArrayList<>();
        ArrayList<MessagingNamespace.MessagingName> nameChanges = new ArrayList<>();

        for (MessagingNamespace.MessagingName newName : namespace.getAllNames()) {
            MessagingNamespace.MessagingName origName = namesBeforeBuffer.remove(newName.getName());
            if (origName == null) { // newly added name
                nas.add(newName);
                namespace.forEachChild(newName, child -> ras.add(new Tuple2<>(newName.getName(), child.getName())));
            } else { // existing name
                HashSet<Name> origChildren = relationshipsBeforeBuffer.get(newName.getName());
                assert origChildren != null;
                if (!origName.getAppName().equals(newName.getAppName())) // appName changed
                    nameChanges.add(newName);
                namespace.forEachChild(newName, child -> {
                    if (!origChildren.remove(child.getName())) { // new relationship
                        ras.add(new Tuple2<>(newName.getName(), child.getName()));
                    }
                });
                for (Name origChild : origChildren) { // deleted relationships
                    rds.add(new Tuple2<>(newName.getName(), origChild));
                }
            }
        }
        for (MessagingNamespace.MessagingName origName : namesBeforeBuffer.values()) { // deleted names
            nds.add(origName.getName());
        }


        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).sendBroadcast(
                new Intent(MessagingNamespace.ACTION_NAMESPACE_CHANGED)
                        .putExtra(MessagingNamespace.EXTRA_NAMES_ADDED, nas)
                        .putExtra(MessagingNamespace.EXTRA_NAMES_REMOVED, nds)
                        .putExtra(MessagingNamespace.EXTRA_RELATIONSHIPS_ADDED, ras)
                        .putExtra(MessagingNamespace.EXTRA_RELATIONSHIPS_REMOVED, rds)
        );
        for (MessagingNamespace.MessagingName changedName : nameChanges) {
            Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).sendBroadcast(
                    new Intent(MessagingNamespace.ACTION_APPNAME_CHANGED)
                            .putExtra(MessagingNamespace.EXTRA_NAME, changedName)
            );
        }

    }
}

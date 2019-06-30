package nist.p_70nanb17h188.demo.pscr19.logic.app.messaging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.Consumer;
import nist.p_70nanb17h188.demo.pscr19.logic.Tuple2;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class Template {
    private static final String TAG = "Template";
    private static final HashMap<Integer, Template> ALL_TEMPLATES = new HashMap<>();

    public static void forEachTemplate(@NonNull Consumer<Template> consumer) {
        synchronized (ALL_TEMPLATES) {
            for (Template template : ALL_TEMPLATES.values()) {
                consumer.accept(template);
            }
        }
    }

    @Nullable
    public static Template getTemplate(Integer id) {
        synchronized (ALL_TEMPLATES) {
            return ALL_TEMPLATES.get(id);
        }
    }

    private final int id;
    @NonNull
    private final String name;
    @NonNull
    private final Name rootNode;
    @NonNull
    private final Name commanderNode;
    private final HashMap<Name, MessagingNamespace.MessagingName> names = new HashMap<>();
    private final HashMap<Name, HashSet<Name>> relationships = new HashMap<>();

    public Template(@NonNull String name, @NonNull Name rootNode, @NonNull Name commanderNode,
                    @NonNull HashSet<MessagingNamespace.MessagingName> names,
                    @NonNull Collection<Tuple2<Name, Name>> relationships) {
        this.name = name;
        this.rootNode = rootNode;
        this.commanderNode = commanderNode;
        MessagingNamespace.MessagingName rootMn = null, commanderMn = null;
        for (MessagingNamespace.MessagingName mn : names) {
            this.names.put(mn.getName(), mn);
            this.relationships.put(mn.getName(), new HashSet<>());
            if (mn.getName().equals(rootNode)) rootMn = mn;
            if (mn.getName().equals(commanderNode)) commanderMn = mn;
        }
        if (rootMn == null) {
            Log.e(TAG, "create: Root node %s not contained in the template %s", rootNode, name);
        }
        if (commanderMn == null) {
            Log.e(TAG, "create: Commander node %s not contained in the template %s", rootNode, name);
        }
        for (Tuple2<Name, Name> relationship : relationships) {
            Name parentName = relationship.getV1();
            Name childName = relationship.getV2();
            HashSet<Name> myChildrenNames = this.relationships.get(parentName);
            if (myChildrenNames == null) {
                Log.e(TAG, "create: Template %s does not have parent name in relationship %s->%s", name, parentName, childName);
                continue;
            }
            if (!this.names.containsKey(childName)) {
                Log.e(TAG, "create: Template %s does not have child name %s", name, childName);
                continue;
            }
            myChildrenNames.add(childName);
        }

        // get a unique id
        synchronized (ALL_TEMPLATES) {
            int id;
            do {
                id = Helper.DEFAULT_RANDOM.nextInt();
            } while (ALL_TEMPLATES.containsKey(id));
            this.id = id;
            ALL_TEMPLATES.put(id, this);
        }
        Log.d(TAG, "Created template, id=%d, name=%s, root=%s commander=%s", id, name,
                rootMn == null ? rootNode.toString() : rootMn.toString(),
                commanderMn == null ? commanderNode.toString() : commanderMn.toString());
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public Name getRootNode() {
        return rootNode;
    }

    @NonNull
    public Name getCommanderNode() {
        return commanderNode;
    }

    @Nullable
    public MessagingNamespace.MessagingName getName(@NonNull Name name) {
        return names.get(name);
    }

    public void forEachName(@NonNull Consumer<MessagingNamespace.MessagingName> consumer) {
        for (MessagingNamespace.MessagingName mn : names.values()) {
            consumer.accept(mn);
        }
    }

    public void forEachChild(@NonNull MessagingNamespace.MessagingName parent, @NonNull Consumer<MessagingNamespace.MessagingName> consumer) {
        HashSet<Name> childrenNames = relationships.get(parent.getName());
        if (childrenNames == null) {
            Log.e(TAG, "create: Template %s does not have parent name %s", name, parent);
            return;
        }
        for (Name childName : childrenNames) {
            MessagingNamespace.MessagingName childMn = names.get(childName);
            assert childMn != null;
            consumer.accept(childMn);
        }
    }
}

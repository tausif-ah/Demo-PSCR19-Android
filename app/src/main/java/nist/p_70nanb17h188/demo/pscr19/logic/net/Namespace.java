package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;

public class Namespace {
    public static final String EXTRA_ADDED = "added";

    /**
     * The context for namespace events.
     */
    public static final String CONTEXT_NAMESPACE = "nist.p_70nanb17h188.demo.pscr19.logic.net.namespace";

    /**
     * Broadcast intent action indicating that there is a name added/removed.
     * One extra {@link #EXTRA_NAME} ({@link Name}) indicates the target name.
     * Another extra {@link #EXTRA_ADDED} ({@link Boolean} indicates weather the name is added (true) or remove d(false).
     * <p>
     * The current names can be iterated through {@link #forEachName(NameConsumer)}.
     */
    public static final String ACTION_NAME_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.Namespace.nameChanged";
    public static final String EXTRA_NAME = "name";

    /**
     * Broadcast intent action indicating that there is a relataionship added/removed.
     * One extra {@link #EXTRA_PARENT} ({@link Name}) indicates the parent of the relationship.
     * Another extra {@link #EXTRA_CHILD} ({@link Name}) indicates the child of the relationship.
     * A third extra {@link #EXTRA_ADDED} ({@link Boolean} indicates weather the relationsihp is added (true) or remove d(false).
     * <p>
     * The current relationships can be iterated through function {@link #forEachChild(Name, NameConsumer)} and {@link #forEachParent(Name, NameConsumer)}.
     */
    public static final String ACTION_RELATIONSHIP_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.Namespace.relationshipChanged";
    public static final String EXTRA_PARENT = "parent";
    public static final String EXTRA_CHILD = "child";

    public interface NameConsumer {

        void accept(@NonNull Name name);
    }

    public static class LoopDetectedException extends Exception {
        private final Name[] loopNames;

        public LoopDetectedException(Name[] loopNames) {
            this.loopNames = loopNames;
        }

        public Name[] getLoopNames() {
            return loopNames;
        }
    }

    private final HashMap<Name, NameWithRelationship> allNames = new HashMap<>();

    public synchronized boolean hasName(@NonNull Name name) {
        return allNames.containsKey(name);
    }

    private NameWithRelationship innerAddName(@NonNull Name name) {
        NameWithRelationship ret = allNames.get(name);
        if (ret == null) {
            allNames.put(name, new NameWithRelationship(name));
            Context.getContext(CONTEXT_NAMESPACE).sendBroadcast(new Intent(ACTION_NAME_CHANGED).putExtra(EXTRA_NAME, name).putExtra(EXTRA_ADDED, true));
        }
        return ret;
    }

    public synchronized void addName(@NonNull Name name) {
        innerAddName(name);
    }

    public synchronized void removeName(@NonNull Name name) {
        NameWithRelationship n = allNames.get(name);
        if (n != null) {
            n.clearRelationships();
            allNames.remove(name);
            Context.getContext(CONTEXT_NAMESPACE).sendBroadcast(new Intent(ACTION_NAME_CHANGED).putExtra(EXTRA_NAME, name).putExtra(EXTRA_ADDED, false));
        }
    }

    public synchronized boolean addRelationship(@NonNull Name parent, @NonNull Name child) {
        NameWithRelationship p = innerAddName(parent);
        NameWithRelationship c = innerAddName(child);
        if (p.addChild(c)) {
            Context.getContext(CONTEXT_NAMESPACE).sendBroadcast(new Intent(ACTION_RELATIONSHIP_CHANGED).putExtra(EXTRA_PARENT, parent).putExtra(EXTRA_CHILD, child).putExtra(EXTRA_ADDED, true));
            return true;
        }
        return false;
    }

    public synchronized boolean removeRelationship(@NonNull Name parent, @NonNull Name child) {
        NameWithRelationship p = allNames.get(parent);
        NameWithRelationship c = allNames.get(child);
        if (p == null || c == null || !p.removeChild(c))
            return false;
        Context.getContext(CONTEXT_NAMESPACE).sendBroadcast(new Intent(ACTION_RELATIONSHIP_CHANGED).putExtra(EXTRA_PARENT, parent).putExtra(EXTRA_CHILD, child).putExtra(EXTRA_ADDED, false));
        return true;
    }

    public synchronized void forEachName(@NonNull NameConsumer consumer) {
        for (NameWithRelationship value : allNames.values()) {
            consumer.accept(value.name);
        }
    }

    public synchronized void forEachChild(@NonNull Name parent, @NonNull NameConsumer consumer) {
        NameWithRelationship p = allNames.get(parent);
        if (p != null) {
            p.forEachChild(consumer);
        }
    }

    public synchronized void forEachParent(@NonNull Name child, @NonNull NameConsumer consumer) {
        NameWithRelationship c = allNames.get(child);
        if (c != null) {
            c.forEachParent(consumer);
        }
    }

    public synchronized void forEachDescendant(@NonNull Name root, @NonNull NameConsumer consumer) {
        NameWithRelationship r = allNames.get(root);
        if (r != null) {
            r.forEachDescendant(consumer);
        }
    }

    public synchronized void forEachAncestor(@NonNull Name leaf, @NonNull NameConsumer consumer) {
        NameWithRelationship l = allNames.get(leaf);
        if (l != null) {
            l.forEachAncestor(consumer);
        }
    }

    public synchronized boolean isChild(@NonNull Name parent, @NonNull Name child) {
        NameWithRelationship p = allNames.get(parent);
        NameWithRelationship c = allNames.get(child);
        return p != null && c != null && p.hasChild(c);
    }

    public synchronized boolean isDescendant(@NonNull Name root, @NonNull Name descendant) {
        NameWithRelationship r = allNames.get(root);
        NameWithRelationship d = allNames.get(descendant);

        return r != null && d != null && r.hasDescendant(d);
    }

    /**
     * Perform a topological sort in the namespace.
     * The parent name would always appear earlier than its children in the result list.
     * If there is a loop in the namespace, {@link LoopDetectedException} will be thwon, the {@link LoopDetectedException#getLoopNames()} will contain the loop detected.
     *
     * @return the topological sort result of the namespace.
     * @throws LoopDetectedException if there is a loop in the namespace.
     */
    public synchronized ArrayList<Name> topologicalSort() throws LoopDetectedException {
        HashSet<NameWithRelationship> permanentMarks = new HashSet<>();
        HashMap<NameWithRelationship, Integer> tempMarks = new HashMap<>();
        ArrayList<NameWithRelationship> visitOrder = new ArrayList<>();
        ArrayList<Name> ret = new ArrayList<>();
        for (NameWithRelationship n : allNames.values()) {
            n.topologicalVisit(tempMarks, permanentMarks, visitOrder, ret);
        }
        return ret;
    }

    private static class NameWithRelationship {

        @NonNull
        private final Name name;
        private final HashSet<NameWithRelationship> children = new HashSet<>();
        private final HashSet<NameWithRelationship> parents = new HashSet<>();

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NameWithRelationship other = (NameWithRelationship) obj;
            return name.equals(other.name);
        }

        NameWithRelationship(@NonNull Name name) {
            this.name = name;
        }

        void clearRelationships() {
            for (NameWithRelationship child : children) {
                child.parents.remove(this);
            }
            children.clear();
            for (NameWithRelationship parent : parents) {
                parent.children.remove(this);
            }
            parents.clear();
        }

        boolean addChild(@NonNull NameWithRelationship child) {
            if (children.add(child)) {
                child.parents.add(this);
                return true;
            } else {
                return false;
            }
        }

        boolean removeChild(@NonNull NameWithRelationship child) {
            if (children.remove(child)) {
                child.parents.remove(this);
                return true;
            } else {
                return false;
            }
        }

        void forEachChild(@NonNull NameConsumer consumer) {
            for (NameWithRelationship child : children) {
                consumer.accept(child.name);
            }
        }

        void forEachParent(@NonNull NameConsumer consumer) {
            for (NameWithRelationship parent : parents) {
                consumer.accept(parent.name);
            }
        }

        void forEachAncestor(@NonNull NameConsumer consumer) {
            ArrayDeque<NameWithRelationship> todo = new ArrayDeque<>();
            HashSet<NameWithRelationship> traversed = new HashSet<>();
            todo.add(this);

            while (!todo.isEmpty()) {
                NameWithRelationship curr = todo.poll();
                if (traversed.contains(curr)) {
                    continue;
                }
                traversed.add(curr);
                consumer.accept(curr.name);
                for (NameWithRelationship parent : curr.parents) {
                    todo.offer(parent);
                }
            }
        }

        void forEachDescendant(@NonNull NameConsumer consumer) {
            ArrayDeque<NameWithRelationship> todo = new ArrayDeque<>();
            HashSet<NameWithRelationship> traversed = new HashSet<>();
            todo.add(this);

            while (!todo.isEmpty()) {
                NameWithRelationship curr = todo.poll();
                if (traversed.contains(curr)) {
                    continue;
                }
                traversed.add(curr);
                consumer.accept(curr.name);
                for (NameWithRelationship child : curr.children) {
                    todo.offer(child);
                }
            }
        }

        boolean hasChild(@NonNull NameWithRelationship child) {
            return children.contains(child);
        }

        boolean hasDescendant(@NonNull NameWithRelationship n) {
            ArrayDeque<NameWithRelationship> todo = new ArrayDeque<>();
            HashSet<NameWithRelationship> traversed = new HashSet<>();
            todo.add(this);

            while (!todo.isEmpty()) {
                NameWithRelationship curr = todo.poll();
                if (curr == n) return true;
                if (traversed.contains(curr)) {
                    continue;
                }
                traversed.add(curr);
                for (NameWithRelationship child : curr.children) {
                    todo.offer(child);
                }
            }
            return false;
        }

        void topologicalVisit(@NonNull HashMap<NameWithRelationship, Integer> tempMarks,
                              @NonNull HashSet<NameWithRelationship> permanentMarks,
                              @NonNull ArrayList<NameWithRelationship> visitOrder,
                              @NonNull ArrayList<Name> result)
                throws LoopDetectedException {
            if (permanentMarks.contains(this)) return;
            Integer lastVisitPosition = tempMarks.get(this);
            // there is a loop from lastVisitPosition to here
            if (lastVisitPosition != null) {
                Name[] loop = new Name[visitOrder.size() - lastVisitPosition];
                for (int i = lastVisitPosition, j = 0; i < visitOrder.size(); i++, j++) {
                    loop[j] = visitOrder.get(i).name;
                }
                throw new LoopDetectedException(loop);
            }
            lastVisitPosition = visitOrder.size();
            tempMarks.put(this, lastVisitPosition);
            visitOrder.add(this);
            for (NameWithRelationship child : children) {
                child.topologicalVisit(tempMarks, permanentMarks, visitOrder, result);
            }
            tempMarks.remove(this);
            visitOrder.remove((int) lastVisitPosition);
            permanentMarks.add(this);
            result.add(0, name);
        }
    }
}

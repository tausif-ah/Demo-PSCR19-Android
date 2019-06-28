package nist.p_70nanb17h188.demo.pscr19.logic.net;

import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.Helper;

import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.logic.Consumer;

public class Namespace {

    private static final long RANDOM_NAME_MAX = -100;
    private static final long RANDOM_NAME_MIN = Integer.MIN_VALUE;

    public static final String EXTRA_ADDED = "added";
    public static final String EXTRA_INITIATOR = "initiator";

    /**
     * The context for namespace events.
     */
    public static final String CONTEXT_NAMESPACE = "nist.p_70nanb17h188.demo.pscr19.logic.net.Namespace";

    /**
     * Broadcast intent action indicating that there is a name added/removed.
     * One extra {@link #EXTRA_NAME} ({@link Name}) indicates the target name.
     * Another extra {@link #EXTRA_ADDED} ({@link Boolean} indicates weather the
     * name is added (true) or remove d(false). A third extra
     * {@link #EXTRA_INITIATOR} ({@link String} indicates the initiator of the
     * action.
     * <p>
     * The current names can be iterated through
     * {@link #forEachName(Consumer)}.
     */
    public static final String ACTION_NAME_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.Namespace.nameChanged";
    public static final String EXTRA_NAME = "name";

    /**
     * Broadcast intent action indicating that there is a relataionship
     * added/removed. One extra {@link #EXTRA_PARENT} ({@link Name}) indicates
     * the parent of the relationship. Another extra
     * {@link #EXTRA_CHILD} ({@link Name}) indicates the child of the
     * relationship. A third extra {@link #EXTRA_ADDED} ({@link Boolean}
     * indicates weather the relationsihp is added (true) or remove d(false). A
     * fourth extra {@link #EXTRA_INITIATOR} ({@link String} indicates the
     * initiator of the action.
     * <p>
     * The current relationships can be iterated through function
     * {@link #forEachChild(Name, Consumer)} and
     * {@link #forEachParent(Name, Consumer)}.
     */
    public static final String ACTION_RELATIONSHIP_CHANGED = "nist.p_70nanb17h188.demo.pscr19.logic.net.Namespace.relationshipChanged";
    public static final String EXTRA_PARENT = "parent";
    public static final String EXTRA_CHILD = "child";

    public static class LoopDetectedException extends Exception {

        @NonNull
        private final Name[] loopNames;

        LoopDetectedException(@NonNull Name[] loopNames) {
            this.loopNames = loopNames;
        }

        @NonNull
        public Name[] getLoopNames() {
            return loopNames;
        }
    }

    private final HashMap<Name, NameWithRelationship> allNames = new HashMap<>();

    /**
     * Checks if a name is in the namespace.
     *
     * @param name The name to be tested.
     * @return True if the name is in the namespace.
     */
    public synchronized boolean hasName(@NonNull Name name) {
        return allNames.containsKey(name);
    }

    @NonNull
    private NameWithRelationship innerAddName(@NonNull Name name, @NonNull String initiator) {
        NameWithRelationship ret = allNames.get(name);
        if (ret == null) {
            allNames.put(name, ret = new NameWithRelationship(name));
            Context.getContext(CONTEXT_NAMESPACE).sendBroadcast(
                    new Intent(ACTION_NAME_CHANGED)
                            .putExtra(EXTRA_NAME, name)
                            .putExtra(EXTRA_ADDED, true)
                            .putExtra(EXTRA_INITIATOR, initiator));
        }
        return ret;
    }

    /**
     * Add a name to the namespace. If successfully added, an intent with
     * {@link #ACTION_NAME_CHANGED} will be broadcasted.
     *
     * @param name      The name to be added.
     * @param initiator The initiator of the action.
     *
     * @return True if the name is added, false if the name already exists.
     */
    public synchronized boolean addName(@NonNull Name name, @NonNull String initiator) {
        NameWithRelationship ret = allNames.get(name);
        if (ret != null) {
            return false;
        }
        allNames.put(name, new NameWithRelationship(name));
        Context.getContext(CONTEXT_NAMESPACE).sendBroadcast(
                new Intent(ACTION_NAME_CHANGED)
                        .putExtra(EXTRA_NAME, name)
                        .putExtra(EXTRA_ADDED, true)
                        .putExtra(EXTRA_INITIATOR, initiator));
        return true;
    }

    /**
     * Add a random name to the namespace. If successfully added, an intent with
     * {@link #ACTION_NAME_CHANGED} will be broadcasted.
     *
     * @param initiator The initiator of the action.
     * @return The name created.
     */
    public synchronized Name addRandomName(@NonNull String initiator) {
        Name n;
        do {
            n = new Name(Helper.getPositiveRandom() % (RANDOM_NAME_MAX - RANDOM_NAME_MIN) + RANDOM_NAME_MIN);
        } while (allNames.containsKey(n));
        innerAddName(n, initiator);
        return n;
    }

    /**
     * Remove a name from the namespace. If successfully removed, an intent with
     * {@link #ACTION_RELATIONSHIP_CHANGED} for each relationship (to parents
     * and children of the node) deleted will be broadcasted, and an intent of
     * {@link #ACTION_RELATIONSHIP_CHANGED} for this name will be broadcasted.
     *
     * @param name      The name to be deleted.
     * @param initiator The initiator of the action.
     */
    public synchronized boolean removeName(@NonNull Name name, @NonNull String initiator) {
        NameWithRelationship n = allNames.get(name);
        if (n == null) {
            return false;
        }
            n.clearRelationships(initiator);
            allNames.remove(name);
            Context.getContext(CONTEXT_NAMESPACE).sendBroadcast(
                    new Intent(ACTION_NAME_CHANGED)
                            .putExtra(EXTRA_NAME, name)
                            .putExtra(EXTRA_ADDED, false)
                            .putExtra(EXTRA_INITIATOR, initiator));
        return true;
    }

    /**
     * Add a relationship to the namespace. Parent and child will be create if
     * they do not yet exist in the namespace. If successfully added, an intent
     * with {@link #ACTION_RELATIONSHIP_CHANGED} will be broadcasted.
     *
     * @param parent    The parent name of the relationship.
     * @param child     The chld name of the relationship.
     * @param initiator The initiator of this action.
     * @return True if the relationsip is added, false if the relationship
     * already exists.
     */
    public synchronized boolean addRelationship(@NonNull Name parent, @NonNull Name child,
                                                @NonNull String initiator) {
        NameWithRelationship p = innerAddName(parent, initiator);
        NameWithRelationship c = innerAddName(child, initiator);
        return p.addChild(c, initiator);
    }

    /**
     * Remove a relationship from the namespace. If parent or child does not
     * exist in the namespace, they will not be created. If successfully
     * removed, an intent with {@link #ACTION_RELATIONSHIP_CHANGED} will be
     * broadcasted.
     *
     * @param parent    The parent nname of the relationship.
     * @param child     The child name of the relationship.
     * @param initiator The initiator of the action.
     * @return True if the relationship is removed, false if the parent, the
     * child or the relationship do not exist.
     */
    public synchronized boolean removeRelationship(@NonNull Name parent, @NonNull Name child,
                                                   @NonNull String initiator) {
        NameWithRelationship p = allNames.get(parent);
        NameWithRelationship c = allNames.get(child);
        return p != null && c != null && p.removeChild(c, initiator);
    }

    /**
     * Iterate through all the names in the namespace. The consumer should not
     * try to change the namespace.
     *
     * @param consumer The consumer of the names.
     */
    public synchronized void forEachName(@NonNull Consumer<Name> consumer) {
        for (NameWithRelationship value : allNames.values()) {
            consumer.accept(value.name);
        }
    }

    /**
     * Iterate through all the children of a parent name. If parent does not
     * exist in the namespace, the function will return directly (without adding
     * parent into the namespace).
     *
     * @param parent   The parent name.
     * @param consumer The consumer of the child names.
     */
    public synchronized void forEachChild(@NonNull Name parent, @NonNull Consumer<Name> consumer) {
        NameWithRelationship p = allNames.get(parent);
        if (p != null) {
            p.forEachChild(consumer);
        }
    }

    /**
     * Iterate through all the parents of a child name. If child does not exist
     * in the namespace, the function will return directly (without adding child
     * into the namespace).
     *
     * @param child    The child name.
     * @param consumer The consumer of the parent names.
     */
    public synchronized void forEachParent(@NonNull Name child, @NonNull Consumer<Name> consumer) {
        NameWithRelationship c = allNames.get(child);
        if (c != null) {
            c.forEachParent(consumer);
        }
    }

    /**
     * Iterate through all the descendants of a root name. If root does not
     * exist in the namespace, the function will return directly (without adding
     * root into the namespace). This function uses BFS, could be heavy, but
     * works correctly even if there is loop in the namespace.
     *
     * @param root     The root name.
     * @param consumer The consumer of the descendant names.
     */
    public synchronized void forEachDescendant(@NonNull Name root, @NonNull Consumer<Name> consumer) {
        NameWithRelationship r = allNames.get(root);
        if (r != null) {
            r.forEachDescendant(consumer);
        }
    }

    /**
     * Iterate through all the ancestors of a leaf name. If leaf does not exist
     * in the namespace, the function will return directly (without adding leaf
     * into the namespace). This function uses BFS, could be heavy, but works
     * correctly even if there is loop in the namespace.
     *
     * @param leaf     The leaf name.
     * @param consumer The consumer of the ancestor names.
     */
    public synchronized void forEachAncestor(@NonNull Name leaf, @NonNull Consumer<Name> consumer) {
        NameWithRelationship l = allNames.get(leaf);
        if (l != null) {
            l.forEachAncestor(consumer);
        }
    }

    /**
     * Checks if the parent name is a parent of the child name.
     *
     * @param parent The parent name to be tested.
     * @param child  The child name to be tested.
     * @return True if parent is a parent of child.
     */
    public synchronized boolean isChild(@NonNull Name parent, @NonNull Name child) {
        NameWithRelationship p = allNames.get(parent);
        NameWithRelationship c = allNames.get(child);
        return p != null && c != null && p.hasChild(c);
    }

    /**
     * Checks if the root name is an ancestor of the descendant name. This
     * function uses BFS, could be heavy, but works correctly even if there is
     * loop in the namespace.
     *
     * @param root       The root name to be tested.
     * @param descendant The descendant name to be tested.
     * @return True if root is an ancestor of descendant.
     */
    public synchronized boolean isDescendant(@NonNull Name root, @NonNull Name descendant) {
        NameWithRelationship r = allNames.get(root);
        NameWithRelationship d = allNames.get(descendant);

        return r != null && d != null && r.hasDescendant(d);
    }

    /**
     * Perform a topological sort in the namespace. The parent name would always
     * appear earlier than its children in the result list. If there is a loop
     * in the namespace, {@link LoopDetectedException} will be thwon, the
     * {@link LoopDetectedException#getLoopNames()} will contain the loop
     * detected. This function uses DFS, could be heavy.
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

        void clearRelationships(@NonNull String initiator) {
            NameWithRelationship[] tmpChildren = children.toArray(new NameWithRelationship[0]);
            for (NameWithRelationship child : tmpChildren) {
                removeChild(child, initiator);
            }
            NameWithRelationship[] tmpParents = parents.toArray(new NameWithRelationship[0]);
            for (NameWithRelationship parent : tmpParents) {
                parent.removeChild(this, initiator);
            }
        }

        boolean addChild(@NonNull NameWithRelationship child, @NonNull String initiator) {
            if (children.add(child)) {
                child.parents.add(this);
                Context.getContext(CONTEXT_NAMESPACE).sendBroadcast(
                        new Intent(ACTION_RELATIONSHIP_CHANGED)
                                .putExtra(EXTRA_PARENT, this.name)
                                .putExtra(EXTRA_CHILD, child.name)
                                .putExtra(EXTRA_ADDED, true)
                                .putExtra(EXTRA_INITIATOR, initiator));
                return true;
            } else {
                return false;
            }
        }

        boolean removeChild(@NonNull NameWithRelationship child, @NonNull String initiator) {
            if (children.remove(child)) {
                child.parents.remove(this);
                Context.getContext(CONTEXT_NAMESPACE).sendBroadcast(
                        new Intent(ACTION_RELATIONSHIP_CHANGED)
                                .putExtra(EXTRA_PARENT, this.name)
                                .putExtra(EXTRA_CHILD, child.name)
                                .putExtra(EXTRA_ADDED, false)
                                .putExtra(EXTRA_INITIATOR, initiator));
                return true;
            } else {
                return false;
            }
        }

        void forEachChild(@NonNull Consumer<Name> consumer) {
            for (NameWithRelationship child : children) {
                consumer.accept(child.name);
            }
        }

        void forEachParent(@NonNull Consumer<Name> consumer) {
            for (NameWithRelationship parent : parents) {
                consumer.accept(parent.name);
            }
        }

        void forEachAncestor(@NonNull Consumer<Name> consumer) {
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

        void forEachDescendant(@NonNull Consumer<Name> consumer) {
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
                if (curr == n) {
                    return true;
                }
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
            if (permanentMarks.contains(this)) {
                return;
            }
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

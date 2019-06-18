package nist.p_70nanb17h188.demo.pscr19.logic;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

public class FIFOSet<T> implements Iterable<T> {
    public interface ItemChangedEventHandler<T> {
        void handleItemChanged(@NonNull T item, boolean added);
    }

    private final int capacity;
    @NonNull
    private final ArrayList<T> items;
    private final HashSet<T> index = new HashSet<>();
    private final HashSet<ItemChangedEventHandler<T>> itemChangedEventHandlers = new HashSet<>();

    public FIFOSet(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity should be > 0");
        this.capacity = capacity;
        items = new ArrayList<>(capacity);
    }

    public synchronized boolean addItemChangedEventHandler(ItemChangedEventHandler<T> handler) {
        return itemChangedEventHandlers.add(handler);
    }

    public synchronized boolean removeItemChangedEventHandler(ItemChangedEventHandler<T> handler) {
        return itemChangedEventHandlers.remove(handler);
    }

    public synchronized boolean add(@NonNull T t) {
        // redundant item
        if (index.contains(t)) return false;
        if (items.size() == capacity) {
            //remove the earliest item
            T tmp = items.remove(0);
            index.remove(tmp);
            // notify that an item has been removed
            for (ItemChangedEventHandler<T> h : itemChangedEventHandlers)
                h.handleItemChanged(tmp, false);
        }
        // add the new item
        items.add(t);
        index.add(t);
        // notify that an item has added
        for (ItemChangedEventHandler<T> h : itemChangedEventHandlers)
            h.handleItemChanged(t, true);
        return true;
    }

    public synchronized void clear() {
        ArrayList<T> tmp = new ArrayList<>(items);
        items.clear();
        index.clear();
        for (T obj : tmp)
            for (ItemChangedEventHandler<T> h : itemChangedEventHandlers)
                h.handleItemChanged(obj, false);

    }

    @NonNull
    @Override
    public synchronized Iterator<T> iterator() {
        return items.iterator();
    }

    public synchronized int size() {
        return items.size();
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized boolean contains(@NonNull T obj) {
        return index.contains(obj);
    }

    @NonNull
    public synchronized T[] toArray(@NonNull T[] a) {
        return items.toArray(a);
    }

    public int getCapacity() {
        return capacity;
    }
}

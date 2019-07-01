package nist.p_70nanb17h188.demo.pscr19.logic;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class FIFOMap<T, V> implements Iterable<FIFOMap.Entry<T, V>> {
    public interface ItemChangedEventHandler<T, V> {
        void handleItemChanged(@NonNull T key, @NonNull V value, boolean added);
    }

    public static class Entry<T, V> {
        private final T key;
        private final V val;

        Entry(T key, V val) {
            this.key = key;
            this.val = val;
        }

        public T getKey() {
            return key;
        }

        public V getVal() {
            return val;
        }
    }

    private final int capacity;
    @NonNull
    private final ArrayList<Entry<T, V>> items;
    private final HashMap<T, V> index = new HashMap<>();
    private final HashSet<ItemChangedEventHandler<T, V>> itemChangedEventHandlers = new HashSet<>();

    public FIFOMap(int capacity) {
        this.capacity = capacity;
        items = new ArrayList<>(capacity);
    }

    public synchronized boolean addItemChangedEventHandler(ItemChangedEventHandler<T, V> handler) {
        return itemChangedEventHandlers.add(handler);
    }

    public synchronized boolean removeItemChangedEventHandler(ItemChangedEventHandler<T, V> handler) {
        return itemChangedEventHandlers.remove(handler);
    }

    public synchronized boolean add(@NonNull T key, @NonNull V val) {
        // redundant item
        if (index.containsKey(key)) return false;
        if (items.size() == capacity) {
            //remove the earliest item
            Entry<T, V> tmp = items.remove(0);
            index.remove(tmp.getKey());
            // notify that an item has been removed
            for (ItemChangedEventHandler<T, V> h : itemChangedEventHandlers)
                h.handleItemChanged(tmp.getKey(), tmp.getVal(), false);
        }
        // add the new item
        items.add(new Entry<>(key, val));
        index.put(key, val);
        // notify that an item has added
        for (ItemChangedEventHandler<T, V> h : itemChangedEventHandlers)
            h.handleItemChanged(key, val, true);
        return true;
    }

    public synchronized void clear() {
        ArrayList<Entry<T, V>> tmp = new ArrayList<>(items);
        items.clear();
        index.clear();
        for (Entry<T, V> obj : tmp)
            for (ItemChangedEventHandler<T, V> h : itemChangedEventHandlers)
                h.handleItemChanged(obj.getKey(), obj.getVal(), false);
    }

    @NonNull
    @Override
    public synchronized Iterator<Entry<T, V>> iterator() {
        return items.iterator();
    }

    public synchronized int size() {
        return items.size();
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized boolean containsKey(@NonNull T obj) {
        return index.containsKey(obj);
    }

    @Nullable
    public synchronized V get(@NonNull T obj) {
        return index.get(obj);
    }

    public synchronized Entry<T, V>[] toArray(@NonNull Entry<T, V>[] a) {
        return items.toArray(a);
    }

    public synchronized void forEach(BiConsumer<T, V> consumer) {
        for (Entry<T, V> item : items) consumer.accept(item.key, item.getVal());
    }

    public int getCapacity() {
        return capacity;
    }

}

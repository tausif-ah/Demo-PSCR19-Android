package nist.p_70nanb17h188.demo.pscr19.gui.log;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableArrayList;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

class LogFragmentViewModel extends ViewModel {
    private static final String TAG_ALL = "__ALL__";

    final MutableLiveData<FilterCriteria> criteria = new MutableLiveData<>();
    final ObservableArrayList<LogItem> cache = new ObservableArrayList<>();
    private final HashMap<String, AtomicInteger> tagCounts = new HashMap<>();
    private final BroadcastReceiver receiver = (context, intent) -> onReceiveBroadcastIntent(intent);
    private Handler mainLoopHandler;

    LogFragmentViewModel() {
        List<String> tags = new ArrayList<>();
        tags.add(TAG_ALL);
        criteria.setValue(new FilterCriteria(LogType.Verbose, LogType.Error, null, tags));

        Context.getContext(Log.CONTEXT_LOG)
                .registerReceiver(
                        receiver,
                        new IntentFilter().addAction(Log.ACTION_LOG_ITEM_INSERTED)
                );

        ArrayList<LogItem> existingItems = Log.getLatestLogItems();
        cache.addAll(existingItems);
        for (LogItem item : existingItems) {
            AtomicInteger value = tagCounts.get(item.getTag());
            if (value == null) {
                tagCounts.put(item.getTag(), value = new AtomicInteger(0));
            }
            value.incrementAndGet();
        }
        FilterCriteria original = criteria.getValue();
        assert original != null;
        criteria.postValue(new FilterCriteria(original.getLvMin(), original.getLvMax(), original.getSelectedTag(), updateTags()));
    }

    void setMainLoopHandler(@Nullable Handler handler) {
        mainLoopHandler = handler;
    }

    void setLvMin(@NonNull LogType lvMin) {
        FilterCriteria original = criteria.getValue();
        assert original != null;
        if (original.getLvMin() == lvMin) return;
        LogType newMax = original.getLvMax().getVal() < lvMin.getVal() ? lvMin : original.getLvMax();
        criteria.postValue(new FilterCriteria(lvMin, newMax, original.getSelectedTag(), original.getTags()));
    }

    void setLvMax(@NonNull LogType lvMax) {
        FilterCriteria original = criteria.getValue();
        assert original != null;
        if (original.getLvMax() == lvMax) return;
        LogType newMin = original.getLvMin().getVal() > lvMax.getVal() ? lvMax : original.getLvMin();
        criteria.postValue(new FilterCriteria(newMin, lvMax, original.getSelectedTag(), original.getTags()));
    }

    void setSelectedTag(@Nullable String selectedTag) {
        FilterCriteria original = criteria.getValue();
        assert original != null;
        if (Objects.equals(selectedTag, original.getSelectedTag())) return;
        criteria.postValue(new FilterCriteria(original.getLvMin(), original.getLvMax(), selectedTag, original.getTags()));
    }

    private ArrayList<String> updateTags() {
        String[] tmp = tagCounts.keySet().toArray(new String[0]);
        Arrays.sort(tmp);
        ArrayList<String> newTags = new ArrayList<>(Arrays.asList(tmp));
        newTags.add(0, TAG_ALL);

        FilterCriteria original = criteria.getValue();
        assert original != null;
        String selectedTag = original.getSelectedTag();
        int idx = selectedTag == null ? 0 : Arrays.binarySearch(tmp, selectedTag);
        if (idx < 0) newTags.add(-idx, selectedTag);
        return newTags;
    }

    private void onReceiveBroadcastIntent(Intent intent) {

        if (intent == null || !Log.ACTION_LOG_ITEM_INSERTED.equals(intent.getAction())) return;
        LogItem newItem = intent.getExtra(Log.EXTRA_LOG_ITEM);
        assert newItem != null;
        if (mainLoopHandler == null) insertLogItem(newItem);
        else mainLoopHandler.post(() -> insertLogItem(newItem));
    }

    private void insertLogItem(@NonNull LogItem newItem) {
        synchronized (this) {
            boolean needUpdateKeySet = false;
            AtomicInteger count;
            if (cache.size() == Log.getCapacity()) {
                LogItem removed = cache.remove(cache.size() - 1);
                count = tagCounts.get(removed.getTag());
                assert count != null;
                if (count.decrementAndGet() == 0) {
                    tagCounts.remove(removed.getTag());
                    needUpdateKeySet = true;
                }
            }
            cache.add(0, newItem);
            count = tagCounts.get(newItem.getTag());
            if (count == null) {
                tagCounts.put(newItem.getTag(), count = new AtomicInteger(0));
                needUpdateKeySet = true;
            }
            count.incrementAndGet();
            if (needUpdateKeySet) {
                FilterCriteria original = criteria.getValue();
                assert original != null;
                criteria.postValue(new FilterCriteria(original.getLvMin(), original.getLvMax(), original.getSelectedTag(), updateTags()));
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Context.getContext(Log.CONTEXT_LOG).unregisterReceiver(receiver);
    }

}
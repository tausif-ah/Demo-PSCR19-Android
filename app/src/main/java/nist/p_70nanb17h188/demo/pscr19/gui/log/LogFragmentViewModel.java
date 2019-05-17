package nist.p_70nanb17h188.demo.pscr19.gui.log;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.ObservableArrayList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

class LogFragmentViewModel extends ViewModel {
    private static final String TAG_ALL = "__ALL__";

    final MutableLiveData<FilterCriteria> criteria = new MutableLiveData<>();
    final ObservableArrayList<LogItem> cache = new ObservableArrayList<>();
    private final HashMap<String, AtomicInteger> tagCounts = new HashMap<>();
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onReceiveBroadcastIntent(intent);
        }
    };
    private Application application;

    public LogFragmentViewModel() {
        List<String> tags = new ArrayList<>();
        tags.add(TAG_ALL);
        criteria.setValue(new FilterCriteria(LogType.Verbose, LogType.Error, null, tags));
    }

    void setApplication(@NonNull Application application) {
        synchronized (this) {
            if (this.application != null) return;
            this.application = application;
            synchronizeData();
        }
        Context context = application.getApplicationContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Log.ACTION_LOG_ITEM_INSERTED);
        context.registerReceiver(receiver, filter);
    }

    private void synchronizeData() {
        ArrayList<LogItem> existingItems = Log.getLatestLogItems();
        cache.addAll(existingItems);
        for (LogItem item : existingItems) {
            AtomicInteger value = tagCounts.get(item.tag);
            if (value == null) {
                tagCounts.put(item.tag, value = new AtomicInteger(0));
            }
            value.incrementAndGet();
        }
        FilterCriteria original = criteria.getValue();
        assert original != null;
        criteria.postValue(new FilterCriteria(original.lvMin, original.lvMax, original.selectedTag, updateTags()));
    }

    void setLvMin(@NonNull LogType lvMin) {
        FilterCriteria original = criteria.getValue();
        assert original != null;
        if (original.lvMin == lvMin) return;
        LogType newMax = original.lvMax.val < lvMin.val ? lvMin : original.lvMax;
        criteria.postValue(new FilterCriteria(lvMin, newMax, original.selectedTag, original.tags));
    }

    void setLvMax(@NonNull LogType lvMax) {
        FilterCriteria original = criteria.getValue();
        assert original != null;
        if (original.lvMax == lvMax) return;
        LogType newMin = original.lvMin.val > lvMax.val ? lvMax : original.lvMin;
        criteria.postValue(new FilterCriteria(newMin, lvMax, original.selectedTag, original.tags));
    }

    void setSelectedTag(@Nullable String selectedTag) {
        FilterCriteria original = criteria.getValue();
        assert original != null;
        if (Objects.equals(selectedTag, original.selectedTag)) return;
        criteria.postValue(new FilterCriteria(original.lvMin, original.lvMax, selectedTag, original.tags));
    }

    private ArrayList<String> updateTags() {
        String[] tmp = tagCounts.keySet().toArray(new String[0]);
        Arrays.sort(tmp);
        ArrayList<String> newTags = new ArrayList<>(Arrays.asList(tmp));
        newTags.add(0, TAG_ALL);

        FilterCriteria original = criteria.getValue();
        assert original != null;
        String selectedTag = original.selectedTag;
        int idx = selectedTag == null ? 0 : Arrays.binarySearch(tmp, selectedTag);
        if (idx < 0) newTags.add(-idx, selectedTag);
        return newTags;
    }

    private void onReceiveBroadcastIntent(Intent intent) {
        if (intent == null || !Log.ACTION_LOG_ITEM_INSERTED.equals(intent.getAction())) return;
        LogItem newItem = intent.getParcelableExtra(Log.EXTRA_LOG_ITEM);
        synchronized (this) {
            boolean needUpdateKeySet = false;
            AtomicInteger count;
            if (cache.size() == Log.getCapacity()) {
                LogItem removed = cache.remove(cache.size() - 1);
                count = tagCounts.get(removed.tag);
                assert count != null;
                if (count.decrementAndGet() == 0) {
                    tagCounts.remove(removed.tag);
                    needUpdateKeySet = true;
                }
            }
            cache.add(0, newItem);
            count = tagCounts.get(newItem.tag);
            if (count == null) {
                tagCounts.put(newItem.tag, count = new AtomicInteger(0));
                needUpdateKeySet = true;
            }
            count.incrementAndGet();
            if (needUpdateKeySet) {
                FilterCriteria original = criteria.getValue();
                assert original != null;
                criteria.postValue(new FilterCriteria(original.lvMin, original.lvMax, original.selectedTag, updateTags()));
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        synchronized (this) {
            if (application != null) {
                application.getApplicationContext().unregisterReceiver(receiver);
            }
        }
    }

}
package nist.p_70nanb17h188.demo.pscr19.gui.log;

import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

public class LogFilterFragmentViewModel extends ViewModel {
    interface ShowingTagUpdatedHandler {
        void accept(List<String> tags, int newPosition);
    }


    private static final String TAG_ALL = "__ALL__";
    private final LogFilterLevelTag filter;
    private final BroadcastReceiver logUpdatedReceiver = this::onLogUpdated;
    private final HashMap<String, AtomicInteger> tagCounts = new HashMap<>();
    private final List<String> showingTags = new ArrayList<>();
    private ShowingTagUpdatedHandler showingTagUpdated;

    public LogFilterFragmentViewModel() {
        filter = new LogFilterLevelTag(LogType.Verbose, LogType.Error, null);

        synchronizeData();
        Context.getContext(Log.CONTEXT_LOG).registerReceiver(logUpdatedReceiver,
                new IntentFilter().addAction(Log.ACTION_LOG_ITEM_UPDATED));
    }

    LogFilterLevelTag getFilter() {
        return filter;
    }

    void setShowingTagUpdated(ShowingTagUpdatedHandler showingTagUpdated) {
        this.showingTagUpdated = showingTagUpdated;
        if (showingTagUpdated != null)
            updateTags();
    }

    List<String> getShowingTags() {
        return showingTags;
    }

    void setMax(@NonNull LogType type) {
        filter.update(type.getVal() < filter.getLvMin().getVal() ? type : filter.getLvMin(), type, filter.getSelectedTag());
    }

    void setMin(@NonNull LogType type) {
        filter.update(type, type.getVal() > filter.getLvMax().getVal() ? type : filter.getLvMax(), filter.getSelectedTag());
    }

    void setTag(@Nullable String tag) {
        filter.update(filter.getLvMin(), filter.getLvMax(), tag);
    }

    private void onLogUpdated(@NonNull Context context, @NonNull Intent intent) {
        LogItem added = intent.getExtra(Log.EXTRA_LOG_ITEM_INSERTED);
        boolean needUpdateTags = false;
        if (added != null) {
            AtomicInteger integer = tagCounts.get(added.getTag());
            if (integer == null) {
                tagCounts.put(added.getTag(), integer = new AtomicInteger(0));
                needUpdateTags = true;
            }
            integer.incrementAndGet();
        }
        LogItem removed = intent.getExtra(Log.EXTRA_LOG_ITEM_REMOVED);
        if (removed != null) {
            AtomicInteger integer = tagCounts.get(removed.getTag());
            if (integer != null) {
                int count = integer.decrementAndGet();
                if (count == 0) {
                    tagCounts.remove(removed.getTag());
                    needUpdateTags = true;
                }
            }
        }
        if (needUpdateTags) updateTags();
    }

    private void synchronizeData() {
        ArrayList<LogItem> items = Log.getLatestLogItems();
        for (LogItem item : items) {
            AtomicInteger integer = tagCounts.get(item.getTag());
            if (integer == null) tagCounts.put(item.getTag(), integer = new AtomicInteger(0));
            integer.incrementAndGet();
        }
        updateTags();
    }

    private void updateTags() {
        String[] tmp = tagCounts.keySet().toArray(new String[0]);
        Arrays.sort(tmp);
        showingTags.clear();
        showingTags.addAll(Arrays.asList(tmp));
        showingTags.add(0, TAG_ALL);

        String selectedTag = filter.getSelectedTag();
        int idx;
        if (selectedTag != null) {
            idx = Arrays.binarySearch(tmp, selectedTag);
            if (idx < 0) {
                idx = -idx;
                showingTags.add(idx, selectedTag);
            } else {
                idx++;
            }
        } else {
            idx = 0;
        }
        if (showingTagUpdated != null) {
            showingTagUpdated.accept(showingTags, idx);
        }
    }


    // ontagschanged

    @Override
    protected void onCleared() {
        super.onCleared();
        Context.getContext(Log.CONTEXT_LOG).unregisterReceiver(logUpdatedReceiver);
    }
}

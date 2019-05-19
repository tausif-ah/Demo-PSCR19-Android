package nist.p_70nanb17h188.demo.pscr19.gui.log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

class FilterCriteria {
    @NonNull
    private final LogType lvMin;
    @NonNull
    private final LogType lvMax;
    @Nullable
    private final String selectedTag;
    @NonNull
    private final List<String> tags;

    FilterCriteria(@NonNull LogType lvMin, @NonNull LogType lvMax, @Nullable String selectedTag, @NonNull List<String> tags) {
        this.lvMin = lvMin;
        this.lvMax = lvMax;
        this.selectedTag = selectedTag;
        this.tags = tags;
    }

    @NonNull
    LogType getLvMin() {
        return lvMin;
    }

    @NonNull
    LogType getLvMax() {
        return lvMax;
    }

    @Nullable
    String getSelectedTag() {
        return selectedTag;
    }

    @NonNull
    List<String> getTags() {
        return tags;
    }

    boolean match(LogItem item) {
        boolean ret = selectedTag == null || item.getTag().equals(selectedTag);
        ret = ret && item.getType().getVal() >= lvMin.getVal() && item.getType().getVal() <= lvMax.getVal();
        return ret;
    }

}
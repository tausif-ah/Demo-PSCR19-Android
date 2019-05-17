package nist.p_70nanb17h188.demo.pscr19.gui.log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

class FilterCriteria {
    @NonNull
    final LogType lvMin;
    @NonNull
    final LogType lvMax;
    @Nullable
    final String selectedTag;
    @NonNull
    final List<String> tags;

    FilterCriteria(@NonNull LogType lvMin, @NonNull LogType lvMax, @Nullable String selectedTag, @NonNull List<String> tags) {
        this.lvMin = lvMin;
        this.lvMax = lvMax;
        this.selectedTag = selectedTag;
        this.tags = tags;
    }

    boolean match(LogItem item) {
        boolean ret = selectedTag == null || item.tag.equals(selectedTag);
        ret = ret && item.type.val >= lvMin.val && item.type.val <= lvMax.val;
        return ret;
    }

}
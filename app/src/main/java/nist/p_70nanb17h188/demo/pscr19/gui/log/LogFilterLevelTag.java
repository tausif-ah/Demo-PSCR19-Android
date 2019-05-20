package nist.p_70nanb17h188.demo.pscr19.gui.log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

class LogFilterLevelTag extends LogFilter {
    @NonNull
    private LogType lvMin;
    @NonNull
    private LogType lvMax;
    @Nullable
    private String selectedTag;

    LogFilterLevelTag(@NonNull LogType lvMin, @NonNull LogType lvMax, @Nullable String selectedTag) {
        this.lvMin = lvMin;
        this.lvMax = lvMax;
        this.selectedTag = selectedTag;
    }

    void update(@NonNull LogType lvMin, @NonNull LogType lvMax, @Nullable String selectedTag) {
        this.lvMin = lvMin;
        this.lvMax = lvMax;
        this.selectedTag = selectedTag;
        fireLogFilterUpdated();
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

    @Override
    public boolean matches(@NonNull LogItem item) {
        boolean ret = selectedTag == null || item.getTag().equals(selectedTag);
        ret = ret && item.getType().getVal() >= lvMin.getVal() && item.getType().getVal() <= lvMax.getVal();
        return ret;
    }
}
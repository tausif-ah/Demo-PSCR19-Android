package nist.p_70nanb17h188.demo.pscr19.gui;


import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import nist.p_70nanb17h188.demo.pscr19.Log;
import nist.p_70nanb17h188.demo.pscr19.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class LogFragment extends Fragment {

    private static final LeadingMarginSpan DEFAULT_FIRST_LINE_MARGIN = new LeadingMarginSpan.Standard(0, 40);
    private static final LeadingMarginSpan DEFAULT_REST_LINE_MARGIN = new LeadingMarginSpan.Standard(40, 40);
    private Spinner spinner_lvMin, spinner_lvMax, spinner_tag;
    private Log.LogType lvMin = Log.LogType.Verbose, lvMax = Log.LogType.Error;
    private String tag = null;
    private ArrayAdapter<String> tagAdapter;
    private LinearLayoutManager listLayoutManager;
    private RecyclerView list;
    private RecyclerView.Adapter<LogViewHolder> listAdapter = new RecyclerView.Adapter<LogViewHolder>() {

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
            return new LogViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder viewHolder, int i) {
            viewHolder.bindLog(Log.getItemAt(i));
        }

        @Override
        public int getItemCount() {
            return Log.getSize();
        }
    };
    private final AdapterView.OnItemSelectedListener logLevelItemSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.LogType selected = (Log.LogType) parent.getSelectedItem();
            if (parent == spinner_lvMin) {
                if (lvMin != selected) {
                    lvMin = selected;
                    if (lvMin.getVal() > lvMax.getVal()) {
                        spinner_lvMax.setSelection(position);
                        lvMax = lvMin;
                    }
                    filterUpdated();
                }
            } else if (parent == spinner_lvMax) {
                if (lvMax != selected) {
                    lvMax = selected;
                    if (lvMax.getVal() < lvMin.getVal()) {
                        spinner_lvMin.setSelection(position);
                        lvMin = lvMax;
                    }
                    filterUpdated();
                }
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    private final AdapterView.OnItemSelectedListener tagItemSelectedListener = new AdapterView.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String newTag = position == 0 ? null : (String) parent.getSelectedItem();
            if (!Objects.equals(tag, newTag)) {
                tag = newTag;
                filterUpdated();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    private Log.RangeChangedHandler insertHandler, removeHandler;
    private Log.TagsChangedHandler tagsChangedHandler;

    public LogFragment() {
        insertHandler = this::logInserted;
        removeHandler = this::logRemoved;
        tagsChangedHandler = this::logTagsChanged;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spinner_lvMin = view.findViewById(R.id.log_lvMin);
        spinner_lvMax = view.findViewById(R.id.log_lvMax);
        Log.LogType[] logTypes = Log.LogType.values();
        Arrays.sort(logTypes, 0, logTypes.length, (a, b) -> Integer.compare(a.getVal(), b.getVal()));

        ArrayAdapter<Log.LogType> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, logTypes);
        spinner_lvMin.setAdapter(adapter);
        spinner_lvMin.setSelection(0);
        spinner_lvMin.setOnItemSelectedListener(logLevelItemSelectedListener);
        spinner_lvMax.setAdapter(adapter);
        spinner_lvMax.setSelection(logTypes.length - 1);
        spinner_lvMax.setOnItemSelectedListener(logLevelItemSelectedListener);

        spinner_tag = view.findViewById(R.id.log_tag);
        tagAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinner_tag.setAdapter(tagAdapter);
        spinner_tag.setSelection(0);
        spinner_tag.setOnItemSelectedListener(tagItemSelectedListener);
        logTagsChanged(null);
        Log.addTagsChangedHandler(tagsChangedHandler);

        listLayoutManager = new WrapLinearLayoutManager(view.getContext());
        list = view.findViewById(R.id.log_list);
        list.setLayoutManager(listLayoutManager);
        list.setAdapter(listAdapter);
//        android.util.Log.d("LogFragment", "Add!");

        Log.addItemRangeInsertedHandler(insertHandler);
        Log.addItemRangeRemovedHandler(removeHandler);

    }

    @Override
    public void onDestroyView() {
//        android.util.Log.d("LogFragment", "Destroy!");
//        boolean removed = Log.removeTagsChangedHandler(tagsChangedHandler);
//        android.util.Log.d("LogFragment", "Destroy!" + removed);
        Log.removeItemRangeInsertedHandler(insertHandler);
        Log.removeItemRangeRemovedHandler(removeHandler);
        super.onDestroyView();
    }

    private void logTagsChanged(Log log) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
//            android.util.Log.d("LogFragment", "logTagsChagned!");
                tagAdapter.clear();
                tagAdapter.add("__ALL__");
                String[] tags = Log.getTags();
                tagAdapter.addAll(tags);

                if (tag != null && !Arrays.asList(tags).contains(tag)) {
                    tagAdapter.add(tag);
                    tagAdapter.notifyDataSetChanged();
                    spinner_tag.setSelection(tags.length + 1);
                } else {
                    tagAdapter.notifyDataSetChanged();
                }
            });
        }

    }

    private void filterUpdated() {
//        android.util.Log.d("LogFragment", "min=" + lvMin + ", max=" + lvMax + ", tag=" + tag);
//            listAdapter.notifyDataSetChanged();
        listAdapter.notifyItemRangeChanged(0, listAdapter.getItemCount());
    }

    private void logInserted(Log log, int position, int count) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> {
//            android.util.Log.d("LogFragment", "logInserted");
//            listAdapter.notifyDataSetChanged();
                listAdapter.notifyItemRangeInserted(position, count);
                if (listLayoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    list.smoothScrollToPosition(0);
                }
            });
        }

    }

    private void logRemoved(Log log, int position, int count) {
        Activity activity = getActivity();
        if (activity != null) {
            getActivity().runOnUiThread(() -> {
//            android.util.Log.d("LogFragment", "logRemoved");
//            listAdapter.notifyDataSetChanged();
                listAdapter.notifyItemRangeRemoved(position, count);
            });
        }
    }

    @NonNull
    private SpannableStringBuilder getLogText(Log.LogItem item) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(String.format(Locale.US, "[ %s | %d ] %s/%s: ", item.getTimeString(), item.getId(), item.getType().getAcry(), item.getTag()));
        int position = builder.length();
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, position, 0);
        builder.setSpan(DEFAULT_FIRST_LINE_MARGIN, 0, position, 0);
        builder.append(item.getMessage());
        builder.setSpan(DEFAULT_REST_LINE_MARGIN, position, builder.length(), 0);
        return builder;
    }

    private class LogViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private View itemView;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            textView = itemView.findViewById(R.id.log_content);
        }

        void bindLog(Log.LogItem item) {
            textView.setText(getLogText(item), TextView.BufferType.SPANNABLE);
            boolean visible = tag == null || item.getTag().equals(tag);
            visible = visible && item.getType().getVal() >= lvMin.getVal() && item.getType().getVal() <= lvMax.getVal();
            textView.setVisibility(visible ? View.VISIBLE : View.GONE);
//            itemView.setVisibility(visible ? View.VISIBLE : View.GONE);
            ViewGroup.MarginLayoutParams p = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = 0;
            if (visible) {
                Context c = getContext();
                if (c != null) {
                    margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, c.getResources().getDisplayMetrics());
                }
            }
            p.setMargins(margin, margin, margin, margin);
            itemView.setLayoutParams(p);

        }
    }


}

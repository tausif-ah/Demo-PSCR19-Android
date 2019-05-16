package nist.p_70nanb17h188.demo.pscr19.gui.log;


import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

/**
 * A simple {@link Fragment} subclass.
 */
public class LogFragment extends Fragment {

    private static final LeadingMarginSpan DEFAULT_FIRST_LINE_MARGIN = new LeadingMarginSpan.Standard(0, 40);
    private static final LeadingMarginSpan DEFAULT_REST_LINE_MARGIN = new LeadingMarginSpan.Standard(40, 40);
    private LogFragmentViewModel viewModel;
    private RecyclerView list;
    private ImageButton btnTop;
    private boolean isTopLog = true;
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
    private BroadcastReceiver logIntentReceiver;
    private IntentFilter logIntentFilter = new IntentFilter();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getActivity() != null;
        viewModel = ViewModelProviders.of(getActivity()).get(LogFragmentViewModel.class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.criteria.removeObservers(this);
    }

    @NonNull
    private static SpannableStringBuilder getLogText(Log.LogItem item) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(String.format(Locale.US, "[ %s | %d ] %s/%s: ", item.getTimeString(), item.id, item.type.acry, item.tag));
        int position = builder.length();
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, position, 0);
        builder.setSpan(DEFAULT_FIRST_LINE_MARGIN, 0, position, 0);
        builder.append(item.message);
        builder.setSpan(DEFAULT_REST_LINE_MARGIN, position, builder.length(), 0);
        return builder;
    }

    @Override
    public void onResume() {
        super.onResume();
        logIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                    case Log.ACTION_ITEMS_INSERTED: {
                        int position = intent.getIntExtra(Log.EXTRA_POSITION_START, 0);
                        int count = intent.getIntExtra(Log.EXTRA_ITEM_COUNT, 0);
                        if (count == 0) return;
                        listAdapter.notifyItemRangeInserted(position, count);
                        if (isTopLog) {
                            list.scrollToPosition(0);
                        }
                    }
                    break;
                    case Log.ACTION_ITEMS_REMOVED: {
                        int position = intent.getIntExtra(Log.EXTRA_POSITION_START, 0);
                        int count = intent.getIntExtra(Log.EXTRA_ITEM_COUNT, 0);
                        if (count == 0) return;
                        listAdapter.notifyItemRangeRemoved(position, count);
                    }
                    break;
                    case Log.ACTION_TAGS_CHANGED:
                        viewModel.updateTags();
                        break;
                }
            }
        };
        assert getActivity() != null && getActivity().getApplicationContext() != null;
        getActivity().getApplicationContext().registerReceiver(logIntentReceiver, logIntentFilter);

    }

    @Override
    public void onPause() {
        super.onPause();
        assert getActivity() != null && getActivity().getApplicationContext() != null;
        getActivity().getApplicationContext().unregisterReceiver(logIntentReceiver);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);

        Log.LogType[] logTypes = Log.LogType.values();
        Arrays.sort(logTypes, 0, logTypes.length, (a, b) -> Integer.compare(a.val, b.val));
        ArrayAdapter<Log.LogType> logTypeArrayAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, logTypes);
        Spinner spinner_lvMin = view.findViewById(R.id.log_lvMin);
        spinner_lvMin.setAdapter(logTypeArrayAdapter);
        spinner_lvMin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setLvMin((Log.LogType) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        Spinner spinner_lvMax = view.findViewById(R.id.log_lvMax);
        spinner_lvMax.setAdapter(logTypeArrayAdapter);
        spinner_lvMax.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setLvMax((Log.LogType) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Spinner spinner_tag = view.findViewById(R.id.log_tag);
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinner_tag.setAdapter(tagAdapter);
        spinner_tag.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setTag(position == 0 ? null : (String) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        viewModel.updateTags();
        viewModel.criteria.observe(this, criteria -> {
            if (criteria == null) return;
            spinner_lvMin.setSelection(logTypeArrayAdapter.getPosition(criteria.lvMin));
            spinner_lvMax.setSelection(logTypeArrayAdapter.getPosition(criteria.lvMax));
            tagAdapter.clear();
            tagAdapter.addAll(criteria.tags);
            tagAdapter.notifyDataSetChanged();
            spinner_tag.setSelection(criteria.selectedTag == null ? 0 : tagAdapter.getPosition(criteria.selectedTag));
            listAdapter.notifyItemRangeChanged(0, listAdapter.getItemCount());
        });

        list = view.findViewById(R.id.log_list);
        LinearLayoutManager listLayoutManager = new WrapLinearLayoutManager(view.getContext());
        list.setLayoutManager(listLayoutManager);
        list.setAdapter(listAdapter);


        logIntentFilter.addAction(Log.ACTION_ITEMS_INSERTED);
        logIntentFilter.addAction(Log.ACTION_ITEMS_REMOVED);
        logIntentFilter.addAction(Log.ACTION_TAGS_CHANGED);

        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int state = list.getScrollState();
                if (state == RecyclerView.SCROLL_STATE_DRAGGING || state == RecyclerView.SCROLL_STATE_SETTLING) {
                    boolean newIsTopLog = list.computeVerticalScrollOffset() == 0;
                    if (newIsTopLog != isTopLog) {
                        isTopLog = newIsTopLog;
                        btnTop.setVisibility(isTopLog ? View.INVISIBLE : View.VISIBLE);
                    }
                }
            }
        });

        btnTop = view.findViewById(R.id.log_top);
        btnTop.setOnClickListener(v -> {
            isTopLog = true;
            list.scrollToPosition(0);
            btnTop.setVisibility(View.INVISIBLE);
        });
        return view;
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
            FilterCriteria criteria = viewModel.criteria.getValue();
            assert criteria != null;
            textView.setText(getLogText(item), TextView.BufferType.SPANNABLE);
            boolean visible = criteria.match(item);
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

    private static class FilterCriteria {
        @NonNull
        final Log.LogType lvMin;
        @NonNull
        final Log.LogType lvMax;
        final String selectedTag;
        final List<String> tags;

        FilterCriteria(@NonNull Log.LogType lvMin, @NonNull Log.LogType lvMax, String selectedTag, List<String> tags) {
            this.lvMin = lvMin;
            this.lvMax = lvMax;
            this.selectedTag = selectedTag;
            this.tags = tags;
        }

        boolean match(Log.LogItem item) {
            boolean ret = selectedTag == null || item.tag.equals(selectedTag);
            ret = ret && item.type.val >= lvMin.val && item.type.val <= lvMax.val;
            return ret;
        }

    }

    private static class LogFragmentViewModel extends ViewModel {

        private final MutableLiveData<FilterCriteria> criteria = new MutableLiveData<>();

        public LogFragmentViewModel() {
            ArrayList<String> ret = new ArrayList<>();
            ret.add("__ALL__");
            criteria.setValue(new FilterCriteria(Log.LogType.Verbose, Log.LogType.Error, null, ret));
            updateTags();
        }

        void setLvMax(Log.LogType lvMax) {
            FilterCriteria original = criteria.getValue();
            assert original != null;
            if (original.lvMax == lvMax) return;
            Log.LogType newMin = original.lvMin.val > lvMax.val ? lvMax : original.lvMin;
            criteria.postValue(new FilterCriteria(newMin, lvMax, original.selectedTag, original.tags));
        }

        void setLvMin(Log.LogType lvMin) {
            FilterCriteria original = criteria.getValue();
            assert original != null;
            if (original.lvMin == lvMin) return;
            Log.LogType newMax = original.lvMax.val < lvMin.val ? lvMin : original.lvMax;
            criteria.postValue(new FilterCriteria(lvMin, newMax, original.selectedTag, original.tags));
        }

        void setTag(String tag) {
            FilterCriteria original = criteria.getValue();
            assert original != null;
            if (Objects.equals(tag, original.selectedTag)) return;
            criteria.postValue(new FilterCriteria(original.lvMin, original.lvMax, tag, original.tags));
        }

        void updateTags() {
            FilterCriteria original = criteria.getValue();
            assert original != null;

            String[] tmp = Log.getTags();
            Arrays.sort(tmp);
            ArrayList<String> newTags = new ArrayList<>(Arrays.asList(tmp));
            newTags.add(0, "__ALL__");

            String tagValue = original.selectedTag;
            int idx = tagValue == null ? 0 : Arrays.binarySearch(tmp, tagValue);
            if (idx < 0) newTags.add(-idx, tagValue);

            criteria.postValue(new FilterCriteria(original.lvMin, original.lvMax, original.selectedTag, newTags));
        }

    }


}

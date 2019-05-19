package nist.p_70nanb17h188.demo.pscr19.gui.log;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.databinding.ObservableList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

public class LogFragment extends Fragment {
    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static final LeadingMarginSpan DEFAULT_FIRST_LINE_MARGIN = new LeadingMarginSpan.Standard(0, 40);
    private static final LeadingMarginSpan DEFAULT_REST_LINE_MARGIN = new LeadingMarginSpan.Standard(40, 40);
    private LogFragmentViewModel viewModel;
    private ObservableList.OnListChangedCallback onListChangedCallback;
    private boolean isTopLog = true;

    @NonNull
    private static SpannableStringBuilder getLogText(LogItem item) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(String.format(Locale.US, "[ %s | %d ] %s/%s: ", DEFAULT_TIME_FORMAT.format(item.getTime()), item.getId(), item.getType().getAcry(), item.getTag()));
        int position = builder.length();
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, position, 0);
        builder.setSpan(DEFAULT_FIRST_LINE_MARGIN, 0, position, 0);
        builder.append(item.getMessage());
        builder.setSpan(DEFAULT_REST_LINE_MARGIN, position, builder.length(), 0);
        return builder;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = getActivity();
        assert activity != null;
        viewModel = ViewModelProviders.of(activity).get(LogFragmentViewModel.class);
        viewModel.setMainLoopHandler(new Handler());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);
        LogType[] logTypes = LogType.values();
        Arrays.sort(logTypes, 0, logTypes.length, (a, b) -> Integer.compare(a.getVal(), b.getVal()));
        ArrayAdapter<LogType> logTypeArrayAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, logTypes);
        Spinner spinner_lvMin = view.findViewById(R.id.log_lvMin);
        spinner_lvMin.setAdapter(logTypeArrayAdapter);
        spinner_lvMin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setLvMin((LogType) parent.getSelectedItem());
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
                viewModel.setLvMax((LogType) parent.getSelectedItem());
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
                viewModel.setSelectedTag(position == 0 ? null : (String) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        RecyclerView list = view.findViewById(R.id.log_list);
        LinearLayoutManager listLayoutManager = new WrapLinearLayoutManager(view.getContext());
        list.setLayoutManager(listLayoutManager);
        RecyclerView.Adapter<LogViewHolder> listAdapter = new RecyclerView.Adapter<LogViewHolder>() {
            @NonNull
            @Override
            public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
                return new LogViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull LogViewHolder viewHolder, int i) {
                viewHolder.bindLog(viewModel.cache.get(i));
            }

            @Override
            public int getItemCount() {
                return viewModel.cache.size();
            }
        };
        list.setAdapter(listAdapter);

        Observer<FilterCriteria> observer = criteria -> {
            assert criteria != null;
            spinner_lvMin.setSelection(logTypeArrayAdapter.getPosition(criteria.getLvMin()));
            spinner_lvMax.setSelection(logTypeArrayAdapter.getPosition(criteria.getLvMax()));
            tagAdapter.clear();
            tagAdapter.addAll(criteria.getTags());
            tagAdapter.notifyDataSetChanged();
            spinner_tag.setSelection(criteria.getSelectedTag() == null ? 0 : tagAdapter.getPosition(criteria.getSelectedTag()));
            listAdapter.notifyItemRangeChanged(0, listAdapter.getItemCount());
        };
        viewModel.criteria.observe(this, observer);
        observer.onChanged(viewModel.criteria.getValue());

        viewModel.cache.addOnListChangedCallback(onListChangedCallback = new ObservableList.OnListChangedCallback() {
            @Override
            public void onChanged(ObservableList sender) {
                listAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(ObservableList sender, int positionStart, int itemCount) {
                listAdapter.notifyItemRangeChanged(positionStart, itemCount);
            }

            @Override
            public void onItemRangeInserted(ObservableList sender, int positionStart, int itemCount) {
                listAdapter.notifyItemRangeInserted(positionStart, itemCount);
                if (isTopLog) list.scrollToPosition(0);
            }

            @Override
            public void onItemRangeMoved(ObservableList sender, int fromPosition, int toPosition, int itemCount) {
                listAdapter.notifyDataSetChanged();
            }

            @Override
            public void onItemRangeRemoved(ObservableList sender, int positionStart, int itemCount) {
                listAdapter.notifyItemRangeRemoved(positionStart, itemCount);
            }
        });

        ImageButton btnTop = view.findViewById(R.id.log_top);
        btnTop.setOnClickListener(v -> {
            isTopLog = true;
            list.scrollToPosition(0);
            btnTop.setVisibility(View.INVISIBLE);
        });
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

        return view;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        viewModel.criteria.removeObservers(this);
        viewModel.cache.removeOnListChangedCallback(onListChangedCallback);
        viewModel.setMainLoopHandler(null);
    }

    private class LogViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private View itemView;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            textView = itemView.findViewById(R.id.log_content);
        }

        void bindLog(LogItem item) {
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

}
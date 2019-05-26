package nist.p_70nanb17h188.demo.pscr19.gui.log;


import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;

public class LogContentFragment extends Fragment {
    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static final LeadingMarginSpan DEFAULT_FIRST_LINE_MARGIN = new LeadingMarginSpan.Standard(0, 40);
    private static final LeadingMarginSpan DEFAULT_REST_LINE_MARGIN = new LeadingMarginSpan.Standard(40, 40);

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

    private LogFilter filter = LogFilter.ACCEPT_ALL;
    private RecyclerView list;
    private FloatingActionButton btnScrollTop;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean showScrollTop = true, topScroll = true;
    private final LogFilter.LogFilterUpdatedListener filterUpdatedListener = this::synchronizeData;
    private final ArrayList<LogItem> showingLogs = new ArrayList<>();
    private final BroadcastReceiver logUpdatedListener = this::onReceiveLogUpdated;
    private final RecyclerView.Adapter<LogViewHolder> adapter = new RecyclerView.Adapter<LogViewHolder>() {

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
            return new LogViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder logViewHolder, int i) {
            logViewHolder.bindLog(showingLogs.get(i));
        }

        @Override
        public int getItemCount() {
            return showingLogs.size();
        }
    };

    public LogContentFragment() {
        filter.addLogFilterUpdatedListener(filterUpdatedListener);
    }

    public synchronized void setFilter(@NonNull LogFilter filter) {
        if (filter == this.filter) return;
        this.filter.removeLogFilterUpdateListener(filterUpdatedListener);
        this.filter = filter;
        this.filter.addLogFilterUpdatedListener(filterUpdatedListener);
        filterUpdatedListener.logFilterUpdated(filter);
    }

    public void setShowScrollTop(boolean showScrollTop) {
        this.showScrollTop = showScrollTop;
        updateBtnScrollTopVisibility();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_content, container, false);
        list = view.findViewById(R.id.log_list);
        list.setLayoutManager(new WrapLinearLayoutManager(view.getContext()));
        list.setAdapter(adapter);

        btnScrollTop = view.findViewById(R.id.log_scroll_top);
        btnScrollTop.setOnClickListener(v -> {
            topScroll = true;
            updateBtnScrollTopVisibility();
            list.scrollToPosition(0);
        });
        updateBtnScrollTopVisibility();

        filterUpdatedListener.logFilterUpdated(filter);

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
                    if (newIsTopLog != topScroll) {
                        topScroll = newIsTopLog;
                        updateBtnScrollTopVisibility();
                    }
                }
            }
        });

        Context.getContext(Log.CONTEXT_LOG).registerReceiver(this.logUpdatedListener,
                new IntentFilter().addAction(Log.ACTION_LOG_ITEM_UPDATED));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Context.getContext(Log.CONTEXT_LOG).unregisterReceiver(this.logUpdatedListener);
    }

    private void synchronizeData(LogFilter filter) {
        this.showingLogs.clear();
        ArrayList<LogItem> items = Log.getLatestLogItems();
        for (LogItem i : items)
            if (filter.matches(i))
                this.showingLogs.add(i);
        adapter.notifyDataSetChanged();
    }

    private void onReceiveLogUpdated(@NonNull Context context, @NonNull Intent intent) {
        LogItem added = intent.getExtra(Log.EXTRA_LOG_ITEM_INSERTED);
        if (added != null) {
            if (filter.matches(added)) {
                showingLogs.add(0, added);
                adapter.notifyItemRangeInserted(0, 1);
                if (topScroll)
                    list.scrollToPosition(0);
            }
        }
        LogItem removed = intent.getExtra(Log.EXTRA_LOG_ITEM_REMOVED);
        if (removed != null) {
            int idx = showingLogs.indexOf(removed);
            if (idx >= 0) {
                showingLogs.remove(idx);
                adapter.notifyItemRangeRemoved(idx, 1);
            }
        }
    }

    private void updateBtnScrollTopVisibility() {
        if (showScrollTop && !topScroll) {
            btnScrollTop.show();
        } else {
            btnScrollTop.hide();
        }
    }

    private class LogViewHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.log_content);
        }

        void bindLog(LogItem item) {
            textView.setText(getLogText(item), TextView.BufferType.SPANNABLE);
        }
    }

}

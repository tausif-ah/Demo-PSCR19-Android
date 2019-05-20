package nist.p_70nanb17h188.demo.pscr19.gui.log;


import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.Arrays;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

public class LogFilterFragment extends Fragment {

    private Spinner spinnerLvMin, spinnerLvMax, spinnerTag;
    private ArrayAdapter<LogType> logTypeArrayAdapter;
    private ArrayAdapter<String> logTagArrayAdapter;
    private LogFilterFragmentViewModel viewModel;
    private final LogFilter.LogFilterUpdatedListener logFilterUpdatedListener = this::onLogFilterUpdated;

    public LogFilterFragment() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log_filter, container, false);
        spinnerLvMin = view.findViewById(R.id.log_lvMin);
        spinnerLvMax = view.findViewById(R.id.log_lvMax);
        spinnerTag = view.findViewById(R.id.log_tag);
        FragmentActivity activity = getActivity();
        assert activity != null;
        viewModel = ViewModelProviders.of(activity).get(LogFilterFragmentViewModel.class);

        LogType[] logTypes = LogType.values();
        Arrays.sort(logTypes, 0, logTypes.length, (a, b) -> Integer.compare(a.getVal(), b.getVal()));
        logTypeArrayAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, logTypes);
        spinnerLvMin.setAdapter(logTypeArrayAdapter);
        spinnerLvMax.setAdapter(logTypeArrayAdapter);

        spinnerLvMin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (freeze) return;
                viewModel.setMin((LogType) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinnerLvMax.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (freeze) return;
                viewModel.setMax((LogType) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        logTagArrayAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, viewModel.getShowingTags());
        spinnerTag.setAdapter(logTagArrayAdapter);
        viewModel.setShowingTagUpdated(l -> logTagArrayAdapter.notifyDataSetChanged());
        spinnerTag.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (freeze) return;
                viewModel.setTag(position == 0 ? null : (String) parent.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        viewModel.getFilter().addLogFilterUpdatedListener(logFilterUpdatedListener);
        logFilterUpdatedListener.logFilterUpdated(viewModel.getFilter());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.getFilter().removeLogFilterUpdateListener(logFilterUpdatedListener);
        viewModel.setShowingTagUpdated(null);
    }

    private boolean freeze = false;

    private void onLogFilterUpdated(LogFilter filter) {
        if (!(filter instanceof LogFilterLevelTag)) return;
        LogFilterLevelTag f = (LogFilterLevelTag) filter;
        freeze = true;
        spinnerLvMin.setSelection(logTypeArrayAdapter.getPosition(f.getLvMin()));
        spinnerLvMax.setSelection(logTypeArrayAdapter.getPosition(f.getLvMax()));
        spinnerTag.setSelection(f.getSelectedTag() == null ? 0 : logTagArrayAdapter.getPosition(f.getSelectedTag()));
        freeze = false;
    }
}

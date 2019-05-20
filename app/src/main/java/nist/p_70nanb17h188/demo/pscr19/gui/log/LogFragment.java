package nist.p_70nanb17h188.demo.pscr19.gui.log;


import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import nist.p_70nanb17h188.demo.pscr19.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class LogFragment extends Fragment {


    public LogFragment() {
        // Required empty public constructor
    }

    private LogFilterFragmentViewModel filterFragmentViewModel;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);
        // Inflate the layout for this fragment
        FragmentActivity activity = getActivity();
        assert activity != null;
        filterFragmentViewModel = ViewModelProviders.of(activity).get(LogFilterFragmentViewModel.class);
        LogContentFragment contentFragment = (LogContentFragment) getChildFragmentManager().findFragmentById(R.id.log_fragment_content);
        assert contentFragment != null;
        contentFragment.setFilter(filterFragmentViewModel.getFilter());
        return view;
    }

}

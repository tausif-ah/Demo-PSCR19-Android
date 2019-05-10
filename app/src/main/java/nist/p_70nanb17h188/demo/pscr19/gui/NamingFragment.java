package nist.p_70nanb17h188.demo.pscr19.gui;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import nist.p_70nanb17h188.demo.pscr19.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class NamingFragment extends Fragment {


    public NamingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_naming, container, false);
    }

}

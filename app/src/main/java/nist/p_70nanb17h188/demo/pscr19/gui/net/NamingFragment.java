package nist.p_70nanb17h188.demo.pscr19.gui.net;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import nist.p_70nanb17h188.demo.pscr19.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class NamingFragment extends Fragment {


    private FragmentPagerAdapter pagerAdapter;


    public NamingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_naming, container, false);
        Fragment fragment = new NeighborsFragment();

        FragmentActivity activity = getActivity();
        assert activity != null;
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        assert fragmentManager != null;
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.naming_fragment_container, fragment);
        ft.commit();
        return view;
    }

}

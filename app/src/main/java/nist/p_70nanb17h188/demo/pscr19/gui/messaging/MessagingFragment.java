package nist.p_70nanb17h188.demo.pscr19.gui.messaging;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class MessagingFragment extends Fragment {


    public MessagingFragment() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messaging, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("LAUNCHER", "MESSAGE RESUMED");
    }
}

package nist.p_70nanb17h188.demo.pscr19.gui;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;

import nist.p_70nanb17h188.demo.pscr19.Log;
import nist.p_70nanb17h188.demo.pscr19.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class LinkFragment extends Fragment {


    public LinkFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_link, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.setCapacity(50);
        Log.d("LinkFragment", "Entering link fragment!");
        new Thread(() -> {
            char[] candidates = "abcdefghijklmnopqrstuvwxyz \n".toCharArray();
            Random rand = new Random();

            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int type = rand.nextInt(Log.LogType.values().length);
                String tag = i % 500 != 0 ? "TEST!!" : "LinkFragment";
                char[] tmp = new char[rand.nextInt(50) + 1];
                for (int j = 0; j < tmp.length; j++)
                    tmp[j] = candidates[rand.nextInt(candidates.length)];
                String msg = new String(tmp);
                switch (type) {
                    case 0:
                        Log.v(tag, msg);
                        break;
                    case 1:
                        Log.d(tag, msg);
                        break;
                    case 2:
                        Log.i(tag, msg);
                        break;
                    case 3:
                        Log.w(tag, msg);
                        break;
                    case 4:
                        Log.e(tag, new IllegalArgumentException("Test!!"), msg);
                        break;
                }

            }
        }).start();
    }
}

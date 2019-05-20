package nist.p_70nanb17h188.demo.pscr19.gui;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.util.ArrayList;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.link.LinkFragment;
import nist.p_70nanb17h188.demo.pscr19.gui.log.LogFragment;
import nist.p_70nanb17h188.demo.pscr19.gui.messaging.MessagingFragment;
import nist.p_70nanb17h188.demo.pscr19.gui.net.NamingFragment;
import nist.p_70nanb17h188.demo.pscr19.gui.work_offload.WorkOffloadFragment;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;


public class MainActivity extends AppCompatActivity {
    private static final int DEFAULT_FRAGMENT = R.id.main_nav_messaging;

    private DrawerLayout drawer;
    private NavigationView navigationView;
    private int currentFragment;
    private ArrayList<Integer> pastFragments;


    private BroadcastReceiver notificationHandler = (context, intent) -> {
        String msg = intent.getExtra(Helper.EXTRA_NOTIFICATION_CONTENT);
        if (msg == null) return;
        LogType type = intent.getExtra(Helper.EXTRA_NOTIFICATION_TYPE);
        if (type == null) type = LogType.Info;
//        int duration = (type.val >= LogType.Warn.val) ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT;
//        Snackbar.make(this.get, msg, duration).show();
        int duration = (type.getVal() >= LogType.Warn.getVal()) ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        Toast.makeText(this, msg, duration).show();
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentFragment", currentFragment);
        outState.putIntegerArrayList("pastFragments", pastFragments);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        drawer = findViewById(R.id.main_drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Log.d("LAUNCHER", "Main ONCREATE");


        if (savedInstanceState == null) {
            pastFragments = new ArrayList<>();
            currentFragment = DEFAULT_FRAGMENT;
            mShowFragment(currentFragment);
        } else {
            currentFragment = savedInstanceState.getInt("currentFragment");
            pastFragments = savedInstanceState.getIntegerArrayList("pastFragments");
        }
        mSetTitle(currentFragment);

        Context.getContext(Helper.CONTEXT_USER_INTERFACE).registerReceiver(notificationHandler, new IntentFilter().addAction(Helper.ACTION_NOTIFY_USER));

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int fragment = menuItem.getItemId();
            if (fragment == currentFragment) return true;
            pastFragments.add(0, currentFragment);
            currentFragment = fragment;
            mShowFragment(currentFragment);
            return true;
        });

        Log.d("LAUNCHER", "Main FINISH");
    }

    private void mSetTitle(int showFragment) {
        switch (showFragment) {
            case R.id.main_nav_messaging:
                setTitle(Device.getName() + " - " + getString(R.string.nav_messaging));
                break;
            case R.id.main_nav_workoffload:
                setTitle(Device.getName() + " - " + getString(R.string.nav_work_offload));
                break;
            case R.id.main_nav_link:
                setTitle(Device.getName() + " - " + getString(R.string.nav_link));
                break;
            case R.id.main_nav_naming:
                setTitle(Device.getName() + " - " + getString(R.string.nav_naming));
                break;
            case R.id.main_nav_log:
                setTitle(Device.getName() + " - " + getString(R.string.nav_log));
                break;
        }
    }


    private void mShowFragment(int showFragment) {
        navigationView.setCheckedItem(showFragment);
        Fragment fragment;
        switch (showFragment) {
            case R.id.main_nav_messaging:
                fragment = new MessagingFragment();
                break;
            case R.id.main_nav_workoffload:
                fragment = new WorkOffloadFragment();
                break;
            case R.id.main_nav_link:
                fragment = new LinkFragment();
                break;
            case R.id.main_nav_naming:
                fragment = new NamingFragment();
                break;
            case R.id.main_nav_log:
                fragment = new LogFragment();
                break;
            default:
                return;
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main_fragment_container, fragment);
        ft.commit();
        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Context.getContext(Helper.CONTEXT_USER_INTERFACE).unregisterReceiver(notificationHandler);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!pastFragments.isEmpty()) {
                currentFragment = pastFragments.remove(0);
                mShowFragment(currentFragment);
                mSetTitle(currentFragment);
            }
        }
    }

}

package nist.p_70nanb17h188.demo.pscr19.gui;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.util.Stack;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.link.LinkFragment;
import nist.p_70nanb17h188.demo.pscr19.gui.log.LogFragment;
import nist.p_70nanb17h188.demo.pscr19.gui.messaging.MessagingFragment;
import nist.p_70nanb17h188.demo.pscr19.gui.net.NamingFragment;
import nist.p_70nanb17h188.demo.pscr19.gui.work_offload.WorkOffloadFragment;
import nist.p_70nanb17h188.demo.pscr19.logic.Device;


public class MainActivity extends AppCompatActivity {
    private static final int DEFAULT_FRAGMENT = R.id.main_nav_messaging;

    private Fragment messaging, workOffload, link, naming, log;
    private MainActivityViewModel viewModel;
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(Device.getName());
        drawer = findViewById(R.id.main_drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        messaging = new MessagingFragment();
        link = new LinkFragment();
        naming = new NamingFragment();
        workOffload = new WorkOffloadFragment();
        log = new LogFragment();

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);
        navigationView.setNavigationItemSelectedListener(item -> {
            viewModel.setShowingFragment(item.getItemId());
            return true;
        });
        viewModel.getShowingFragment().observe(this, showingFragment -> {
            if (showingFragment == null) showingFragment = DEFAULT_FRAGMENT;
            navigationView.setCheckedItem(showingFragment);
            switch (showingFragment) {
                case R.id.main_nav_messaging:
                    setTitle(Device.getName() + " - " + getString(R.string.nav_messaging));
                    navigate(messaging);
                    break;
                case R.id.main_nav_workoffload:
                    setTitle(Device.getName() + " - " + getString(R.string.nav_work_offload));
                    navigate(workOffload);
                    break;
                case R.id.main_nav_link:
                    setTitle(Device.getName() + " - " + getString(R.string.nav_link));
                    navigate(link);
                    break;
                case R.id.main_nav_naming:
                    setTitle(Device.getName() + " - " + getString(R.string.nav_naming));
                    navigate(naming);
                    break;
                case R.id.main_nav_log:
                    setTitle(Device.getName() + " - " + getString(R.string.nav_log));
                    navigate(log);
                    break;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            viewModel.goBack();
        }
    }

    private void navigate(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main_fragment_container, fragment);
        ft.commit();
        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    private static class MainActivityViewModel extends ViewModel {
        private MutableLiveData<Integer> showingFragment = new MutableLiveData<>();
        private Stack<Integer> stack = new Stack<>();

        public MainActivityViewModel() {
            showingFragment.setValue(DEFAULT_FRAGMENT);
        }

        LiveData<Integer> getShowingFragment() {
            return showingFragment;
        }

        void setShowingFragment(int showingFragment) {
            int orig = this.showingFragment.getValue() == null ? DEFAULT_FRAGMENT : this.showingFragment.getValue();
            if (showingFragment != orig) {
                stack.push(orig);
                this.showingFragment.postValue(showingFragment);
            }
        }

        void goBack() {
            if (stack.isEmpty()) return;
            this.showingFragment.postValue(stack.pop());
        }
    }

}

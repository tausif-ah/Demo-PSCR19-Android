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
import android.view.MenuItem;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.R;


public class MainActivity extends AppCompatActivity {

    private Fragment messaging, workOffload, link, naming, log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(Device.getName());
        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
        messaging = new MessagingFragment();
        link = new LinkFragment();
        naming = new NamingFragment();
        workOffload = new WorkOffloadFragment();
        log = new LogFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container, messaging).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            getSupportFragmentManager().popBackStackImmediate();
        }
    }

    private void navigate(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main_fragment_container, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.main_nav_messaging:
                navigate(messaging);
                break;
            case R.id.main_nav_workoffload:
                navigate(workOffload);
                break;
            case R.id.main_nav_link:
                navigate(link);
                break;
            case R.id.main_nav_naming:
                navigate(naming);
                break;
            case R.id.main_nav_log:
                navigate(log);
                break;
        }

        DrawerLayout drawer = findViewById(R.id.main_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

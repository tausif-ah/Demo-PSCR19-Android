package nist.p_70nanb17h188.demo.pscr19.gui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Arrays;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.Device;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

public class LauncherActivity extends AppCompatActivity {
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.INTERNET,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (String s : REQUIRED_PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{s}, 0);
        }
        setContentView(R.layout.activity_launcher);
        Log.init(Log.DEFAULT_CAPACITY, getApplication());

        ListView nameList = findViewById(R.id.launcher_names);
        String[] names = Device.getExistingNames();
        Arrays.sort(names);
        nameList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        nameList.setOnItemClickListener((parent, view, position, id) -> {
            String name = (String) parent.getItemAtPosition(position);
            if (!Device.isPhone(name)) {
                Snackbar.make(parent, String.format("%s cannot be chosen on a phone", name), BaseTransientBottomBar.LENGTH_SHORT).show();
                return;
            }

            Device.setName((String) parent.getItemAtPosition(position));
            LinkLayer.init(getApplication());

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

}

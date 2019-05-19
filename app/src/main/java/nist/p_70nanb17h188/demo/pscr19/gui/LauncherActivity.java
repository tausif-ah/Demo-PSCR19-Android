package nist.p_70nanb17h188.demo.pscr19.gui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Arrays;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.DelayRunner;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

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

    private BroadcastReceiver notificationHandler = (context, intent) -> {
        String msg = intent.getExtra(Helper.EXTRA_NOTIFICATION_CONTENT);
        if (msg == null) return;
        LogType type = intent.getExtra(Helper.EXTRA_NOTIFICATION_TYPE);
        if (type == null) type = LogType.Info;
//        int duration = (type.val >= LogType.Warn.val) ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT;
//        Snackbar.make(this.get, msg, duration).show();
        int duration = (type.getVal() >= LogType.Warn.getVal()) ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
        DelayRunner.getDefaultInstance().post(() -> Toast.makeText(this, msg, duration).show());
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context.getContext(Helper.CONTEXT_USER_INTERFACE).registerReceiver(notificationHandler, new IntentFilter().addAction(Helper.ACTION_NOTIFY_USER));

        for (String s : REQUIRED_PERMISSIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{s}, 0);
        }
        setContentView(R.layout.activity_launcher);
        Log.init(Log.DEFAULT_CAPACITY);


        ListView nameList = findViewById(R.id.launcher_names);
        String[] names = Device.getExistingNames();
        Arrays.sort(names);
        nameList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        nameList.setOnItemClickListener(this::nameListClicked);
    }


    private void nameListClicked(AdapterView<?> parent, View view, int position, long id) {
        String name = (String) parent.getItemAtPosition(position);
        if (!Device.isPhone(name)) {
            Helper.notifyUser(LogType.Info, "%s cannot be chosen on a phone", name);
            return;
        }

        Device.setName((String) parent.getItemAtPosition(position));
        Log.d("LAUNCHER", "INIT START");
        LinkLayer.init();
        NetLayer.init();
        Log.d("LAUNCHER", "INIT END");

        startActivity(new android.content.Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Context.getContext(Helper.CONTEXT_USER_INTERFACE).unregisterReceiver(notificationHandler);

    }
}

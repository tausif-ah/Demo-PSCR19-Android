package nist.p_70nanb17h188.demo.pscr19.GUI;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import nist.p_70nanb17h188.demo.pscr19.Constants;
import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.link.LinkDiscoveryController;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.link.LinkLayer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Constants.mainContext = this;
        showExistingNames();
    }

    public void showExistingNames() {
        final String existingNames[] = Device.getExistingNames();
        ListView nameListView = findViewById(R.id.existing_names_listView);
        ArrayAdapter<String> nameListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, existingNames);
        nameListView.setAdapter(nameListAdapter);
        nameListAdapter.notifyDataSetChanged();

        nameListView.setClickable(true);
        nameListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Device.setName(existingNames[i]);
                LinkLayer.init();
            }
        });
    }
}

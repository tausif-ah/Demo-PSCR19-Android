package nist.p_70nanb17h188.demo.pscr19.gui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.R;

public class LauncherActivity extends AppCompatActivity {
    private ListView nameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        nameList = findViewById(R.id.launcher_names);
        nameList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Device.getExistingNames()));
        nameList.setOnItemClickListener(this::onNameClicked);
    }

    private void onNameClicked(AdapterView<?> parent, View view, int position, long id) {
        Device.setName((String) parent.getItemAtPosition(position));
//        LinkLayer.init();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

//    public void showExistingNames() {
//        final String existingNames[] = Device.getExistingNames();
//        ListView nameListView = findViewById(R.id.existing_names_listView);
//        ArrayAdapter<String> nameListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, existingNames);
//        nameListView.setAdapter(nameListAdapter);
//        nameListAdapter.notifyDataSetChanged();
//
//        nameListView.setClickable(true);
//        nameListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Device.setName(existingNames[i]);
//                LinkLayer.init();
//                updateUIAfterNameSelection();
//            }
//        });
//    }

//    public void updateUIAfterNameSelection() {
//        ListView existingNamesView = findViewById(R.id.existing_names_listView);
//        existingNamesView.setVisibility(View.INVISIBLE);
//        LinearLayout bottomPanel = findViewById(R.id.bottomButtonPanel);
//        Button commonButton = findViewById(R.id.commonButton);
//        if (Device.getName().equals("Mule"))
//            commonButton.setText("Name Routing Report");
//        else if (Device.getName().equals("M1") || Device.getName().equals("M2"))
//            commonButton.setText("Offload Master");
//        else
//            commonButton.setText("Worker");
//        bottomPanel.setVisibility(View.VISIBLE);
//    }
}

package nist.p_70nanb17h188.demo.pscr19.GUI;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import nist.p_70nanb17h188.demo.pscr19.Constants;
import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.link.LinkLayer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Constants.mainContext = this;
        setInitialUI();
    }

    public void setInitialUI() {

        Button messaging = findViewById(R.id.messagingButton);
        messaging.setTag(Constants.MESSAGING_BUTTON);

        Button linkStatus = findViewById(R.id.linkStatusButton);
        linkStatus.setTag(Constants.LINK_STATUS_BUTTON);

        Button common = findViewById(R.id.commonButton);
        common.setTag(Constants.COMMON_BUTTON);

        LinearLayout bottomPanel = findViewById(R.id.bottomButtonPanel);
        bottomPanel.setVisibility(View.INVISIBLE);

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
                updateUIAfterNameSelection();
            }
        });
    }

    public void updateUIAfterNameSelection() {
        ListView existingNamesView = findViewById(R.id.existing_names_listView);
        existingNamesView.setVisibility(View.INVISIBLE);
        LinearLayout bottomPanel = findViewById(R.id.bottomButtonPanel);
        Button commonButton = findViewById(R.id.commonButton);
        if (Device.getName().equals("Mule"))
            commonButton.setText("Name Routing Report");
        else if (Device.getName().equals("M1") || Device.getName().equals("M2"))
            commonButton.setText("Offload Master");
        else
            commonButton.setText("Worker");
        bottomPanel.setVisibility(View.VISIBLE);
    }

    public void buttonClicked(View view) {
        int tag = (int)view.getTag();
        if (tag == Constants.MESSAGING_BUTTON) {
            Intent intent = new Intent(this, MessagingActivity.class);
            startActivity(intent);
        }
        else if (tag == Constants.LINK_STATUS_BUTTON) {
            Intent intent = new Intent(this, LinkStatusActivity.class);
            startActivity(intent);
        }
        else {
            if (Device.getName().equals("Mule")) {
                Intent intent = new Intent(this, NameRoutingActivity.class);
                startActivity(intent);
            }
            else if (Device.getName().equals("M1") || Device.getName().equals("M2")) {
                Intent intent = new Intent(this, OffloadMasterActivity.class);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent(this, OffloadWorkerActivity.class);
                startActivity(intent);
            }
        }
    }
}

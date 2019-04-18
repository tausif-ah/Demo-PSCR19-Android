package nist.p_70nanb17h188.demo.pscr19.GUI;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import java.util.ArrayList;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.link.Link;

public class LinkStatusActivity extends AppCompatActivity {

    ArrayList<Link> links;
    ListView linksListView;
    LinksListAdapter linksListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_status);
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    void init() {
        linksListView = findViewById(R.id.links_listView);
        links = new ArrayList<>();
        linksListAdapter = new LinksListAdapter(this, links);
        linksListView.setAdapter(linksListAdapter);
    }

    public void linksChanged() {
        linksListAdapter.notifyDataSetChanged();
    }
}

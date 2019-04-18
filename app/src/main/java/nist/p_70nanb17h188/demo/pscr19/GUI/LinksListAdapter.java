package nist.p_70nanb17h188.demo.pscr19.GUI;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import nist.p_70nanb17h188.demo.pscr19.Constants;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.link.Link;

public class LinksListAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<Link> links;

    LinksListAdapter(Context context, ArrayList<Link> links) {
        mContext = context;
        this.links = links;
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return links.size();
    }

    @Override
    public Object getItem(int i) {
        return links.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Link currentLink = links.get(i);
        View rowView;
        rowView = mInflater.inflate(R.layout.links_list_row_view, viewGroup, false);
        String linkName;
        String linkType;
        if (currentLink.linkType == Constants.WIFI_DIRECT_LINK) {
            linkName = currentLink.wifiP2pDevice.deviceName;
            linkType = "WiFi Direct";
        }
        else {
            linkName = currentLink.bluetoothDevice.getName();
            linkType = "Bluetooth";
        }
        TextView linkNameText = rowView.findViewById(R.id.link_name_textView);
        linkNameText.setText(linkName);
        TextView linkTypeText = rowView.findViewById(R.id.link_type_textView);
        linkTypeText.setText(linkType);
        Button connect = rowView.findViewById(R.id.connect_button);
        connect.setTag(i);
        return rowView;
    }
}

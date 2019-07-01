package nist.p_70nanb17h188.demo.pscr19.gui.net;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class RelationshipListArrayAdapter extends ArrayAdapter<Name> {
    RelationshipListArrayAdapter(@NonNull Context context, @NonNull List<Name> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
        View view = super.getView(position, convertView, parent);
        TextView textView = view.findViewById(android.R.id.text1);
        Name name = getItem(position);
        if (name == null) {
            textView.setText("----");
            textView.setBackgroundResource(R.color.colorLinkUnknown);
        } else {
            MessagingNamespace.MessagingName mn = namespace.getName(name);
            if (mn == null) {
                textView.setText(name.toString());
                textView.setBackgroundResource(R.color.colorLinkUnknown);
            } else {
                String[] incidents = MessagingNamespace.getDefaultInstance().getNameIncidents(mn.getName());
                textView.setText(String.format("%s %s %s",
                        mn.getAppName(),
                        mn.getName(),
                        incidents.length == 0 ? "" : Arrays.toString(incidents)));
                textView.setBackgroundResource(Constants.getNameTypeColorResource(mn.getType()));
            }
        }
        return view;
    }
}

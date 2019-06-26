package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

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
    public RelationshipListArrayAdapter(@NonNull Context context, @NonNull List<Name> objects) {
        super(context, android.R.layout.simple_list_item_1, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = view.findViewById(android.R.id.text1);
        Name n = getItem(position);
        MessagingNamespace.MessagingName mn = n == null ? null : MessagingNamespace.getDefaultInstance().getName(n);
        if (mn == null) {
            textView.setText(n == null ? "NULL" : n.toString());
            textView.setBackgroundResource(R.color.colorLinkUnknown);
        } else {
            String[] incidents = MessagingNamespace.getDefaultInstance().getNameIncidents(n);
            textView.setText(String.format("%s %s",
                    mn.getAppName(),
                    incidents.length == 0 ? "" : Arrays.toString(incidents)));
            textView.setBackgroundResource(Constants.getNameTypeColorResource(mn.getType()));
        }
        return view;
    }
}

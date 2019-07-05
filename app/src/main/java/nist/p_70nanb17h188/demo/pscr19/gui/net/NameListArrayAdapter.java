package nist.p_70nanb17h188.demo.pscr19.gui.net;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingName;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class NameListArrayAdapter extends ArrayAdapter<Name> {

    NameListArrayAdapter(@NonNull Context context, @NonNull List<Name> objects) {
        super(context, android.R.layout.simple_spinner_dropdown_item, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(convertView, parent, android.R.layout.simple_spinner_dropdown_item, getItem(position));
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(convertView, parent, android.R.layout.simple_spinner_dropdown_item, getItem(position));
    }

    private View getView(@Nullable View convertView, @NonNull ViewGroup parent, int resourceId, Name name) {
        MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
        TextView textView;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            textView = convertView.findViewById(android.R.id.text1);
            convertView.setTag(textView);
        } else {
            textView = (TextView) convertView.getTag();
        }
        if (name == null) {
            textView.setText("----");
            textView.setBackgroundResource(R.color.colorLinkUnknown);
        } else {
            textView.setText(getNameRepresentation(name));
            MessagingName mn = namespace.getName(name);
            if (mn == null) {
                textView.setBackgroundResource(R.color.colorLinkUnknown);
            } else {
                textView.setBackgroundResource(Constants.getNameTypeColorResource(mn.getType()));
            }
        }
        return convertView;
    }

    private String getNameRepresentation(MessagingName mn) {
        String[] incidents = MessagingNamespace.getDefaultInstance().getNameIncidents(mn.getName());
        return String.format("%s %s %s", mn.getAppName(), mn.getName(), incidents.length == 0 ? "" : Arrays.toString(incidents));
    }

    private String getNameRepresentation(Name n) {
        MessagingName mn = MessagingNamespace.getDefaultInstance().getName(n);
        if (mn == null) return n.toString();
        else return getNameRepresentation(mn);
    }
}

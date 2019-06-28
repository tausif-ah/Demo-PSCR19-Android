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

import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;

public class NameListArrayAdapter extends ArrayAdapter<MessagingNamespace.MessagingName> {
    NameListArrayAdapter(@NonNull Context context, @NonNull List<MessagingNamespace.MessagingName> objects) {
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

    private View getView(@Nullable View convertView, @NonNull ViewGroup parent, int resourceId, MessagingNamespace.MessagingName name) {
        TextView textView;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            textView = convertView.findViewById(android.R.id.text1);
            convertView.setTag(textView);
        } else {
            textView = (TextView) convertView.getTag();
        }
        textView.setBackgroundResource(Constants.getNameTypeColorResource(name.getType()));
        String[] incidents = MessagingNamespace.getDefaultInstance().getNameIncidents(name.getName());
        textView.setText(String.format("%s %s",
                name.getAppName(),
                incidents.length == 0 ? "" : Arrays.toString(incidents)
        ));

        return convertView;
    }
}

package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.net.Constants;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingName;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class NameListArrayAdapter extends ArrayAdapter<Name> {
    private final boolean showName;
    private final ArrayList<Name> allItems = new ArrayList<>();
    private Filter nameFilter = new Filter() {
        @Override
        public String convertResultToString(Object resultValue) {
            return getNameRepresentation((Name) resultValue);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<Name> suggestions = new ArrayList<>();
            if (constraint != null) {
                String input = constraint.toString().toLowerCase();
                for (Name n : allItems) {
                    String representation = getNameRepresentation(n).toLowerCase();
                    if (representation.contains(input)) suggestions.add(n);
                }
            } else {
                for (int i = 0; i < getCount(); i++) {
                    Name n = getItem(i);
                    suggestions.add(n);
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = suggestions;
            filterResults.count = suggestions.size();
            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ArrayList<Name> filteredList = (ArrayList<Name>) results.values;
            NameListArrayAdapter.super.clear();
            if (results.count > 0)
                NameListArrayAdapter.super.addAll(filteredList);
            NameListArrayAdapter.super.notifyDataSetChanged();
        }
    };

    NameListArrayAdapter(@NonNull Context context, boolean showName, @NonNull List<Name> objects) {
        super(context, android.R.layout.simple_spinner_dropdown_item, objects);
        this.showName = showName;
        allItems.addAll(objects);
    }

    void clearNames() {
        super.clear();
        allItems.clear();
    }

    void addName(Name object) {
        allItems.add(object);
    }

    @Override
    public void add(@Nullable Name object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(@NonNull Collection<? extends Name> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(Name... items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
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
        if (showName)
            return String.format("%s %s %s", mn.getAppName(), mn.getName(), incidents.length == 0 ? "" : Arrays.toString(incidents));
        else
            return String.format("%s %s", mn.getAppName(), incidents.length == 0 ? "" : Arrays.toString(incidents));

    }

    private String getNameRepresentation(Name n) {
        MessagingName mn = MessagingNamespace.getDefaultInstance().getName(n);
        if (mn == null) return n.toString();
        else return getNameRepresentation(mn);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return nameFilter;
    }
}


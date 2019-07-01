package nist.p_70nanb17h188.demo.pscr19.gui.net;


import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Namespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

/**
 * A simple {@link Fragment} subclass.
 */
public class NamingFragment extends Fragment {

    private static final Comparator<MessagingNamespace.MessagingName> DEFAULT_MESSAGING_NAME_COMPARATOR = (n1, n2) -> n1.getAppName().toLowerCase().compareTo(n2.getAppName().toLowerCase());
    private static final Comparator<Name> DEFAULT_NAME_COMPARATOR = (n1, n2) -> Long.compare(n1.getValue(), n2.getValue());

    public static class NamingFragmentViewModel extends ViewModel {
        final MutableLiveData<Name> currName = new MutableLiveData<>();
        final MutableLiveData<Boolean> usingMessagingNamespace = new MutableLiveData<>();

        public NamingFragmentViewModel() {
            MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
            currName.setValue(namespace.getIncidentRoot());
            usingMessagingNamespace.setValue(true);
        }
    }

    private Spinner names;
    private NameListArrayAdapter namesAdapter;
    private RelationshipListArrayAdapter childrenAdapter, parentsAdapter, ancestorsAdapter, descendantsAdapter;
    private NamingFragmentViewModel viewModel;

    public NamingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_naming, container, false);
        FragmentActivity activity = getActivity();
        assert activity != null;
        assert getContext() != null;

        viewModel = ViewModelProviders.of(activity).get(NamingFragmentViewModel.class);

        names = view.findViewById(R.id.naming_names);
        namesAdapter = new NameListArrayAdapter(getContext(), new ArrayList<>());
        names.setAdapter(namesAdapter);
        names.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Name mn = namesAdapter.getItem(position);
                if (mn != null) viewModel.currName.postValue(mn);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ListView ancestors = view.findViewById(R.id.naming_ancestors);
        ancestorsAdapter = new RelationshipListArrayAdapter(getContext(), new ArrayList<>());
        ancestors.setAdapter(ancestorsAdapter);
        ancestors.setOnItemClickListener((parent, view1, position, id) -> viewModel.currName.postValue(ancestorsAdapter.getItem(position)));

        ListView parents = view.findViewById(R.id.naming_parents);
        parentsAdapter = new RelationshipListArrayAdapter(getContext(), new ArrayList<>());
        parents.setAdapter(parentsAdapter);
        parents.setOnItemClickListener((parent, view1, position, id) -> viewModel.currName.postValue(parentsAdapter.getItem(position)));

        ListView children = view.findViewById(R.id.naming_children);
        childrenAdapter = new RelationshipListArrayAdapter(getContext(), new ArrayList<>());
        children.setAdapter(childrenAdapter);
        children.setOnItemClickListener((parent, v, position, id) -> viewModel.currName.postValue(childrenAdapter.getItem(position)));

        ListView descendants = view.findViewById(R.id.naming_descendants);
        descendantsAdapter = new RelationshipListArrayAdapter(getContext(), new ArrayList<>());
        descendants.setAdapter(descendantsAdapter);
        descendants.setOnItemClickListener((parent, view1, position, id) -> viewModel.currName.postValue(descendantsAdapter.getItem(position)));

        ToggleButton namespaceSwitch = view.findViewById(R.id.naming_namespace_switch);
        namespaceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.usingMessagingNamespace.postValue(isChecked));

        updateNames();
        viewModel.currName.observe(this, this::setName);
        viewModel.usingMessagingNamespace.observe(this, usingNamespace -> {
            assert usingNamespace != null;
            namespaceSwitch.setChecked(usingNamespace);
            updateNames();
        });

        // listen to context events
        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).registerReceiver(
                onNamespaceChangeReceived,
                new IntentFilter().addAction(MessagingNamespace.ACTION_NAMESPACE_CHANGED));
        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).registerReceiver(
                onAppNameChangeReceived,
                new IntentFilter().addAction(MessagingNamespace.ACTION_APPNAME_CHANGED));
        Context.getContext(Namespace.CONTEXT_NAMESPACE).registerReceiver(
                onNamespaceChangeReceived,
                new IntentFilter().addAction(Namespace.ACTION_NAME_CHANGED).addAction(Namespace.ACTION_RELATIONSHIP_CHANGED)
        );
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.currName.removeObservers(this);
        viewModel.usingMessagingNamespace.removeObservers(this);
        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).unregisterReceiver(onNamespaceChangeReceived);
        Context.getContext(Namespace.CONTEXT_NAMESPACE).unregisterReceiver(onNamespaceChangeReceived);

    }

    private final BroadcastReceiver onNamespaceChangeReceived = (context, intent) -> updateNames();

    private final BroadcastReceiver onAppNameChangeReceived = (context, intent) -> {
        namesAdapter.notifyDataSetChanged();
        ancestorsAdapter.notifyDataSetChanged();
        parentsAdapter.notifyDataSetChanged();
        childrenAdapter.notifyDataSetChanged();
        descendantsAdapter.notifyDataSetChanged();
    };

    private void updateNames() {
        MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
        Name currName = viewModel.currName.getValue();

        Boolean usingMessagingNamespace = viewModel.usingMessagingNamespace.getValue();
        assert usingMessagingNamespace != null;


        namesAdapter.clear();
        if (usingMessagingNamespace) {
            MessagingNamespace.MessagingName[] allNames = namespace.getAllNames().toArray(new MessagingNamespace.MessagingName[0]);
            Arrays.sort(allNames, DEFAULT_MESSAGING_NAME_COMPARATOR);
            for (MessagingNamespace.MessagingName name : allNames) namesAdapter.add(name.getName());

        } else {
            ArrayList<Name> allNameList = new ArrayList<>();
            NetLayer.forEachName(allNameList::add);
            Name[] allNames = allNameList.toArray(new Name[0]);
            Arrays.sort(allNames, DEFAULT_NAME_COMPARATOR);
            for (Name name : allNames) namesAdapter.add(name);
        }
        namesAdapter.notifyDataSetChanged();

        // if current name is removed, go back to incident root
        if (currName == null || namesAdapter.getPosition(currName) < 0)
            currName = namespace.getIncidentRoot();

        viewModel.currName.postValue(currName);
        setName(currName);
    }

    private void setName(Name name) {
        MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
        Boolean usingMessagingNamespace = viewModel.usingMessagingNamespace.getValue();
        assert usingMessagingNamespace != null;
        if (name == null) return;
        names.setSelection(namesAdapter.getPosition(name));

        ancestorsAdapter.clear();
        parentsAdapter.clear();
        childrenAdapter.clear();
        descendantsAdapter.clear();
        if (usingMessagingNamespace) {
            MessagingNamespace.MessagingName mn = namespace.getName(name);
            if (mn != null) {
                HashSet<MessagingNamespace.MessagingName> parents = new HashSet<>();
                namespace.forEachParent(mn, parents::add);
                MessagingNamespace.MessagingName[] parentsNames = parents.toArray(new MessagingNamespace.MessagingName[0]);
                Arrays.sort(parentsNames, DEFAULT_MESSAGING_NAME_COMPARATOR);
                for (MessagingNamespace.MessagingName parentName : parentsNames)
                    parentsAdapter.add(parentName.getName());

                HashSet<MessagingNamespace.MessagingName> ancestors = new HashSet<>();
                namespace.forEachAncestor(mn, ancestors::add);
                ancestors.removeAll(parents);
                ancestors.remove(mn);
                MessagingNamespace.MessagingName[] ancestorsNames = ancestors.toArray(new MessagingNamespace.MessagingName[0]);
                Arrays.sort(ancestorsNames, DEFAULT_MESSAGING_NAME_COMPARATOR);
                for (MessagingNamespace.MessagingName ancestorName : ancestorsNames)
                    ancestorsAdapter.add(ancestorName.getName());

                HashSet<MessagingNamespace.MessagingName> children = new HashSet<>();
                namespace.forEachChild(mn, children::add);
                MessagingNamespace.MessagingName[] childrenNames = children.toArray(new MessagingNamespace.MessagingName[0]);
                Arrays.sort(childrenNames, DEFAULT_MESSAGING_NAME_COMPARATOR);
                for (MessagingNamespace.MessagingName childName : childrenNames)
                    childrenAdapter.add(childName.getName());

                HashSet<MessagingNamespace.MessagingName> descendants = new HashSet<>();
                namespace.forEachDescendant(mn, descendants::add);
                descendants.removeAll(children);
                descendants.remove(mn);
                MessagingNamespace.MessagingName[] descendantsNames = descendants.toArray(new MessagingNamespace.MessagingName[0]);
                Arrays.sort(descendantsNames, DEFAULT_MESSAGING_NAME_COMPARATOR);
                for (MessagingNamespace.MessagingName descendantName : descendantsNames)
                    descendantsAdapter.add(descendantName.getName());
            }
        } else {
            HashSet<Name> parents = new HashSet<>();
            NetLayer.forEachParent(name, parents::add);
            Name[] parentsNames = parents.toArray(new Name[0]);
            Arrays.sort(parentsNames, DEFAULT_NAME_COMPARATOR);
            for (Name parentName : parentsNames) parentsAdapter.add(parentName);

            HashSet<Name> ancestors = new HashSet<>();
            NetLayer.forEachAncestor(name, ancestors::add);
            ancestors.removeAll(parents);
            ancestors.remove(name);
            Name[] ancestorsNames = ancestors.toArray(new Name[0]);
            Arrays.sort(ancestorsNames, DEFAULT_NAME_COMPARATOR);
            for (Name ancestorName : ancestorsNames) ancestorsAdapter.add(ancestorName);

            HashSet<Name> children = new HashSet<>();
            NetLayer.forEachChild(name, children::add);
            Name[] childrenNames = children.toArray(new Name[0]);
            Arrays.sort(childrenNames, DEFAULT_NAME_COMPARATOR);
            for (Name childName : childrenNames) childrenAdapter.add(childName);

            HashSet<Name> descendants = new HashSet<>();
            NetLayer.forEachDescendant(name, descendants::add);
            descendants.removeAll(children);
            descendants.remove(name);
            Name[] descendantsNames = descendants.toArray(new Name[0]);
            Arrays.sort(descendantsNames, DEFAULT_NAME_COMPARATOR);
            for (Name descendantName : descendantsNames) descendantsAdapter.add(descendantName);
        }
        ancestorsAdapter.notifyDataSetChanged();
        parentsAdapter.notifyDataSetChanged();
        childrenAdapter.notifyDataSetChanged();
        descendantsAdapter.notifyDataSetChanged();
    }


}

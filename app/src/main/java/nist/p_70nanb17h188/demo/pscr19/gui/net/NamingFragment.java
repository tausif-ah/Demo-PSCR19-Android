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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

/**
 * A simple {@link Fragment} subclass.
 */
public class NamingFragment extends Fragment {

    public static class NamingFragmentViewModel extends ViewModel {
        final MutableLiveData<MessagingNamespace.MessagingName> currName = new MutableLiveData<>();

        public NamingFragmentViewModel() {
            MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
            currName.setValue(namespace.getName(namespace.getIncidentRoot()));
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
                MessagingNamespace.MessagingName mn = namesAdapter.getItem(position);
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

        updateNames();
        viewModel.currName.observe(this, this::setName);

        // listen to context events
        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).registerReceiver(
                onNamespaceChangeReceived,
                new IntentFilter().addAction(MessagingNamespace.ACTION_NAMESPACE_CHANGED));
        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).registerReceiver(
                onAppNameChangeReceived,
                new IntentFilter().addAction(MessagingNamespace.ACTION_APPNAME_CHANGED));

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.currName.removeObservers(this);
        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).unregisterReceiver(onNamespaceChangeReceived);

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
        MessagingNamespace.MessagingName currName = viewModel.currName.getValue();

        MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
        MessagingNamespace.MessagingName[] allNames = namespace.getAllNames().toArray(new MessagingNamespace.MessagingName[0]);
        Arrays.sort(allNames, (n1, n2) -> n1.getAppName().toLowerCase().compareTo(n2.getAppName().toLowerCase()));
        namesAdapter.clear();
        namesAdapter.addAll(allNames);
        namesAdapter.notifyDataSetChanged();

        // if current name is removed, go back to incident root
        if (currName == null || namespace.getName(currName.getName()) == null) {
            currName = namespace.getName(namespace.getIncidentRoot());
        }
        viewModel.currName.postValue(currName);
        setName(currName);
    }

    private void setName(MessagingNamespace.MessagingName mn) {
        MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
        if (mn == null) return;
        names.setSelection(namesAdapter.getPosition(mn));

        HashSet<MessagingNamespace.MessagingName> children = new HashSet<>();
        namespace.forEachChild(mn, children::add);
        childrenAdapter.clear();
        childrenAdapter.addAll(children);
        childrenAdapter.notifyDataSetChanged();

        HashSet<MessagingNamespace.MessagingName> parents = new HashSet<>();
        namespace.forEachParent(mn, parents::add);
        parentsAdapter.clear();
        parentsAdapter.addAll(parents);
        parentsAdapter.notifyDataSetChanged();

        HashSet<MessagingNamespace.MessagingName> ancestors = new HashSet<>();
        namespace.forEachAncestor(mn, ancestors::add);
        ancestors.remove(mn);
        ancestors.removeAll(parents);
        ancestorsAdapter.clear();
        ancestorsAdapter.addAll(ancestors);
        ancestorsAdapter.notifyDataSetChanged();

        HashSet<MessagingNamespace.MessagingName> descendants = new HashSet<>();
        namespace.forEachDescendant(mn, descendants::add);
        descendants.remove(mn);
        descendants.removeAll(children);
        descendantsAdapter.clear();
        descendantsAdapter.addAll(descendants);
        descendantsAdapter.notifyDataSetChanged();

    }


}

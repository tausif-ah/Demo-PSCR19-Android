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
import nist.p_70nanb17h188.demo.pscr19.gui.messaging.NameListArrayAdapter;
import nist.p_70nanb17h188.demo.pscr19.gui.messaging.RelationshipListArrayAdapter;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Namespace;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

/**
 * A simple {@link Fragment} subclass.
 */
public class NamingFragment extends Fragment {
    private static final String KEY_CURRENT_NAME = "nist.p_70nanb17h188.demo.pscr19.gui.net.NamingFragment.currentName";

    public static class NamingFragmentViewModel extends ViewModel {
        final MutableLiveData<Name> currName = new MutableLiveData<>();

        public NamingFragmentViewModel() {
            currName.setValue(MessagingNamespace.getDefaultInstance().getIncidentRoot());
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
        MessagingNamespace.MessagingName[] allNames = MessagingNamespace.getDefaultInstance().getAllNodes().toArray(new MessagingNamespace.MessagingName[0]);
        Arrays.sort(allNames, (n1, n2) -> n1.getAppName().toLowerCase().compareTo(n2.getAppName().toLowerCase()));

        namesAdapter = new NameListArrayAdapter(getContext(), new ArrayList<>(Arrays.asList(allNames)));
        names.setAdapter(namesAdapter);
        names.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MessagingNamespace.MessagingName mn = namesAdapter.getItem(position);
                if (mn != null) viewModel.currName.postValue(mn.getName());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        ListView ancestors = view.findViewById(R.id.naming_ancestors);
        ancestorsAdapter = new RelationshipListArrayAdapter(getContext(), new ArrayList<>());
        ancestors.setAdapter(ancestorsAdapter);
        ancestors.setOnItemClickListener((parent, view1, position, id) -> {
            Name n = ancestorsAdapter.getItem(position);
            MessagingNamespace.MessagingName mn = n == null ? null : MessagingNamespace.getDefaultInstance().getName(n);
            if (mn != null) viewModel.currName.postValue(n);
        });

        ListView parents = view.findViewById(R.id.naming_parents);
        parentsAdapter = new RelationshipListArrayAdapter(getContext(), new ArrayList<>());
        parents.setAdapter(parentsAdapter);
        parents.setOnItemClickListener((parent, view1, position, id) -> {
            Name n = parentsAdapter.getItem(position);
            MessagingNamespace.MessagingName mn = n == null ? null : MessagingNamespace.getDefaultInstance().getName(n);
            if (mn != null) viewModel.currName.postValue(n);
        });

        ListView children = view.findViewById(R.id.naming_children);
        childrenAdapter = new RelationshipListArrayAdapter(getContext(), new ArrayList<>());
        children.setAdapter(childrenAdapter);
        children.setOnItemClickListener((parent, v, position, id) -> {
            Name n = childrenAdapter.getItem(position);
            MessagingNamespace.MessagingName mn = n == null ? null : MessagingNamespace.getDefaultInstance().getName(n);
            if (mn != null) viewModel.currName.postValue(n);
        });

        ListView descendants = view.findViewById(R.id.naming_descendants);
        descendantsAdapter = new RelationshipListArrayAdapter(getContext(), new ArrayList<>());
        descendants.setAdapter(descendantsAdapter);
        descendants.setOnItemClickListener((parent, view1, position, id) -> {
            Name n = descendantsAdapter.getItem(position);
            MessagingNamespace.MessagingName mn = n == null ? null : MessagingNamespace.getDefaultInstance().getName(n);
            if (mn != null) viewModel.currName.postValue(n);
        });

        viewModel.currName.observe(this, this::setName);

        // TODO: listen to context events
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.currName.removeObservers(this);

    }

    private void setName(Name n) {
        MessagingNamespace.MessagingName mn = MessagingNamespace.getDefaultInstance().getName(n);
        if (mn == null) return;
        names.setSelection(namesAdapter.getPosition(mn));

        Namespace namespace = NetLayer.getDefaultInstance().getNamespace();
        HashSet<Name> children = new HashSet<>();
        namespace.forEachChild(n, children::add);
        childrenAdapter.clear();
        childrenAdapter.addAll(children);
        childrenAdapter.notifyDataSetChanged();

        HashSet<Name> parents = new HashSet<>();
        namespace.forEachParent(n, parents::add);
        parentsAdapter.clear();
        parentsAdapter.addAll(parents);
        parentsAdapter.notifyDataSetChanged();

        HashSet<Name> ancestors = new HashSet<>();
        namespace.forEachAncestor(n, ancestors::add);
        ancestors.remove(n);
        ancestors.removeAll(parents);
        ancestorsAdapter.clear();
        ancestorsAdapter.addAll(ancestors);
        ancestorsAdapter.notifyDataSetChanged();

        HashSet<Name> descendants = new HashSet<>();
        namespace.forEachDescendant(n, descendants::add);
        descendants.remove(n);
        descendants.removeAll(children);
        descendantsAdapter.clear();
        descendantsAdapter.addAll(descendants);
        descendantsAdapter.notifyDataSetChanged();

    }


}

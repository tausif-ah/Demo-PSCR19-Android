package nist.p_70nanb17h188.demo.pscr19.gui.link;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
//import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

/**
 * A simple {@link Fragment} subclass.
 */
public class LinkFragment extends Fragment {
    // public static final String TAG = "LinkFragment";
    private LinkFragmentViewModel viewModel;

    public LinkFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getActivity() != null;
        viewModel = ViewModelProviders.of(getActivity()).get(LinkFragmentViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_link, container, false);

        LinearLayout groupInfo = view.findViewById(R.id.link_group_info);
        TextView txtGroupName = view.findViewById(R.id.link_group_name);
        TextView txtGroupPass = view.findViewById(R.id.link_group_pass);
        viewModel.wifiGroupInfo.observe(this, p -> {
            if (p == null || !p.isGroupOwner()) {
                groupInfo.setVisibility(View.GONE);
            } else {
                groupInfo.setVisibility(View.VISIBLE);
                txtGroupName.setText(p.getNetworkName());
                txtGroupPass.setText(p.getPassphrase());
            }
        });
        Button txtWifiDiscoverUpdate = view.findViewById(R.id.link_wifi_discover_update);
        txtWifiDiscoverUpdate.setText(LinkFragmentViewModel.DATE_STRING_ON_NULL);
        viewModel.strWifiDiscoverUpdateTime.observe(this, timeString -> txtWifiDiscoverUpdate.setText(String.format(Locale.US, "Wifi-D: %s", timeString)));
        Button txtBluetoothUpdate = view.findViewById(R.id.link_bluetooth_update);
        txtBluetoothUpdate.setText(LinkFragmentViewModel.DATE_STRING_ON_NULL);
        viewModel.strBluetoothUpdateTime.observe(this, timeString -> txtBluetoothUpdate.setText(String.format(Locale.US, "BT: %s", timeString)));
        viewModel.wifiDiscovering.observe(this, discovering -> txtWifiDiscoverUpdate.setCompoundDrawablesWithIntrinsicBounds(0, 0, Constants.getDiscoverStatusImageResource(discovering), 0));
        viewModel.bluetoothDiscovering.observe(this, discovering -> txtBluetoothUpdate.setCompoundDrawablesWithIntrinsicBounds(0, 0, Constants.getDiscoverStatusImageResource(discovering), 0));
        txtWifiDiscoverUpdate.setOnClickListener(v -> LinkLayer.getDefaultImplementation().getWifiLinkManager().discoverPeers());
        txtBluetoothUpdate.setOnClickListener(v -> {
//            Context.getContext(Helper.CONTEXT_USER_INTERFACE).sendBroadcast(new Intent(Helper.ACTION_NOTIFY_USER).putExtra(Helper.EXTRA_NOTIFICATION_TYPE, LogType.Info).putExtra(Helper.EXTRA_NOTIFICATION_CONTENT, "txtBluetoothUpdate clicked!"));
        });

        RecyclerView list = view.findViewById(R.id.link_links);
        LinearLayoutManager listLayoutManager = new WrapLinearLayoutManager(view.getContext());
        list.setLayoutManager(listLayoutManager);
        list.setAdapter(new RecyclerView.Adapter<LinkViewHolder>() {
            @NonNull
            @Override
            public LinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_link2, parent, false);
                return new LinkViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull LinkViewHolder viewHolder, int i) {
                viewHolder.bind(viewModel.links[i]);
            }

            @Override
            public int getItemCount() {
                return viewModel.links.length;
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        viewModel.wifiGroupInfo.removeObservers(this);
        viewModel.strWifiDiscoverUpdateTime.removeObservers(this);
        viewModel.strBluetoothUpdateTime.removeObservers(this);
        viewModel.wifiDiscovering.removeObservers(this);
        viewModel.bluetoothDiscovering.removeObservers(this);
        for (Link l : viewModel.links) {
            l.getStatus().removeObservers(this);
            l.getEstablishConnection().removeObservers(this);
        }
        super.onDestroy();
    }

    private class LinkViewHolder extends RecyclerView.ViewHolder {
        private Button btnItem;
        private Link instance;
        //        private final TextView txtName;
        //        private final SwitchCompat swEstablish;
        //        private final ImageButton btnEstablish;
        //        private final ImageView imgStatus;
        //        private final LinearLayout container;
        private final Observer<Link.LinkStatus> linkStatusObserver = linkStatus -> updateLinkStatus();
        private final Observer<Boolean> linkEstablishObserver = establish -> updateLinkStatus();

        LinkViewHolder(@NonNull View itemView) {
            super(itemView);
            btnItem = itemView.findViewById(R.id.link_container);
//            txtName = itemView.findViewById(R.id.link_name);
//            btnEstablish = itemView.findViewById(R.id.link_connect);
//            btnEstablish.setOnClickListener(view -> instance.onEstablishConnectionClick());
//            container = itemView.findViewById(R.id.link_container);
//            imgStatus = itemView.findViewById(R.id.link_status);
            btnItem.setOnClickListener(view -> instance.onEstablishConnectionClick());
        }

        private void updateLinkStatus() {
            Link.LinkStatus status = instance.getStatus().getValue();
            Boolean establishConnection = instance.getEstablishConnection().getValue();
            assert status != null && establishConnection != null;
            btnItem.setCompoundDrawablesRelativeWithIntrinsicBounds(Constants.getLinkStatusImageResource(status), 0, Constants.getEstablishActionImageResource(establishConnection), 0);
        }

        void bind(Link link) {
            if (instance != null) {
                instance.getStatus().removeObserver(linkStatusObserver);
                instance.getEstablishConnection().removeObserver(linkEstablishObserver);
            }
            instance = link;
            link.getStatus().observe(LinkFragment.this, linkStatusObserver);
            linkStatusObserver.onChanged(link.status.getValue());

            link.getEstablishConnection().observe(LinkFragment.this, linkEstablishObserver);
            linkEstablishObserver.onChanged(link.establishConnection.getValue());

            btnItem.setText(link.name);
            assert getActivity() != null;
            btnItem.setBackgroundTintList(getResources().getColorStateList(Constants.getLinkTypeColorResource(instance.getClass()), getActivity().getTheme()));
//            txtName.setText(link.name);
//            container.setBackgroundResource(Constants.getLinkTypeColorResource(instance.getClass()));
//            assert link.getStatus().getValue() != null;
//            imgStatus.setImageResource(Constants.getLinkStatusImageResource(link.getStatus().getValue()));
        }
    }
}
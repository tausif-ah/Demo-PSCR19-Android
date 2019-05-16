package nist.p_70nanb17h188.demo.pscr19.gui.link;


import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.logic.link.WifiLinkManager;

/**
 * A simple {@link Fragment} subclass.
 */
public class LinkFragment extends Fragment {
    //        public static final String TAG = "LinkFragment";
    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static final String DATE_STRING_ON_NULL = "--:--:--.---";
    private LinkFragmentViewModel viewModel;

    public LinkFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getActivity() != null;
        viewModel = ViewModelProviders.of(getActivity()).get(LinkFragmentViewModel.class);
        viewModel.setApplication(getActivity().getApplication());
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
        TextView txtWifiDiscoverUpdate = view.findViewById(R.id.link_wifi_discover_update);
        txtWifiDiscoverUpdate.setText(DATE_STRING_ON_NULL);
        viewModel.strWifiDiscoverUpdateTime.observe(this, txtWifiDiscoverUpdate::setText);
        TextView txtBluetoothUpdate = view.findViewById(R.id.link_bluetooth_update);
        txtBluetoothUpdate.setText(DATE_STRING_ON_NULL);
        viewModel.strBluetoothUpdateTime.observe(this, txtBluetoothUpdate::setText);
        ImageView imgWifiDiscover = view.findViewById(R.id.link_wifi_discovery);
        viewModel.wifiDiscovering.observe(this, discovering -> imgWifiDiscover.setImageResource(Constants.getDiscoverStatusImageResource(discovering)));
        ImageView imgBluetoothDiscover = view.findViewById(R.id.link_bluetooth_discovery);
        viewModel.bluetoothDiscovering.observe(this, discovering -> imgBluetoothDiscover.setImageResource(Constants.getDiscoverStatusImageResource(discovering)));

        RecyclerView list = view.findViewById(R.id.link_links);
        LinearLayoutManager listLayoutManager = new WrapLinearLayoutManager(view.getContext());
        list.setLayoutManager(listLayoutManager);
        list.setAdapter(new RecyclerView.Adapter<LinkViewHolder>() {
            @NonNull
            @Override
            public LinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_link, parent, false);
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
    public void onDestroyView() {
        viewModel.wifiGroupInfo.removeObservers(this);
        viewModel.strWifiDiscoverUpdateTime.removeObservers(this);
        viewModel.strBluetoothUpdateTime.removeObservers(this);
        viewModel.wifiDiscovering.removeObservers(this);
        viewModel.bluetoothDiscovering.removeObservers(this);
        for (Link l : viewModel.links) {
            l.getStatus().removeObservers(this);
            l.getEstablishConnection().removeObservers(this);
        }
        super.onDestroyView();
    }

    private static class LinkFragmentViewModel extends ViewModel {
        final MutableLiveData<Date> wifiDiscoverUpdateTime = new MutableLiveData<>();
        final MutableLiveData<Date> bluetoothUpdateTime = new MutableLiveData<>();
        final Function<Date, String> updateTimeTransformation = d -> d == null ? DATE_STRING_ON_NULL : DEFAULT_TIME_FORMAT.format(d);
        final LiveData<String> strWifiDiscoverUpdateTime = Transformations.map(wifiDiscoverUpdateTime, updateTimeTransformation);
        final LiveData<String> strBluetoothUpdateTime = Transformations.map(bluetoothUpdateTime, updateTimeTransformation);
        final MutableLiveData<WifiP2pGroup> wifiGroupInfo = new MutableLiveData<>();
        final MutableLiveData<Boolean> wifiDiscovering = new MutableLiveData<>(), bluetoothDiscovering = new MutableLiveData<>();
        final Link[] links;
        Application application;
        BroadcastReceiver receiver;

        public LinkFragmentViewModel() {
            links = Constants.getConnections();
            Arrays.sort(links, 0, links.length, (a, b) -> a.name.compareTo(b.name));
        }

        void setApplication(Application application) {
            if (this.application == null) {
                this.application = application;
                Context context = application.getApplicationContext();
                IntentFilter filter = new IntentFilter();
                filter.addAction(WifiLinkManager.ACTION_WIFI_GROUP_CHANGED);
                filter.addAction(WifiLinkManager.ACTION_WIFI_DISCOVERY_STATE_CHANGED);
                filter.addAction(WifiLinkManager.ACTION_WIFI_LIST_CHANGED);

                context.registerReceiver(receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context ctx, Intent intent) {
                        String action = intent.getAction();
                        if (action == null) return;
                        switch (action) {
                            case WifiLinkManager.ACTION_WIFI_DISCOVERY_STATE_CHANGED:
                                wifiDiscovering.postValue(intent.getBooleanExtra(WifiLinkManager.EXTRA_IS_DISCOVERING, false));
                                break;
                            case WifiLinkManager.ACTION_WIFI_GROUP_CHANGED:
                                wifiGroupInfo.postValue(intent.getParcelableExtra(WifiLinkManager.EXTRA_GROUP_INFO));
                                break;
                            case WifiLinkManager.ACTION_WIFI_LIST_CHANGED:
                                wifiDiscoverUpdateTime.postValue((Date) intent.getSerializableExtra(WifiLinkManager.EXTRA_TIME));
                                updateWifiDeviceList(intent.getParcelableExtra(WifiLinkManager.EXTRA_DEVICE_LIST));
                                break;
                        }
                    }
                }, filter);
                synchronizeData();
            }

        }

        @Override
        protected void onCleared() {
            super.onCleared();
            if (application != null) {
                application.getApplicationContext().unregisterReceiver(receiver);
                application = null;
            }
        }

        private void synchronizeData() {
            WifiLinkManager wifiLinkManager = WifiLinkManager.getDefaultInstance();
            if (wifiLinkManager != null) {
                wifiGroupInfo.postValue(wifiLinkManager.getLastGroupInfo());
                wifiDiscovering.postValue(wifiLinkManager.isWifiDiscovering());
                wifiDiscoverUpdateTime.postValue(wifiLinkManager.getLastDiscoverTime());
                wifiGroupInfo.postValue(wifiLinkManager.getLastGroupInfo());
                updateWifiDeviceList(wifiLinkManager.getLastDiscoverList());
            }

            //if bluetoothLinkManager != null
            {
                bluetoothDiscovering.postValue(false);
                bluetoothUpdateTime.postValue(null);
            }
        }

        private void updateWifiDeviceList(WifiP2pDeviceList list) {
//            Log.d(TAG, "updateWifiDeviceList, list=%s", list);
            if (list == null) return;
            HashMap<String, LinkWifiDirect> remaining = new HashMap<>();
            for (Link l : links) {
                if (l instanceof LinkWifiDirect) remaining.put(l.name, (LinkWifiDirect) l);
            }
            for (WifiP2pDevice device : list.getDeviceList()) {
                String name = device.deviceName;
                if (name.startsWith("[Phone]")) name = name.substring(7).trim();

                LinkWifiDirect l = remaining.get(name);
                if (l == null) {
                    continue;
                }
//                Log.d(TAG, "updateWifiDeviceList, name=%s, status=%d", device.deviceName, device.status);
                l.setDeviceInDiscovery(device);
                remaining.remove(name);
            }
            for (LinkWifiDirect l : remaining.values()) {
                l.setDeviceInDiscovery(null);
            }
        }
    }

    private class LinkViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtName;
        //        private final SwitchCompat swEstablish;
        private final ImageButton btnEstablish;
        private final ImageView imgStatus;
        private final LinearLayout container;
        private final Observer<Link.LinkStatus> linkStatusObserver = new Observer<Link.LinkStatus>() {
            @Override
            public void onChanged(@Nullable Link.LinkStatus linkStatus) {
//                Log.d(TAG, "updateWifiDeviceList, name=%s, linkStatus=%s", instance.name, linkStatus);
                imgStatus.setImageResource(Constants.getLinkStatusImageResource(linkStatus));
            }
        };
        private final Observer<Boolean> linkEstablishObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean establish) {
//                Log.d(TAG, "updateWifiDeviceList, name=%s, establish=%b", instance.name, establish);
                btnEstablish.setImageResource(Constants.getEstablishActionImageResource(establish));
            }
        };
        private Link instance;

        LinkViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.link_name);
            btnEstablish = itemView.findViewById(R.id.link_connect);
            btnEstablish.setOnClickListener(view -> instance.onEstablishConnectionClick());
            container = itemView.findViewById(R.id.link_container);
            imgStatus = itemView.findViewById(R.id.link_status);
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

            txtName.setText(link.name);
            container.setBackgroundResource(Constants.getLinkTypeColorResource(instance.getClass()));
            assert link.getStatus().getValue() != null;
            imgStatus.setImageResource(Constants.getLinkStatusImageResource(link.getStatus().getValue()));
        }
    }
}
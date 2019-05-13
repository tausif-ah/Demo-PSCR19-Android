package nist.p_70nanb17h188.demo.pscr19.gui;


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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.Log;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.link.Constants;
import nist.p_70nanb17h188.demo.pscr19.link.WifiLinkManager;

/**
 * A simple {@link Fragment} subclass.
 */
public class LinkFragment extends Fragment {
    public static final String TAG = "LinkFragment";
    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static final String DATE_STRING_ON_NULL = "--:--:--.---";
    private RecyclerView.Adapter<LinkViewHolder> listAdapter;

    public LinkFragment() {
        // Required empty public constructor
    }

    private LinkFragmentViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getActivity() != null;
        viewModel = ViewModelProviders.of(getActivity()).get(LinkFragmentViewModel.class);
        viewModel.setContext(getActivity().getApplicationContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_link, container, false);

        LinearLayout groupInfo = view.findViewById(R.id.link_group_info);
        TextView txtGroupName = view.findViewById(R.id.link_group_name);
        TextView txtGroupPass = view.findViewById(R.id.link_group_pass);
        viewModel.groupInfo.observe(this, p -> {
            if (p == null || p.first == null || p.second == null) {
                groupInfo.setVisibility(View.GONE);
            } else {
                groupInfo.setVisibility(View.VISIBLE);
                txtGroupName.setText(p.first);
                txtGroupPass.setText(p.second);
            }
        });
        TextView txtWifiUpdate = view.findViewById(R.id.link_wifi_update);
        txtWifiUpdate.setText(DATE_STRING_ON_NULL);
        viewModel.strWifiUpdateTime.observe(this, txtWifiUpdate::setText);
        TextView txtBluetoothUpdate = view.findViewById(R.id.link_bluetooth_update);
        txtBluetoothUpdate.setText(DATE_STRING_ON_NULL);
        viewModel.strBluetoothUpdateTime.observe(this, txtBluetoothUpdate::setText);
        ImageView imgWifiDiscover = view.findViewById(R.id.link_wifi_discovery);
        viewModel.wifiDiscovering.observe(this, discovering -> imgWifiDiscover.setImageResource((discovering == null || !discovering) ? R.drawable.ic_circle_red : R.drawable.ic_circle_green));
        ImageView imgBluetoothDiscover = view.findViewById(R.id.link_bluetooth_discovery);
        viewModel.bluetoothDiscovering.observe(this, discovering -> imgBluetoothDiscover.setImageResource((discovering == null || !discovering) ? R.drawable.ic_circle_red : R.drawable.ic_circle_green));

        RecyclerView list = view.findViewById(R.id.link_links);
        LinearLayoutManager listLayoutManager = new WrapLinearLayoutManager(view.getContext());
        list.setLayoutManager(listLayoutManager);
        list.setAdapter(listAdapter = new RecyclerView.Adapter<LinkViewHolder>() {
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
        viewModel.groupInfo.removeObservers(this);
        viewModel.strWifiUpdateTime.removeObservers(this);
        viewModel.strBluetoothUpdateTime.removeObservers(this);
        viewModel.wifiDiscovering.removeObservers(this);
        viewModel.bluetoothDiscovering.removeObservers(this);
        super.onDestroyView();
    }

    private enum LinkStatus {
        NotFound(R.drawable.ic_circle_red),
        NotConnected(R.drawable.ic_circle_yellow),
        Connected(R.drawable.ic_circle_blue),
        TCPEstablished(R.drawable.ic_circle_green);

        final int resource;

        LinkStatus(int resource) {
            this.resource = resource;
        }
    }

    private class LinkViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtName;
        private final SwitchCompat swEstablish;
        private final ImageView imgStatus;
        private final LinearLayout container;
        private Link instance;

        LinkViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.link_name);
            swEstablish = itemView.findViewById(R.id.link_connect);
            swEstablish.setOnCheckedChangeListener((view, isChecked) -> {
                if (!instance.setEstablishConnection(isChecked)) {
                    swEstablish.setChecked(!isChecked);
                }
            });
            container = itemView.findViewById(R.id.link_container);
            imgStatus = itemView.findViewById(R.id.link_status);
        }

        private final Observer<LinkStatus> linkStatusObserver = new Observer<LinkStatus>() {
            @Override
            public void onChanged(@Nullable LinkStatus linkStatus) {
                if (linkStatus != null)
                    imgStatus.setImageResource(linkStatus.resource);
            }
        };

        void bind(Link link) {
            if (instance != null) {
                instance.status.removeObserver(linkStatusObserver);
            }
            instance = link;
            link.status.observe(LinkFragment.this, linkStatusObserver);
            txtName.setText(link.name);
            swEstablish.setChecked(link.isEstablishConnection());
            container.setBackgroundResource(link.type.getColorResource());
            assert link.status.getValue() != null;
            imgStatus.setImageResource(link.status.getValue().resource);
        }
    }

    private abstract static class Link {

        final String name;
        final Constants.LinkType type;
        private boolean establishConnection;
        final MutableLiveData<LinkStatus> status = new MutableLiveData<>();

        Link(String name, Constants.LinkType type, boolean establishConnection) {
            this.name = name;
            this.type = type;
            this.establishConnection = establishConnection;
            this.status.setValue(LinkStatus.NotFound);
        }

        boolean isEstablishConnection() {
            return establishConnection;
        }

        boolean setEstablishConnection(boolean establishConnection) {
            this.establishConnection = establishConnection;
            return true;
        }
    }

    private static class WiFiDirectLink extends Link {
        private WifiP2pDevice device;

        WiFiDirectLink(String name, boolean establishConnection) {
            super(name, Constants.LinkType.WiFiDirect, establishConnection);
        }

        void setDevice(WifiP2pDevice device) {
            this.device = device;
            if (device != null) {
                if (status.getValue() == LinkStatus.NotFound) {
                    status.postValue(LinkStatus.NotConnected);
                }
                // connect
            } else {
                // This can happen, for legacy Wifi connections, and it's perfectly OK
//                if (status.getValue() == LinkStatus.Connected) {
////                    Log.d(TAG, "??? connected, but cannot find device???");
//                }
                status.postValue(LinkStatus.NotFound);
            }
        }

        @Override
        boolean setEstablishConnection(boolean establishConnection) {
            if (Constants.isWifiDirectGroupowner() && establishConnection) {
                return false;
            }
            return super.setEstablishConnection(establishConnection);
        }
    }

    private static class BluetoothLink extends Link {

        BluetoothLink(String name, boolean establishConnection) {
            super(name, Constants.LinkType.Bluetooth, establishConnection);
        }
    }


    private static class LinkFragmentViewModel extends ViewModel {
        final MutableLiveData<Date> wifiUpdateTime = new MutableLiveData<>(), bluetoothUpdateTime = new MutableLiveData<>();
        final Function<Date, String> updateTimeTransformation = d -> d == null ? DATE_STRING_ON_NULL : DEFAULT_TIME_FORMAT.format(d);
        final LiveData<String> strWifiUpdateTime = Transformations.map(wifiUpdateTime, updateTimeTransformation);
        final LiveData<String> strBluetoothUpdateTime = Transformations.map(bluetoothUpdateTime, updateTimeTransformation);
        final MutableLiveData<Pair<String, String>> groupInfo = new MutableLiveData<>();
        final MutableLiveData<Boolean> wifiDiscovering = new MutableLiveData<>(), bluetoothDiscovering = new MutableLiveData<>();
        Context context;
        BroadcastReceiver receiver;
        final Link[] links;

        public LinkFragmentViewModel() {
            List<Constants.Connection> connections = Constants.getConnections();
            links = new Link[connections.size()];
            int i = 0;
            for (Constants.Connection c : connections) {
                if (c.getType() == Constants.LinkType.WiFiDirect)
                    links[i++] = new WiFiDirectLink(c.getDestinationName(), c.isEstablishedByDefault());
                else
                    links[i++] = new BluetoothLink(c.getDestinationName(), c.isEstablishedByDefault());
            }
            Arrays.sort(links, 0, links.length, (a, b) -> a.name.compareTo(b.name));
        }

        public void setContext(Context context) {
            if (this.context == null) {
                this.context = context;
                IntentFilter filter = new IntentFilter();
                filter.addAction(WifiLinkManager.ACTION_WIFI_GROUP_CHANGED);
                filter.addAction(WifiLinkManager.ACTION_WIFI_DISCOVERY_STATE_CHANGED);
                filter.addAction(WifiLinkManager.ACTION_WIFI_LIST_CHANGED);

                context.registerReceiver(receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (action == null) return;
                        switch (action) {
                            case WifiLinkManager.ACTION_WIFI_DISCOVERY_STATE_CHANGED:
                                wifiDiscovering.postValue(intent.getBooleanExtra(WifiLinkManager.EXTRA_IS_DISCOVERING, false));
                                break;
                            case WifiLinkManager.ACTION_WIFI_GROUP_CHANGED:
                                groupInfo.postValue(new Pair<>(intent.getStringExtra(WifiLinkManager.EXTRA_GROUP_NAME), intent.getStringExtra(WifiLinkManager.EXTRA_GROUP_PASS)));
                                break;
                            case WifiLinkManager.ACTION_WIFI_LIST_CHANGED:
                                wifiUpdateTime.postValue((Date) intent.getSerializableExtra(WifiLinkManager.EXTRA_TIME));
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
            if (context != null) {
                context.unregisterReceiver(receiver);
                context = null;
            }
        }

        private void synchronizeData() {
            WifiLinkManager wifiLinkManager = WifiLinkManager.getDefaultInstance();
            if (wifiLinkManager != null) {
                groupInfo.postValue(new Pair<>(wifiLinkManager.getGroupName(), wifiLinkManager.getGroupPass()));
                wifiDiscovering.postValue(wifiLinkManager.isWifiDiscovering());
                wifiUpdateTime.postValue(wifiLinkManager.getLastDiscoverTime());
                updateWifiDeviceList(wifiLinkManager.getLastDiscoverList());
            }

            //if bluetoothLinkManager != null
            {
                bluetoothDiscovering.postValue(false);
                bluetoothUpdateTime.postValue(null);
            }
        }

        private void updateWifiDeviceList(WifiP2pDeviceList list) {
            if (list == null) return;
            HashMap<String, WiFiDirectLink> remaining = new HashMap<>();
            for (Link l : links) {
                if (l.type == Constants.LinkType.WiFiDirect)
                    remaining.put(l.name, (WiFiDirectLink) l);
            }
            for (WifiP2pDevice device : list.getDeviceList()) {
                WiFiDirectLink l = remaining.get(device.deviceName);
                if (l == null) continue;
                l.setDevice(device);
                remaining.remove(device.deviceName);
            }
            for (WiFiDirectLink l : remaining.values()) {
                l.setDevice(null);
            }
        }

    }

}

package nist.p_70nanb17h188.demo.pscr19.gui.net;


import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Digest;
import nist.p_70nanb17h188.demo.pscr19.logic.net.GossipModule;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Message;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

/**
 * A simple {@link Fragment} subclass.
 */
public class GossipFragment extends Fragment {
    private static final int MAX_MSG_SHOW_LEN = 20;

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView txtDigest, txtNonce, txtMsg;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDigest = itemView.findViewById(R.id.gossip_digest);
            txtNonce = itemView.findViewById(R.id.gossip_nonce);
            txtMsg = itemView.findViewById(R.id.gossip_content);
        }

        void bindDigest(@NonNull Digest digest) {
            txtDigest.setText(digest.toString());
            Message msg = NetLayer.getDefaultInstance().getGossipModule().getMessage(digest);
            if (msg == null) {
                txtNonce.setText("---");
                txtMsg.setText("---");
            } else {
                txtNonce.setText(String.format(Locale.US, "%016X", msg.getNonce()));
                byte[] buf = msg.getData();
                if (buf.length > MAX_MSG_SHOW_LEN) {
                    txtMsg.setText(String.format("%s...", Helper.getHexString(buf, 0, MAX_MSG_SHOW_LEN)));
                } else {
                    txtMsg.setText(Helper.getHexString(buf));
                }
            }
        }
    }

    private GossipFragmentViewModel viewModel;
    private final ArrayList<Digest> digests = new ArrayList<>();
    private boolean topScroll = true;
    private RecyclerView blacklist;
    private final RecyclerView.Adapter<MessageViewHolder> adapter = new RecyclerView.Adapter<MessageViewHolder>() {
        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gossip, parent, false);
            return new MessageViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder messageViewHolder, int i) {
            messageViewHolder.bindDigest(digests.get(digests.size() - i - 1));
        }

        @Override
        public int getItemCount() {
            return digests.size();
        }
    };

    private final BroadcastReceiver gossipReceiver = (context, intent) -> {
        switch (intent.getAction()) {
            case GossipModule.ACTION_BUFFER_CHANGED: {
                Digest digest = intent.getExtra(GossipModule.EXTRA_DIGEST);
                Boolean added = intent.getExtra(GossipModule.EXTRA_ADDED);
                assert added != null;
                int idx = digests.indexOf(digest);
                if (idx >= 0) {
                    adapter.notifyItemChanged(digests.size() - idx - 1);
                }
                break;
            }
            case GossipModule.ACTION_BLACKLIST_CHANGED: {
                Digest digest = intent.getExtra(GossipModule.EXTRA_DIGEST);
                Boolean added = intent.getExtra(GossipModule.EXTRA_ADDED);
                assert added != null;
                if (added) {
                    digests.add(digest);
                    adapter.notifyItemRangeInserted(0, 1);
                    if (topScroll) {
                        blacklist.scrollToPosition(0);
                    }
                } else {
                    int idx = digests.indexOf(digest);
                    if (idx >= 0) {
                        digests.remove(idx);
                        adapter.notifyItemRangeRemoved(digests.size() - idx, 1);
                    }
                }
                break;
            }
        }
    };

    public GossipFragment() {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_gossip, container, false);
        FragmentActivity activity = getActivity();
        assert activity != null;
        viewModel = ViewModelProviders.of(activity).get(GossipFragmentViewModel.class);

        Button btnAdd;
        ToggleButton btnFixed, btnStore;
        TextView txtNeighbors;

        btnAdd = view.findViewById(R.id.gossip_add);
        btnAdd.setOnClickListener(v -> viewModel.addClicked());

        btnFixed = view.findViewById(R.id.gossip_fixed);
        viewModel.getFixed().observe(this, fixed -> {
            assert fixed != null;
            btnFixed.setChecked(fixed);
        });
        btnFixed.setOnCheckedChangeListener((v, checked) -> viewModel.setFixed(checked));

        btnStore = view.findViewById(R.id.gossip_store);
        viewModel.getStore().observe(this, store -> {
            assert store != null;
            btnStore.setChecked(store);
        });
        btnStore.setOnCheckedChangeListener((v, checked) -> viewModel.setStore(checked));

        txtNeighbors = view.findViewById(R.id.gossip_connected_neighbors);
        viewModel.getConnectedNeighbors().observe(this, nbr -> {
            assert nbr != null;
            txtNeighbors.setText(nbr);
        });

        blacklist = view.findViewById(R.id.gossip_blacklist);
        blacklist.setLayoutManager(new WrapLinearLayoutManager(view.getContext()));
        blacklist.setAdapter(adapter);

        FloatingActionButton btnTopScroll = view.findViewById(R.id.gossip_scroll_top);
        btnTopScroll.setOnClickListener(v -> {
            topScroll = true;
            btnTopScroll.hide();
            blacklist.scrollToPosition(0);
        });
        btnTopScroll.hide();

        blacklist.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int state = blacklist.getScrollState();
                if (state == RecyclerView.SCROLL_STATE_DRAGGING || state == RecyclerView.SCROLL_STATE_SETTLING) {
                    boolean newIsTopLog = blacklist.computeVerticalScrollOffset() == 0;
                    if (newIsTopLog != topScroll) {
                        topScroll = newIsTopLog;
                        if (topScroll) btnTopScroll.hide();
                        else btnTopScroll.show();
                    }
                }
            }
        });

        digests.addAll(Arrays.asList(NetLayer.getDefaultInstance().getGossipModule().getBlacklist()));
        Context.getContext(GossipModule.CONTEXT_GOSSIP_MODULE).registerReceiver(gossipReceiver,
                new IntentFilter().addAction(GossipModule.ACTION_BLACKLIST_CHANGED)
                        .addAction(GossipModule.ACTION_BUFFER_CHANGED));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.getFixed().removeObservers(this);
        viewModel.getStore().removeObservers(this);
        viewModel.getConnectedNeighbors().removeObservers(this);
        Context.getContext(GossipModule.CONTEXT_GOSSIP_MODULE).unregisterReceiver(gossipReceiver);
    }
}

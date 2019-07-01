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
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.Tuple2;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Digest;
import nist.p_70nanb17h188.demo.pscr19.logic.net.GossipModule;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Message;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

/**
 * A simple {@link Fragment} subclass.
 */
public class GossipFragment extends Fragment {
    private static final int MAX_MSG_SHOW_LEN = 50;

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView txtDigest, txtNonce, txtMsg;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDigest = itemView.findViewById(R.id.gossip_digest);
            txtNonce = itemView.findViewById(R.id.gossip_nonce);
            txtMsg = itemView.findViewById(R.id.gossip_content);
        }

        void bindMessage(@NonNull Tuple2<Digest, Message> message) {
            txtDigest.setText(message.getV1().toString());
            Message msg = message.getV2();
            if (msg == null) {
                txtNonce.setText("---");
                txtMsg.setText("---");
            } else {
                txtNonce.setText(String.format(Locale.US, "%016X", msg.getNonce()));
                byte[] buf = msg.getData();
                if (buf.length > MAX_MSG_SHOW_LEN) {
                    txtMsg.setText(String.format(Locale.US, "[%d] %s...", buf.length, Helper.getHexString(buf, 0, MAX_MSG_SHOW_LEN)));
                } else {
                    txtMsg.setText(String.format(Locale.US, "[%d] %s", buf.length, Helper.getHexString(buf)));
                }
            }
        }
    }

    static class BlacklistViewHolder extends RecyclerView.ViewHolder {
        private TextView txtDigest;

        BlacklistViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDigest = itemView.findViewById(R.id.gossip_digest);
        }

        void bindDigest(@NonNull Digest digest) {
            txtDigest.setText(digest.toString());
        }
    }

    private GossipFragmentViewModel viewModel;
    private final ArrayList<Tuple2<Digest, Message>> messagelist = new ArrayList<>();
    private final ArrayList<Digest> blacklist = new ArrayList<>();
    private boolean messagelistTopScroll = true, blacklistTopScroll = true;
    private RecyclerView messagelistView, blacklistView;
    private TextView countsView;

    private final RecyclerView.Adapter<MessageViewHolder> messageAdapter = new RecyclerView.Adapter<MessageViewHolder>() {
        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gossip_message, parent, false);
            return new MessageViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder messageViewHolder, int i) {
            messageViewHolder.bindMessage(messagelist.get(messagelist.size() - i - 1));
        }

        @Override
        public int getItemCount() {
            return messagelist.size();
        }
    };

    private final RecyclerView.Adapter<BlacklistViewHolder> blacklistAdapter = new RecyclerView.Adapter<BlacklistViewHolder>() {
        @NonNull
        @Override
        public BlacklistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gossip_blacklist, parent, false);
            return new BlacklistViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BlacklistViewHolder blacklistViewHolder, int i) {
            blacklistViewHolder.bindDigest(blacklist.get(blacklist.size() - i - 1));
        }

        @Override
        public int getItemCount() {
            return blacklist.size();
        }
    };

    private final BroadcastReceiver gossipReceiver = (context, intent) -> {
        switch (intent.getAction()) {
            case GossipModule.ACTION_BUFFER_CHANGED: {
                Digest digest = intent.getExtra(GossipModule.EXTRA_DIGEST);
                Message msg = intent.getExtra(GossipModule.EXTRA_MESSAGE);
                Boolean added = intent.getExtra(GossipModule.EXTRA_ADDED);
                assert added != null;
                if (added) {
                    messagelist.add(new Tuple2<>(digest, msg));
                    messageAdapter.notifyItemRangeInserted(0, 1);
                    if (messagelistTopScroll) {
                        messagelistView.scrollToPosition(0);
                    }
                } else {
                    int idx = messagelist.indexOf(new Tuple2<>(digest, msg));
                    if (idx >= 0) {
                        messagelist.remove(idx);
                        messageAdapter.notifyItemRangeRemoved(messagelist.size() - idx, 1);
                    }
                }
                break;
            }
            case GossipModule.ACTION_BLACKLIST_CHANGED: {
                Digest digest = intent.getExtra(GossipModule.EXTRA_DIGEST);
                Boolean added = intent.getExtra(GossipModule.EXTRA_ADDED);
                assert added != null;
                if (added) {
                    blacklist.add(digest);
                    blacklistAdapter.notifyItemRangeInserted(0, 1);
                    if (blacklistTopScroll) {
                        blacklistView.scrollToPosition(0);
                    }
                } else {
                    int idx = blacklist.indexOf(digest);
                    if (idx >= 0) {
                        blacklist.remove(idx);
                        blacklistAdapter.notifyItemRangeRemoved(blacklist.size() - idx, 1);
                    }
                }
                break;
            }
        }
        updateCounts();
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

        blacklistView = view.findViewById(R.id.gossip_blacklist);
        blacklistView.setLayoutManager(new WrapLinearLayoutManager(view.getContext()));
        blacklistView.setAdapter(blacklistAdapter);

        FloatingActionButton btnBlacklistTopScroll = view.findViewById(R.id.gossip_blacklist_scroll_top);
        btnBlacklistTopScroll.setOnClickListener(v -> {
            blacklistTopScroll = true;
            btnBlacklistTopScroll.hide();
            blacklistView.scrollToPosition(0);
        });
        btnBlacklistTopScroll.hide();

        blacklistView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int state = blacklistView.getScrollState();
                if (state == RecyclerView.SCROLL_STATE_DRAGGING || state == RecyclerView.SCROLL_STATE_SETTLING) {
                    boolean newIsTopLog = blacklistView.computeVerticalScrollOffset() == 0;
                    if (newIsTopLog != blacklistTopScroll) {
                        blacklistTopScroll = newIsTopLog;
                        if (blacklistTopScroll) btnBlacklistTopScroll.hide();
                        else btnBlacklistTopScroll.show();
                    }
                }
            }
        });

        messagelistView = view.findViewById(R.id.gossip_message_buffer);
        messagelistView.setLayoutManager(new WrapLinearLayoutManager(view.getContext()));
        messagelistView.setAdapter(messageAdapter);

        FloatingActionButton btnMessagelistTopScroll = view.findViewById(R.id.gossip_message_buffer_scroll_top);
        btnMessagelistTopScroll.setOnClickListener(v -> {
            messagelistTopScroll = true;
            btnMessagelistTopScroll.hide();
            messagelistView.scrollToPosition(0);
        });
        btnMessagelistTopScroll.hide();

        messagelistView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int state = messagelistView.getScrollState();
                if (state == RecyclerView.SCROLL_STATE_DRAGGING || state == RecyclerView.SCROLL_STATE_SETTLING) {
                    boolean newIsTopLog = messagelistView.computeVerticalScrollOffset() == 0;
                    if (newIsTopLog != messagelistTopScroll) {
                        messagelistTopScroll = newIsTopLog;
                        if (messagelistTopScroll) btnMessagelistTopScroll.hide();
                        else btnMessagelistTopScroll.show();
                    }
                }
            }
        });

        countsView = view.findViewById(R.id.gossip_counts);
        updateCounts();

        NetLayer.getDefaultInstance().getGossipModule().forEachMessageInMessageBuffer((digest, message) -> messagelist.add(new Tuple2<>(digest, message)));
        NetLayer.getDefaultInstance().getGossipModule().forEachDigestInBlacklist(blacklist::add);

        Context.getContext(GossipModule.CONTEXT_GOSSIP_MODULE).registerReceiver(gossipReceiver,
                new IntentFilter().addAction(GossipModule.ACTION_BLACKLIST_CHANGED)
                        .addAction(GossipModule.ACTION_BUFFER_CHANGED));
        return view;
    }

    private void updateCounts() {
        countsView.setText(String.format(Locale.US, "MB:%d / BL:%d", NetLayer.getDefaultInstance().getGossipModule().getMessageBufferSize(), NetLayer.getDefaultInstance().getGossipModule().getBlackListSize()));
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

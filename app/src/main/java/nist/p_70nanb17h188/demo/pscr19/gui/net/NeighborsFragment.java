package nist.p_70nanb17h188.demo.pscr19.gui.net;


import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl;

/**
 * A simple {@link Fragment} subclass.
 */
public class NeighborsFragment extends Fragment {
    private static final String TAG = "NeighborsFragment";

    private NeighborsFragmentViewModel viewModel;

    public NeighborsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        assert activity != null;
        viewModel = ViewModelProviders.of(getActivity()).get(NeighborsFragmentViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if ("M1".equals(Device.getName())) {
            String message;
            byte[] messageBytes;

            message = "M1abcdefgh446ew4d6e4a6jfkdsjmclksdmlkcmwdlkmv  oierj oigj54oh4u h g54joi tjeroijhoirtoijo o4wjroi hjoi jhorij oirejhoihoi hoih oirhtoirehtoerhoi";
            messageBytes = message.getBytes();
            Log.d(TAG, "message 1 created by %s --- in bytes: %s", Device.getName(), Helper.getHexString(messageBytes));
            NetLayer.getDefaultInstance().getGossipModule().addMessage(messageBytes);


            message = "M1i";
            messageBytes = message.getBytes();
            Log.d(TAG, "message 1 created by %s --- in bytes: %s", Device.getName(), Helper.getHexString(messageBytes));
            NetLayer.getDefaultInstance().getGossipModule().addMessage(messageBytes);

            NetLayer.getDefaultInstance().getGossipModule().printBuffer();
        }
        View view = inflater.inflate(R.layout.fragment_naming_neighbors, container, false);

        RecyclerView list = view.findViewById(R.id.name_routing_list);
        list.setLayoutManager(new WrapLinearLayoutManager(view.getContext()));
        RecyclerView.Adapter<NeighborViewHolder> listAdapter = new RecyclerView.Adapter<NeighborViewHolder>() {
            @NonNull
            @Override
            public NeighborViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_neighbor, parent, false);
                return new NeighborViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull NeighborViewHolder neighborViewHolder, int i) {
                NeighborID[] neighborIDs = viewModel.connectedNeighbors.getValue();
                assert neighborIDs != null;
                neighborViewHolder.bind(neighborIDs[i]);
            }

            @Override
            public int getItemCount() {
                NeighborID[] neighborIDs = viewModel.connectedNeighbors.getValue();
                assert neighborIDs != null;
                return neighborIDs.length;
            }
        };
        list.setAdapter(listAdapter);


        Observer<NeighborID[]> observer = neighborIDs -> listAdapter.notifyDataSetChanged();
        viewModel.connectedNeighbors.observe(this, observer);
        observer.onChanged(viewModel.connectedNeighbors.getValue());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.connectedNeighbors.removeObservers(this);
    }

    private class NeighborViewHolder extends RecyclerView.ViewHolder {
        @NonNull
        private final TextView txNeighbor;
        private final EditText inputSize;
        private final Button btnSend;
        private NeighborID currentNeighborID;

        NeighborViewHolder(@NonNull View itemView) {
            super(itemView);
            txNeighbor = itemView.findViewById(R.id.name_neighbor_id);
            inputSize = itemView.findViewById(R.id.name_input_size);
            inputSize.setHint("[0," + NetLayer_Impl.MAX_SEND_SIZE + "]");
            btnSend = itemView.findViewById(R.id.name_send);
            btnSend.setOnClickListener(this::onSendClick);
        }

        private void onSendClick(View view) {
//            String txt = inputSize.getText().toString();
//            try {
//                int size = Integer.parseInt(txt);
//                if (size < 0) {
//                    Helper.notifyUser(LogType.Info, "Size should be in range [0, %d]", NetLayer_Impl.MAX_SEND_SIZE);
//                    inputSize.setText("0");
//                    return;
//                }
//                if (size > NetLayer_Impl.MAX_SEND_SIZE) {
//                    Helper.notifyUser(LogType.Info, "Max size: ");
//                    Helper.notifyUser(LogType.Info, "Size should be in range [0, %d]", NetLayer_Impl.MAX_SEND_SIZE);
//                    inputSize.setText(NetLayer_Impl.MAX_SEND_SIZE + "");
//                    return;
//                }
//                String str = size == 0 ? "" : Helper.getRandomString(size, size, Helper.CANDIDATE_CHARSET_LETTERS_NUMBERS);
//                byte[] buf = str.getBytes();
//                boolean succeed = LinkLayer.sendData(currentNeighborID, buf, 0, buf.length);
//                String result = succeed ? "succeeded" : "failed";
//                if (str.length() <= NetLayer_Impl.MAX_SHOW_SIZE) {
//                    Log.d(NetLayer_Impl.TAG, "Send to %s, buf_len=%d, %s! text=%n%s", currentNeighborID.getName(), buf.length, result, str);
//                    Helper.notifyUser(LogType.Info, "Send to %s, buf_len=%d, %s! text=%n%s", currentNeighborID.getName(), buf.length, result, str);
//                } else {
//                    Log.d(NetLayer_Impl.TAG, "Send to %s, buf_len=%d, %s! text_len=%d", currentNeighborID.getName(), buf.length, result, str.length());
//                    Helper.notifyUser(LogType.Info, "Send to %s, buf_len=%d, %s! text_len=%d", currentNeighborID.getName(), buf.length, result, str.length());
//                }
//            } catch (Exception e) {
//                Helper.notifyUser(LogType.Info, "Size should be a number!");
//            }
        }

        private void bind(NeighborID neighborID) {
            currentNeighborID = neighborID;
            txNeighbor.setText(neighborID.getName());
        }
    }
}

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
import android.widget.Toast;

import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.logic.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.link.LinkLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;

/**
 * A simple {@link Fragment} subclass.
 */
public class NeighborsFragment extends Fragment {
    private NeighborsFragmentViewModel viewModel;

    public NeighborsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        assert activity != null;
        viewModel = ViewModelProviders.of(getActivity()).get(NeighborsFragmentViewModel.class);
        viewModel.setApplication(getActivity().getApplication());
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
            btnSend = itemView.findViewById(R.id.name_send);
            btnSend.setOnClickListener(this::onSendClick);
        }

        private void onSendClick(View view) {
            String txt = inputSize.getText().toString();
            try {
                int size = Integer.parseInt(txt);
                if (size < 0) {
                    Toast.makeText(view.getContext(), "Min size: 0", Toast.LENGTH_SHORT).show();
                    inputSize.setText("0");
                    return;
                }
                if (size > 4000) {
                    Toast.makeText(view.getContext(), "Max size: 4000", Toast.LENGTH_SHORT).show();
                    inputSize.setText("4000");
                    return;
                }
                String str = Helper.getRandomString(size, size, Helper.CANDIDATE_CHARSET_LETTERS_NUMBERS);
                byte[] buf = str.getBytes();
                boolean succeed = LinkLayer.sendData(currentNeighborID, buf, 0, buf.length);
                String result = succeed ? "succeeded" : "failed";
                if (str.length() < 40) {
                    Toast.makeText(view.getContext(), String.format(Locale.US, "Send to %s, text=%s, buf_len=%d, %s!", currentNeighborID.name, str, buf.length, result), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(view.getContext(), String.format(Locale.US, "Send to %s, text_len=%d, buf_len=%d, %s!", currentNeighborID.name, str.length(), buf.length, result), Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Toast.makeText(view.getContext(), "Should type number!", Toast.LENGTH_SHORT).show();
            }
        }

        private void bind(NeighborID neighborID) {
            currentNeighborID = neighborID;
            txNeighbor.setText(neighborID.name);
        }
    }
}

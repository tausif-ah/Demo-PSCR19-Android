package nist.p_70nanb17h188.demo.pscr19.gui.net;


import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.log.LogContentFragment;
import nist.p_70nanb17h188.demo.pscr19.gui.log.LogFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.link.NeighborID;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogItem;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Constants;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Digest;
import nist.p_70nanb17h188.demo.pscr19.logic.net.GossipModule;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

/**
 * A simple {@link Fragment} subclass.
 */
public class RoutingFragment extends Fragment {
    private static final String TAG = "RoutingFragment";
    private static final int MAGIC = 0x81CB8D7B;
    private static final String DEFAULT_INITIATOR = "nist.p_70nanb17h188.demo.pscr19.gui.net.RoutingFragment";

    public static class RoutingFragmentViewModel extends ViewModel {
        final MutableLiveData<Name> currDst = new MutableLiveData<>();
        final MutableLiveData<Integer> currSize = new MutableLiveData<>();
        final Name[] knownNames;
        final Name myName;

        public RoutingFragmentViewModel() {
            knownNames = Constants.knownNames();
            Name myName = null;
            for (Name n : knownNames) {
                NeighborID neighborID = Constants.getNameDestination(n);
                if (neighborID == null) continue;
                if (neighborID.getName().equals(Device.getName())) {
                    myName = n;
                    break;
                }
            }
            this.myName = myName;

            currDst.setValue(knownNames[0]);
            currSize.setValue(5000);
            if (myName != null)
                NetLayer.subscribe(myName, this::onDataReceived, DEFAULT_INITIATOR);
        }

        void onDataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, @NonNull String initiator) {
            if (DEFAULT_INITIATOR.equals(initiator)) return;
            Log.d(TAG, "Received src=%s, dst=%s, len=%d", src, dst, data.length);
            ByteBuffer buffer = ByteBuffer.wrap(data);
            if (buffer.remaining() < Helper.INTEGER_SIZE * 2) {
                Log.e(TAG, "Error message. remaining (%d) < integer size (%d) * 2", buffer.remaining(), Helper.INTEGER_SIZE);
                return;
            }
            int magic = buffer.getInt();
            if (magic != MAGIC) {
                Log.e(TAG, "Error message. magic (0x%08x) != MAGIC (0x%08x)", magic, MAGIC);
                return;
            }
            int size = buffer.getInt();
            if (size != buffer.remaining() - Digest.DIGEST_SIZE) {
                Log.e(TAG, "Error message. size=%d, remaining=%d (should be %d)", size, buffer.remaining(), size + Digest.DIGEST_SIZE);
                return;
            }
            byte[] digest;
            try {
                // generate a new object as it is not thread-safe.
                MessageDigest tmpDigest = MessageDigest.getInstance(Digest.DEFAULT_DIGEST_ALGORITHM);
                tmpDigest.update(data, 0, data.length - Digest.DIGEST_SIZE);
                digest = tmpDigest.digest();
            } catch (NoSuchAlgorithmException e) {
                // should never reach here
                e.printStackTrace(System.err);
                throw new AssertionError(Digest.DEFAULT_DIGEST_ALGORITHM);
            }
            for (int i = 1; i <= Digest.DIGEST_SIZE; i++) {
                if (digest[digest.length - i] != data[data.length - i]) {
                    Log.e(TAG, "Error message. digest do not match!");
                    return;
                }
            }
            Log.d(TAG, "Validation successful!");
        }

        boolean sendData() {
            assert currSize.getValue() != null;
            assert currDst.getValue() != null;
            int size = Helper.INTEGER_SIZE * 2 + currSize.getValue() + Digest.DIGEST_SIZE;
            byte[] buf = new byte[size];
            ByteBuffer buffer = ByteBuffer.wrap(buf);
            Helper.DEFAULT_RANDOM.nextBytes(buf);
            buffer.putInt(MAGIC);
            buffer.putInt(currSize.getValue());
            byte[] digest;
            try {
                // generate a new object as it is not thread-safe.
                MessageDigest tmpDigest = MessageDigest.getInstance(Digest.DEFAULT_DIGEST_ALGORITHM);
                tmpDigest.update(buf, 0, buf.length - Digest.DIGEST_SIZE);
                digest = tmpDigest.digest();
            } catch (NoSuchAlgorithmException e) {
                // should never reach here
                e.printStackTrace(System.err);
                throw new AssertionError(Digest.DEFAULT_DIGEST_ALGORITHM);
            }
            buffer.position(size - Digest.DIGEST_SIZE);
            buffer.put(digest);

            NetLayer.sendData(myName == null ? new Name(0) : myName, currDst.getValue(), buf, false, DEFAULT_INITIATOR);
            return true;
        }
    }

    public RoutingFragment() {
        // Required empty public constructor
    }

    private RoutingFragmentViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routing, container, false);
        FragmentActivity activity = getActivity();
        assert activity != null;
        viewModel = ViewModelProviders.of(activity).get(RoutingFragmentViewModel.class);
        TextView txtMyName = view.findViewById(R.id.routing_txt_my_name);
        txtMyName.setText(viewModel.myName == null ? "<<---------------->>" : viewModel.myName.toString());

        Spinner spinnerDst = view.findViewById(R.id.routing_dst_name);
        ArrayAdapter<Name> nameListAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_spinner_dropdown_item, viewModel.knownNames);
        spinnerDst.setAdapter(nameListAdapter);
        spinnerDst.setSelection(nameListAdapter.getPosition(viewModel.currDst.getValue()));

        spinnerDst.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.currDst.postValue(nameListAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        EditText edtSize = view.findViewById(R.id.routing_input_size);
        edtSize.setText(String.format("%s", viewModel.currSize.getValue()));
        edtSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    viewModel.currSize.postValue(Integer.parseInt(s.toString()));
                } catch (Exception e) {
                    edtSize.setText("0");
                    viewModel.currSize.postValue(0);
                    edtSize.selectAll();
                }
            }
        });
        edtSize.setOnEditorActionListener((v, actionId, event) -> viewModel.sendData());

        ImageButton btnSend = view.findViewById(R.id.routing_btn_send);
        btnSend.setOnClickListener(v -> viewModel.sendData());

        LogContentFragment contentFragment = (LogContentFragment) getChildFragmentManager().findFragmentById(R.id.routing_log);
        assert contentFragment != null;
        contentFragment.setFilter(new LogFilter() {
            @Override
            public boolean matches(@NonNull LogItem log) {
                return log.getTag().equals("RoutingModule")
                        || log.getTag().equals(TAG)
                        || (log.getTag().equals("SocketWrapper") && log.getMessage().contains("finished reading bytes length="));
            }
        });

        return view;
    }


}

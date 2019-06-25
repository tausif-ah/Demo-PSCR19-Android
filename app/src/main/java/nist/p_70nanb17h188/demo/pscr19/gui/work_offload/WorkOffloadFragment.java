package nist.p_70nanb17h188.demo.pscr19.gui.work_offload;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

/**
 * A simple {@link Fragment} subclass.
 */
public class WorkOffloadFragment extends Fragment {
    private static final String EMPTY_DURATION_TEXT = "---";
    private static final String EMPTY_NAME_TEXT = "<<---------------->>";


    public WorkOffloadFragment() {
        // Required empty public constructor
    }

    private WorkOffloadMaster masterViewModel;
    private WorkOffloadSlave slaveViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Constants.getName() == null)
            return inflater.inflate(R.layout.fragment_work_offload_nothing, container, false);
        if (Constants.isWorkOffloadMaster()) {
            return createMasterView(inflater, container);
        }
        return createSlaveView(inflater, container);
    }

    private class SlaveViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtView;
        private WorkOffloadMaster.Slave slave;
        private Observer<WorkOffloadMaster.SlaveState> slaveStateObserver = this::setState;

        SlaveViewHolder(@NonNull View itemView) {
            super(itemView);
            txtView = (TextView) itemView;
            txtView.setTypeface(Typeface.MONOSPACE);
        }

        private void setState(WorkOffloadMaster.SlaveState slaveState) {
            assert slaveState != null;
            txtView.setCompoundDrawablesWithIntrinsicBounds(0, 0, slaveState.getImgResource(), 0);
        }

        void bind(WorkOffloadMaster.Slave slave) {
            if (this.slave != null) {
                this.slave.slaveState.removeObserver(slaveStateObserver);
            }
            this.slave = slave;
            slave.slaveState.observe(WorkOffloadFragment.this, slaveStateObserver);
            txtView.setText(slave.getSlaveName().toString());
        }

        void detached() {
            if (this.slave != null)
                this.slave.slaveState.removeObserver(slaveStateObserver);
            this.slave = null;
        }
    }

    class NameViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView item;
        public NameViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            item = itemView.findViewById(android.R.id.text1);
        }
        @Override
        public void onClick(View view) {
            masterViewModel.setTargetName(item.getText());
        }
    }

    class ItemArrayAdapter extends RecyclerView.Adapter<NameViewHolder> {

        //All methods in this adapter are required for a bare minimum recyclerview adapter
        private int listItemLayout;
        private List<String> itemList;
        // Constructor of the class
        public ItemArrayAdapter(int layoutId, List<String> itemList) {
            listItemLayout = layoutId;
            this.itemList = itemList;
        }
        // get the size of the list
        @Override
        public int getItemCount() {
            return itemList == null ? 0 : itemList.size();
        }
        // specify the row layout file and click for each row
        @Override
        public NameViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(listItemLayout, parent, false);
            NameViewHolder myViewHolder = new NameViewHolder(view);
            return myViewHolder;
        }

        // load data in each row element
        @Override
        public void onBindViewHolder(final NameViewHolder holder, final int listPosition) {
            TextView item = holder.item;
            item.setText(itemList.get(listPosition));
        }

    }

    private View createMasterView(@NonNull LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.fragment_work_offload_master, container, false);
        FragmentActivity activity = getActivity();
        assert activity != null;
        masterViewModel = ViewModelProviders.of(activity).get(WorkOffloadMaster.class);
        TextView txtState = view.findViewById(R.id.work_offload_master_state);
        TextView txtTaskId = view.findViewById(R.id.work_offload_master_task_id);
        TextView txtDuration = view.findViewById(R.id.work_offload_master_duration);
        TextView txtShowNoSlave = view.findViewById(R.id.work_offload_master_no_slave_text);
        Button btnStart = view.findViewById(R.id.work_offload_master_btn_start);
        Button btnOffload = view.findViewById(R.id.work_offload_master_btn_offload);
        Button btnApp = view.findViewById(R.id.application_type);
        RecyclerView list = view.findViewById(R.id.work_offload_master_list);
        RecyclerView nameList = view.findViewById(R.id.name_list);
        List<String> names = new ArrayList<>();
        names.add("Adam");
        names.add("Jack");
        names.add("Mary");
        names.add("Jane");
        ItemArrayAdapter itemArrayAdapter = new ItemArrayAdapter(android.R.layout.simple_list_item_1, names);
        list.setLayoutManager(new WrapLinearLayoutManager(view.getContext()));
        nameList.setLayoutManager(new LinearLayoutManager(view.getContext()));
        nameList.setItemAnimator(new DefaultItemAnimator());
        nameList.setAdapter(itemArrayAdapter);
        RecyclerView.Adapter<SlaveViewHolder> adapter = new RecyclerView.Adapter<SlaveViewHolder>() {
            @NonNull
            @Override
            public SlaveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new SlaveViewHolder(v);
            }

            @Override
            public void onBindViewHolder(@NonNull SlaveViewHolder viewHolder, int i) {
                viewHolder.bind(masterViewModel.slaves.get(i));
            }

            @Override
            public int getItemCount() {
                return masterViewModel.slaves.size();
            }

            @Override
            public void onViewRecycled(@NonNull SlaveViewHolder holder) {
                super.onViewRecycled(holder);
                holder.detached();
            }
        };
        list.setAdapter(adapter);

        masterViewModel.currState.observe(this, state -> {
            assert state != null;
            txtState.setText(state.getTextResource());
            if (state == WorkOffloadMaster.MasterState.IDLE) {
                btnStart.setText(R.string.work_offload_master_start_start);
                btnOffload.setEnabled(true);
            } else {
                btnStart.setText(R.string.work_offload_master_start_stop);
                btnOffload.setEnabled(false);
            }
        });
        masterViewModel.offload.observe(this, offload -> {
            assert offload != null;
            btnOffload.setText(offload ? R.string.work_offload_master_offload_offload : R.string.work_offload_master_offload_local);
        });

        masterViewModel.face.observe(this, face ->{
            assert face != null;
            btnApp.setText(face ? R.string.work_offload_application_facial : R.string.work_offload_application_matrix);
        });

        masterViewModel.currentTaskId.observe(this, taskId -> {
            assert taskId != null;
            txtTaskId.setText(String.format(Locale.US, "0x%08x", taskId));
        });
        masterViewModel.setSlaveChangedHandler(m -> adapter.notifyDataSetChanged());
        adapter.notifyDataSetChanged();
        Observer<Long> timeObserver = l -> {
            Long start = masterViewModel.taskStart.getValue(), end = masterViewModel.taskEnd.getValue();
            if (start == null || end == null) {
                txtDuration.setText(EMPTY_DURATION_TEXT);
            } else {
                txtDuration.setText(String.format(Locale.US, "%,d", end - start));
            }
        };
        masterViewModel.taskStart.observe(this, timeObserver);
        masterViewModel.taskEnd.observe(this, timeObserver);
        timeObserver.onChanged(null);
        masterViewModel.showNoSlaveText.observe(this, show -> {
            assert show != null;
            txtShowNoSlave.setVisibility(show ? View.VISIBLE : View.GONE);
        });
        btnOffload.setOnClickListener(v -> masterViewModel.flipOffload());
        btnStart.setOnClickListener(v -> masterViewModel.flipState());
        btnApp.setOnClickListener(v -> masterViewModel.flipApp());
        return view;
    }

    private void destroyMasterView() {
        masterViewModel.currState.removeObservers(this);
        masterViewModel.offload.removeObservers(this);
        masterViewModel.currentTaskId.removeObservers(this);
        masterViewModel.taskStart.removeObservers(this);
        masterViewModel.taskEnd.removeObservers(this);
        masterViewModel.showNoSlaveText.removeObservers(this);
        masterViewModel.setSlaveChangedHandler(null);
    }

    private View createSlaveView(@NonNull LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.fragment_work_offload_slave, container, false);
        FragmentActivity activity = getActivity();
        assert activity != null;
        slaveViewModel = ViewModelProviders.of(activity).get(WorkOffloadSlave.class);
        TextView txtMyName = view.findViewById(R.id.work_offload_slave_my_name);
        assert slaveViewModel.myName != null;
        txtMyName.setText(slaveViewModel.myName.toString());
        TextView txtState = view.findViewById(R.id.work_offload_slave_state);
        TextView txtTaskId = view.findViewById(R.id.work_offload_slave_task_id);
        TextView txtMasterName = view.findViewById(R.id.work_offload_slave_master_name);
        TextView txtDuration = view.findViewById(R.id.work_offload_slave_duration);
        Button btnEnabled = view.findViewById(R.id.work_offload_slave_enable);

        slaveViewModel.currState.observe(this, s -> {
            assert s != null;
            txtState.setText(s.getTextResource());
        });
        slaveViewModel.currWorkId.observe(this, id -> {
            if (id == null) id = 0;
            txtTaskId.setText(String.format(Locale.US, "0x%08x", id));
        });
        Observer<Name> masterNameObserver = name -> {
            if (name == null) txtMasterName.setText(EMPTY_NAME_TEXT);
            else txtMasterName.setText(name.toString());
        };
        slaveViewModel.currMasterName.observe(this, masterNameObserver);
        masterNameObserver.onChanged(slaveViewModel.currMasterName.getValue());
        Observer<Long> timeObserver = l -> {
            Long start = slaveViewModel.taskStart.getValue(), end = slaveViewModel.taskEnd.getValue();
            if (start == null || end == null) {
                txtDuration.setText(EMPTY_DURATION_TEXT);
            } else {
                txtDuration.setText(String.format(Locale.US, "%,d", end - start));
            }
        };
        slaveViewModel.taskStart.observe(this, timeObserver);
        slaveViewModel.taskEnd.observe(this, timeObserver);
        timeObserver.onChanged(null);
        slaveViewModel.enabled.observe(this, enabled -> {
            assert enabled != null;
            btnEnabled.setText(enabled ? R.string.work_offload_slave_enabled : R.string.work_offload_slave_disabled);
        });
        btnEnabled.setOnClickListener(v -> slaveViewModel.flipEnabled());
        return view;
    }

    private void destroySlaveView() {
        slaveViewModel.currState.removeObservers(this);
        slaveViewModel.currWorkId.removeObservers(this);
        slaveViewModel.currMasterName.removeObservers(this);
        slaveViewModel.taskStart.removeObservers(this);
        slaveViewModel.taskEnd.removeObservers(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (Constants.getName() != null) {
            if (Constants.isWorkOffloadMaster()) {
                destroyMasterView();
            } else {
                destroySlaveView();
            }
        }
    }
}

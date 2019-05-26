package nist.p_70nanb17h188.demo.pscr19.gui.work_offload;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.util.Consumer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;
import nist.p_70nanb17h188.demo.pscr19.logic.net.DataReceivedHandler;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

public class WorkOffloadMaster extends ViewModel {
    private static final String TAG = "WorkOffloadMaster";
    private static final long WAIT_RESPONSE_DELAY_MS = 1000;
    private static final long PERFORM_TASK_DURATION = 5000;

    enum MasterState {
        IDLE(R.string.work_offload_master_state_idle),
        WAIT_FOR_RESPONSE(R.string.work_offload_master_state_wait_for_response),
        DISTRIBUTE_WORK(R.string.work_offload_master_state_distribute_work),
        WAIT_FOR_RESULT(R.string.work_offload_master_state_wait_for_result),
        COMPUTE_RESULT(R.string.work_offload_master_state_compute_result);

        private final int textResource;

        MasterState(int textResource) {
            this.textResource = textResource;
        }

        public int getTextResource() {
            return textResource;
        }
    }

    enum SlaveState {
        AVAILABLE(R.drawable.ic_circle_yellow),
        WORKING(R.drawable.ic_circle_blue),
        FINISHED(R.drawable.ic_circle_green);
        private final int imgResource;

        SlaveState(int imgResource) {
            this.imgResource = imgResource;
        }

        int getImgResource() {
            return imgResource;
        }
    }

    static class Slave {
        @NonNull
        private final Name slaveName;
        @NonNull
        private final Name masterName;
        @NonNull
        final MutableLiveData<SlaveState> slaveState = new MutableLiveData<>();
        private DataWorkContent workContent;
        private DataWorkResult workResult;

        Slave(@NonNull Name slaveName, @NonNull Name masterName) {
            this.slaveName = slaveName;
            this.masterName = masterName;
            slaveState.postValue(SlaveState.AVAILABLE);
        }

        synchronized void setSlaveTask(DataWorkContent content) {
            if (workContent != null || workResult != null) return;
            workContent = content;
            NetLayer.sendData(masterName, slaveName, content.toBytes());
            slaveState.postValue(SlaveState.WORKING);
        }

        synchronized void setSlaveResult(DataWorkResult result) {
            if (workContent == null || workResult != null) return;
            workResult = result;
            slaveState.postValue(SlaveState.FINISHED);
        }

        DataWorkContent getWorkContent() {
            return workContent;
        }

        DataWorkResult getWorkResult() {
            return workResult;
        }

        @NonNull
        Name getSlaveName() {
            return slaveName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Slave slave = (Slave) o;
            return slaveName.equals(slave.slaveName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(slaveName);
        }
    }


    final MutableLiveData<MasterState> currState = new MutableLiveData<>();
    final MutableLiveData<Integer> currentTaskId = new MutableLiveData<>();
    final MutableLiveData<Boolean> offload = new MutableLiveData<>();
    final MutableLiveData<Long> taskStart = new MutableLiveData<>();
    final MutableLiveData<Long> taskEnd = new MutableLiveData<>();
    final MutableLiveData<Boolean> showNoSlaveText = new MutableLiveData<>();
    final ArrayList<Slave> slaves = new ArrayList<>();

    private final DataReceivedHandler dataReceivedHandler = this::onDataReceived;
    private final Name myName;
    private Handler workerHandler;
    private Consumer<WorkOffloadMaster> slaveChangedHandler = null;
    private final Thread workerThread;

    public WorkOffloadMaster() {
        currState.setValue(MasterState.IDLE);
        currentTaskId.setValue(0);
        offload.setValue(true);
        showNoSlaveText.setValue(false);
        myName = Constants.getName();
        boolean succeed = NetLayer.subscribe(myName, dataReceivedHandler);
        Log.d(TAG, "Subscribe name: %s, succeed=%b", myName, succeed);
        workerThread = new Thread(this::workerThread);
        workerThread.setDaemon(true);
        workerThread.start();
        while (workerHandler == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void setSlaveChangedHandler(Consumer<WorkOffloadMaster> slaveChangedHandler) {
        this.slaveChangedHandler = slaveChangedHandler;
    }

    private void onDataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data) {
        if (!dst.equals(myName)) return;
        MasterState state = currState.getValue();
        assert state != null;
        Integer currentTaskId = this.currentTaskId.getValue();
        assert currentTaskId != null;
        switch (state) {
            case WAIT_FOR_RESPONSE: {
                // make sure that it is a correct response
                DataWorkResponse response = DataWorkResponse.fromBytes(data);
                if (response == null) return;
                // ignore responses that are not working for my current task
                if (response.getWorkId() != currentTaskId) return;
                addSlave(src);
                break;
            }
            case DISTRIBUTE_WORK:
            case WAIT_FOR_RESULT: {
                DataWorkResult result = DataWorkResult.fromBytes(data);
                if (result == null) return;
                if (result.getWorkId() != currentTaskId) return;

                boolean allSlaveResultGot = true;
                for (Slave s : slaves) {
                    if (s.getSlaveName().equals(src))
                        s.setSlaveResult(result);
                    else if (s.getWorkResult() == null)
                        allSlaveResultGot = false;
                }
                if (allSlaveResultGot) {
                    currState.postValue(MasterState.COMPUTE_RESULT);
                    workerHandler.post(() -> completeTask(currentTaskId));
                }

                break;
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        boolean succeed = NetLayer.unSubscribe(myName, dataReceivedHandler);
        Log.d(TAG, "Unsubscribe from name %s, succeed=%b", myName, succeed);
    }

    synchronized void flipState() {
        if (currState.getValue() == MasterState.IDLE) {
            Boolean offload = this.offload.getValue();
            assert offload != null;
            int workId = Helper.DEFAULT_RANDOM.nextInt();
            currentTaskId.postValue(workId);
            clearSlaves();
            taskEnd.postValue(null);
            showNoSlaveText.setValue(false);
            if (offload) {
                currState.setValue(MasterState.WAIT_FOR_RESPONSE);
                NetLayer.sendData(myName, Constants.getMulticastName(), new DataWorkRequest(workId).toBytes());
                workerHandler.postDelayed(() -> onWaitSlaveTimeout(workId), WAIT_RESPONSE_DELAY_MS);
            } else {
                taskStart.postValue(System.currentTimeMillis());
                currState.setValue(MasterState.COMPUTE_RESULT);
                workerHandler.post(() -> performTaskLocally(workId));
            }
        } else {
            Boolean offload = this.offload.getValue();
            assert offload != null;
            if (offload) {
                currState.postValue(MasterState.IDLE);
                clearSlaves();
            } else {
                if (currState.getValue() == MasterState.COMPUTE_RESULT) {
                    currState.postValue(MasterState.IDLE);
                    workerThread.interrupt();
                }
            }
        }
    }

    private void clearSlaves() {
        slaves.clear();
        if (slaveChangedHandler != null) {
            slaveChangedHandler.accept(this);
        }
    }

    private void addSlave(Name name) {
        Slave toAdd = new Slave(name, myName);
        if (slaves.contains(toAdd)) return;
        slaves.add(toAdd);
        if (slaveChangedHandler != null) {
            slaveChangedHandler.accept(this);
        }
    }

    synchronized void flipOffload() {
        if (currState.getValue() != MasterState.IDLE) return;
        Boolean origValue = offload.getValue();
        assert origValue != null;
        offload.setValue(!origValue);
    }

    private void workerThread() {
        Looper.prepare();

        workerHandler = new android.os.Handler();
        Looper.loop();
    }

    private synchronized void onWaitSlaveTimeout(int taskId) {
        Integer currentTaskId = this.currentTaskId.getValue();
        assert currentTaskId != null;
        if (currentTaskId != taskId) return;
        if (currState.getValue() != MasterState.WAIT_FOR_RESPONSE) return;
        if (slaves.size() == 0) {
            // give up task, no slaves available.
            showNoSlaveText.postValue(true);
            currState.postValue(MasterState.IDLE);
        } else {
            taskStart.postValue(System.currentTimeMillis());
            currState.postValue(MasterState.DISTRIBUTE_WORK);
            workerHandler.post(() -> distributeTask(taskId));
        }
    }

    private void performTaskLocally(int taskId) {
        try {
            // TODO: replace it with real workload
            Thread.sleep(PERFORM_TASK_DURATION);

            synchronized (this) {
                Integer currentTaskId = this.currentTaskId.getValue();
                assert currentTaskId != null;
                if (currState.getValue() == MasterState.COMPUTE_RESULT && taskId == currentTaskId) {
                    currState.postValue(MasterState.IDLE);
                    taskEnd.postValue(System.currentTimeMillis());
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void distributeTask(int taskId) {
        // TODO: divide the task into multiple sub-tasks and give them to slaves
        for (Slave s : slaves) {
            int wait = Helper.DEFAULT_RANDOM.nextInt((int) PERFORM_TASK_DURATION / 2) + 1000;
            byte[] data = new byte[wait];
            DataWorkContent content = new DataWorkContent(taskId, data);
            s.setSlaveTask(content);
        }
        currState.postValue(MasterState.WAIT_FOR_RESULT);

    }

    private void completeTask(int taskId) {
        Integer currentTaskId = this.currentTaskId.getValue();
        assert currentTaskId != null;
        if (currentTaskId != taskId) return;
        // TODO: compute results based on the slave return value
        for (Slave s : slaves) {
            int sentLength = s.getWorkContent().getData().length;
            byte[] result = s.getWorkResult().getData();
            if (result.length != Helper.INTEGER_SIZE) {
                Helper.notifyUser(LogType.Error, "The result from slave %s is not correct!", s.getSlaveName());
                continue;
            }
            ByteBuffer buffer = ByteBuffer.wrap(result);
            if (buffer.getInt() != sentLength)
                Helper.notifyUser(LogType.Error, "The result from slave %s is not correct!", s.getSlaveName());
        }
        taskEnd.postValue(System.currentTimeMillis());
        currState.postValue(MasterState.IDLE);

    }

}

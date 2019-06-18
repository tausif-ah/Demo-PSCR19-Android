package nist.p_70nanb17h188.demo.pscr19.gui.work_offload;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.net.DataReceivedHandler;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

public class WorkOffloadSlave extends ViewModel {
    private static final long WAIT_WORK_DELAY_MS = 4000;

    enum SlaveState {
        IDLE(R.string.work_offload_slave_state_idle),
        WAIT_FOR_WORK(R.string.work_offload_slave_state_wait_for_work),
        WORKING(R.string.work_offload_slave_state_working);

        private final int textResource;

        SlaveState(int textResource) {
            this.textResource = textResource;
        }

        public int getTextResource() {
            return textResource;
        }
    }

    final MutableLiveData<SlaveState> currState = new MutableLiveData<>();
    final MutableLiveData<Integer> currWorkId = new MutableLiveData<>();
    final MutableLiveData<Boolean> enabled = new MutableLiveData<>();
    final MutableLiveData<Name> currMasterName = new MutableLiveData<>();
    final MutableLiveData<Long> taskStart = new MutableLiveData<>();
    final MutableLiveData<Long> taskEnd = new MutableLiveData<>();
    final Name myName = Constants.getName();
    private final Name groupName = Constants.getMulticastName();
    private Handler workerHandler;
    private final Thread workerThread;

    private final DataReceivedHandler multicastDataReceivedHandler = this::onMulticastDataReceived;
    private final DataReceivedHandler unicastDataReceivedHandler = this::onUnicastDataReceived;

    public WorkOffloadSlave() {
        currState.setValue(SlaveState.IDLE);
        currWorkId.setValue(0);
        enabled.setValue(false);
        NetLayer.subscribe(myName, unicastDataReceivedHandler);
        NetLayer.subscribe(groupName, multicastDataReceivedHandler);
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

    synchronized void flipEnabled() {
        Boolean enabled = this.enabled.getValue();
        assert enabled != null;
        this.enabled.postValue(!enabled);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        NetLayer.unSubscribe(myName, unicastDataReceivedHandler);
        NetLayer.unSubscribe(groupName, multicastDataReceivedHandler);
    }

    private synchronized void onMulticastDataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data) {
        if (!dst.equals(groupName)) return;
        SlaveState slaveState = currState.getValue();
        assert slaveState != null;
        if (slaveState != SlaveState.IDLE) return;
        Boolean enabled = this.enabled.getValue();
        assert enabled != null;
        if (!enabled) return;
        DataWorkRequest request = DataWorkRequest.fromBytes(data);
        // cannot parse request
        if (request == null) return;
        int workId = request.getWorkId();
        currWorkId.postValue(workId);
        currMasterName.postValue(src);
        currState.postValue(SlaveState.WAIT_FOR_WORK);
        taskStart.postValue(null);
        taskEnd.postValue(null);
        workerHandler.postDelayed(() -> waitWorkTimeout(workId, src), WAIT_WORK_DELAY_MS);
        NetLayer.sendData(myName, src, new DataWorkResponse(workId).toBytes(), false);
    }

    private synchronized void onUnicastDataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data) {
        if (!dst.equals(myName)) return;
        SlaveState slaveState = currState.getValue();
        assert slaveState != null;
        if (slaveState != SlaveState.WAIT_FOR_WORK) return;
        DataWorkContent content = DataWorkContent.fromBytes(data);
        // cannot parse content
        if (content == null) return;
        int workId = content.getWorkId();
        if (currWorkId.getValue() == null || currWorkId.getValue() != workId) return;
        if (currMasterName.getValue() == null || !currMasterName.getValue().equals(src)) return;
        currState.postValue(SlaveState.WORKING);
        workerHandler.post(() -> performTask(content));
    }

    private synchronized void waitWorkTimeout(int workId, @NonNull Name master) {
        if (master.equals(currMasterName.getValue()) &&
                currWorkId.getValue() != null && currWorkId.getValue() == workId &&
                currState.getValue() == SlaveState.WAIT_FOR_WORK) {
            currState.postValue(SlaveState.IDLE);
            currMasterName.postValue(null);
            currWorkId.postValue(null);
            taskStart.postValue(null);
            taskEnd.postValue(null);
        }
    }

    private void performTask(DataWorkContent content) {
        taskEnd.postValue(null);
        taskStart.postValue(System.currentTimeMillis());
        // TODO: perform the task
        try {
            Thread.sleep(content.getData().length);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        taskEnd.postValue(System.currentTimeMillis());
        // send the result back
        Name currMasterName = this.currMasterName.getValue();
        assert currMasterName != null;
        ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE);
        buffer.putInt(content.getData().length);
        NetLayer.sendData(myName, currMasterName, new DataWorkResult(content.getWorkId(), buffer.array()).toBytes(), false);
        currState.postValue(SlaveState.IDLE);
    }

    private void workerThread() {
        Looper.prepare();

        workerHandler = new android.os.Handler();
        Looper.loop();

    }
}
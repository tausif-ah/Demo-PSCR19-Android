package nist.p_70nanb17h188.demo.pscr19.gui.work_offload;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;

import java.nio.ByteBuffer;

import nist.p_70nanb17h188.demo.pscr19.FaceUtil;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.net.DataReceivedHandler;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imdecode;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class WorkOffloadSlave extends ViewModel {
    private static final long WAIT_WORK_DELAY_MS = 30000;

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
        NetLayer.sendData(myName, src, new DataWorkResponse(workId).toBytes(), true);
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
        ByteBuffer buffer = ByteBuffer.wrap(content.getData());
        if(content.getWorkType()==5){
            synchronized (FaceUtil.faceRecognizer){
                int resultId = 0;
                double resultLevel = 50000;
                int target = buffer.getInt();
                int num = buffer.getInt();
                while(num>0){
                    int seq = buffer.getInt();
                    int size = buffer.getInt();
                    Log.d("Slave is To",seq+" "+size);
                    byte[] bytes = new byte[size];
                    buffer.get(bytes);
                    opencv_core.Mat testImage = imdecode(new opencv_core.Mat(bytes),CV_LOAD_IMAGE_GRAYSCALE);
                    opencv_core.Mat testFace = detectFaces(testImage);
                    int prediction;
                    double acceptanceLevel;
                    if(testFace!=null){
                        IntPointer label = new IntPointer(1);
                        DoublePointer reliability = new DoublePointer(1);
                        FaceUtil.faceRecognizer.predict(testFace, label, reliability);
                        prediction = label.get(0);
                        acceptanceLevel = reliability.get(0);
                    }else{
                        prediction = 0;
                        acceptanceLevel = 0;
                    }
                    if(prediction==target&&acceptanceLevel<resultLevel){
                        resultId = seq;
                        resultLevel = acceptanceLevel;
                    }
                    num--;
                }
                taskEnd.postValue(System.currentTimeMillis());
                // send the result back
                Name currMasterName = this.currMasterName.getValue();
                assert currMasterName != null;
                ByteBuffer buffer1 = ByteBuffer.allocate(2*Helper.INTEGER_SIZE+Helper.DOUBLE_SIZE);
                buffer1.putInt(resultId);
                buffer1.putDouble(resultLevel);
                NetLayer.sendData(myName, currMasterName, new DataWorkResult(content.getWorkId(), buffer1.array()).toBytes(), true);
                currState.postValue(SlaveState.IDLE);
            }
        }
    }

    opencv_core.Mat detectFaces(opencv_core.Mat grey){
        opencv_core.RectVector detectedFaces = new opencv_core.RectVector();
        FaceUtil.faceDetector.detectMultiScale(grey, detectedFaces, 1.1, 1, 0, new opencv_core.Size(150,150), new opencv_core.Size(500,500));
        opencv_core.Rect rectFace = detectedFaces.get(0);
        if(rectFace==null){
            return null;
        }
        opencv_core.Mat capturedFace = new opencv_core.Mat(grey, rectFace);
        resize(capturedFace, capturedFace, new opencv_core.Size(160,160));
        return capturedFace;
    }

    private void workerThread() {
        Looper.prepare();

        workerHandler = new android.os.Handler();
        Looper.loop();

    }
}
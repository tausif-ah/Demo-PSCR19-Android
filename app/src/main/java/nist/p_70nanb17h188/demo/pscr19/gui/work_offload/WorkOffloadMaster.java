package nist.p_70nanb17h188.demo.pscr19.gui.work_offload;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.util.Consumer;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nist.p_70nanb17h188.demo.pscr19.FaceUtil;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.logic.app.EachHelper;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.net.DataReceivedHandler;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer_Impl;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

public class WorkOffloadMaster extends ViewModel {
    private static final String TAG = "WorkOffloadMaster";
    private static final long WAIT_RESPONSE_DELAY_MS = 2000;
    private static final String INITIATOR_WORK_OFFLOAD_MASTER = "nist.p_70nanb17h188.demo.pscr19.gui.work_offload.WorkOffloadMaster";
    private static final String INITIATOR_INIT = NetLayer_Impl.INITIATOR_INIT;

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
            NetLayer.sendData(masterName, slaveName, content.toBytes(), false, INITIATOR_WORK_OFFLOAD_MASTER);
            slaveState.postValue(SlaveState.WORKING);
        }

        synchronized void setSlaveResult(DataWorkResult result) {
            if (workContent == null || workResult != null) return;
            workResult = result;
            slaveState.postValue(SlaveState.FINISHED);
        }

        DataWorkResult getWorkResult() {
            return workResult;
        }

        @NonNull
        Name getSlaveName() {
            return slaveName;
        }

        SlaveState getSlaveState() {
            return slaveState.getValue();
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
    final MutableLiveData<Integer> faceResult = new MutableLiveData<>();
    final MutableLiveData<String> faceResultPath = new MutableLiveData<>();
    final MutableLiveData<Boolean> offload = new MutableLiveData<>();
    final MutableLiveData<Boolean> face = new MutableLiveData<>();
    final MutableLiveData<Long> taskStart = new MutableLiveData<>();
    final MutableLiveData<Long> taskEnd = new MutableLiveData<>();
    final MutableLiveData<Boolean> showNoSlaveText = new MutableLiveData<>();
    final ArrayList<Slave> slaves = new ArrayList<>();

    private final DataReceivedHandler dataReceivedHandler = this::onDataReceived;
    @NonNull
    private final Name myName;
    private Handler workerHandler;
    private Consumer<WorkOffloadMaster> slaveChangedHandler = null;
    private final Thread workerThread;
    private int target = 1;

    private final HashMap<String, Integer> address = new HashMap<>();

    private final ExecutorService pool = Executors.newFixedThreadPool(1);
    private final CompletionService<double[]> ecs = new ExecutorCompletionService<>(pool);

    public WorkOffloadMaster() {
        Name tmpMyName = Constants.getName();
        assert  tmpMyName != null;
        myName = tmpMyName;

        address.put("Adam", 1);
        address.put("Jack", 2);
        address.put("Mary", 5);
        address.put("Jane", 6);
        currState.setValue(MasterState.IDLE);
        currentTaskId.setValue(0);
        offload.setValue(true);
        face.setValue(true);
        showNoSlaveText.setValue(false);
        boolean succeed = NetLayer.subscribe(myName, dataReceivedHandler, INITIATOR_INIT);
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

    void setTargetName(String s) {
        target = address.get(s);
    }

    void setSlaveChangedHandler(Consumer<WorkOffloadMaster> slaveChangedHandler) {
        this.slaveChangedHandler = slaveChangedHandler;
    }

    private void onDataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, @NonNull String initiator) {
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
        boolean succeed = NetLayer.unSubscribe(myName, dataReceivedHandler, INITIATOR_INIT);
        Log.d(TAG, "Unsubscribe from name %s, succeed=%b", myName, succeed);
    }

    synchronized void flipState() {//code actual doing work
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
                NetLayer.sendData(myName, Constants.getMulticastName(), new DataWorkRequest(workId).toBytes(), false, INITIATOR_WORK_OFFLOAD_MASTER);
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

    synchronized void flipApp() {
        Boolean origValue = face.getValue();
        assert origValue != null;
        face.setValue(!origValue);
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
        Boolean face = this.face.getValue();
        assert face != null;
        if (face) {
            synchronized (FaceUtil.faceRecognizer) {
                String trainDir = Environment.getExternalStorageDirectory().getPath() + "/faces/test";
                File root = new File(trainDir);
                FilenameFilter imgFilter = (dir, name) -> {
                    name = name.toLowerCase();
                    return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
                };
                File[] imageFiles = root.listFiles(imgFilter);
                double maxAcceptLevel = 20000;
                String maxPath = "";
                for (File image : imageFiles) {
                    opencv_core.Mat testImage = detectFaces(image.getAbsolutePath());
                    if (testImage == null) {
                        continue;
                    }
                    IntPointer label = new IntPointer(1);
                    DoublePointer reliability = new DoublePointer(1);
                    FaceUtil.faceRecognizer.predict(testImage, label, reliability);
                    int prediction = label.get(0);
                    double acceptanceLevel = reliability.get(0);
                    if (prediction == target && acceptanceLevel < maxAcceptLevel) {
                        maxAcceptLevel = acceptanceLevel;
                        maxPath = image.getAbsolutePath();
                    }
                }
                faceResultPath.postValue(maxPath);
            }
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        synchronized (this) {
            Integer currentTaskId = this.currentTaskId.getValue();
            assert currentTaskId != null;
            if (currState.getValue() == MasterState.COMPUTE_RESULT && taskId == currentTaskId) {
                currState.postValue(MasterState.IDLE);
                taskEnd.postValue(System.currentTimeMillis());
            }
        }
    }

    private opencv_core.Mat detectFaces(String filePath) {
        opencv_core.Mat grey = imread(filePath, CV_LOAD_IMAGE_GRAYSCALE);
        opencv_core.RectVector detectedFaces = new opencv_core.RectVector();
        FaceUtil.faceDetector.detectMultiScale(grey, detectedFaces, 1.1, 1, 0, new opencv_core.Size(150, 150), new opencv_core.Size(500, 500));
        opencv_core.Rect rectFace = detectedFaces.get(0);
        if (rectFace == null) {
            return null;
        }
        opencv_core.Mat capturedFace = new opencv_core.Mat(grey, rectFace);
        resize(capturedFace, capturedFace, new opencv_core.Size(160, 160));
        return capturedFace;
    }

    private byte[] getOneImage(File file, int size) {
        try {
            InputStream in = new FileInputStream(file);
            byte[] fileBytes = new byte[size];
            in.read(fileBytes);
            return fileBytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[20];
    }

    private byte[] getGroupImage(int start, int num) {
        int[] sizes = new int[num];
        File[] files = new File[num];
        int totalSize = 2 * Helper.INTEGER_SIZE * num + 2 * Helper.INTEGER_SIZE;
        for (int i = start; i < start + num; i++) {
            String fileName = Environment.getExternalStorageDirectory().getPath() + "/faces/test/img (" + i + ").jpg";
            files[i - start] = new File(fileName);
            sizes[i - start] = (int) files[i - start].length();
            totalSize += sizes[i - start];
        }
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.putInt(target);
        buffer.putInt(num);
        for (int i = start; i < start + num; i++) {
            buffer.putInt(i);
            buffer.putInt(sizes[i - start]);
            buffer.put(getOneImage(files[i - start], sizes[i - start]));
        }
        return buffer.array();
    }

    private void distributeTask(int taskId) {
        Boolean face = this.face.getValue();
        assert face != null;
        if (face) {
            int jobNum = 200 / (slaves.size() + 1);
            int localStart = 200 - 200 / (slaves.size() + 1) * slaves.size() + 1;
            ecs.submit(new EachHelper(FaceUtil.faceDetector, FaceUtil.faceRecognizer, localStart));
            int start = 1;
            for (Slave s : slaves) {
                byte[] data = getGroupImage(start, jobNum);
                byte type = 5;
                DataWorkContent content = new DataWorkContent(taskId, type, data);
                s.setSlaveTask(content);
                start += jobNum;
            }
            currState.postValue(MasterState.WAIT_FOR_RESULT);
        } else {
            //TODO: Matrix Multiplication
        }
    }

    private void completeTask(int taskId) {
        Integer currentTaskId = this.currentTaskId.getValue();
        assert currentTaskId != null;
        if (currentTaskId != taskId) return;
        double maxLevel = 50000;
        int seq = 0;
        Boolean face = this.face.getValue();
        assert face != null;
        if (face) {
            try {
                double[] localResult = ecs.take().get();
                if ((int) localResult[0] == target) {
                    seq = (int) localResult[0];
                    maxLevel = localResult[1];
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (Slave s : slaves) {
            byte[] result = s.getWorkResult().getData();
            ByteBuffer buffer = ByteBuffer.wrap(result);
            if (face) {
                int id = buffer.getInt();
                double acc = buffer.getDouble();
                if (id != 0) {
                    if (maxLevel > acc) {
                        seq = id;
                        maxLevel = acc;
                    }
                }
            } else {

            }
        }
        if (face) {
            faceResult.postValue(seq);
        }
        taskEnd.postValue(System.currentTimeMillis());
        currState.postValue(MasterState.IDLE);
    }

}

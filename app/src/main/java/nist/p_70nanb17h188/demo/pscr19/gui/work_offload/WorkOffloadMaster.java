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
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
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

        void setFinished(){
            slaveState.postValue(SlaveState.FINISHED);
        }

        synchronized boolean isBusy(){
            return workContent != null;
        }

        synchronized void finish(){
            workContent = null;
        }

        synchronized void setSlaveTask(DataWorkContent content) {
            //if (workContent != null || workResult != null) return;
            workContent = content;
            NetLayer.sendData(masterName, slaveName, content.toBytes(), false, INITIATOR_WORK_OFFLOAD_MASTER);
            slaveState.postValue(SlaveState.WORKING);
        }

        synchronized void setSlaveResult(DataWorkResult result) {
            //if (workContent == null || workResult != null) return;
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
    final MutableLiveData<Boolean> isBigMat = new MutableLiveData<>();
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

    private int resultSeq = 0;

    private double resultAcc = 50000;

    private synchronized void setResultAcc(double num){
        resultAcc = num;
    }

    private synchronized double getResultAcc(){
        return resultAcc;
    }

    private synchronized void setResultSeq(int num){
        resultSeq = num;
    }

    private synchronized int getResultSeq(){
        return resultSeq;
    }

    private int numCompleteFace = 0;

    private synchronized int incr(){
        return ++numCompleteFace;
    }

    private void onDataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, @NonNull String initiator) {
        if (!dst.equals(myName)) return;
        MasterState state = currState.getValue();
        assert state != null;
        Integer currentTaskId = this.currentTaskId.getValue();
        assert currentTaskId != null;
        Boolean face = this.face.getValue();
        assert face != null;
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
            case COMPUTE_RESULT:{
                //slaveState.postValue(SlaveState.FINISHED);
                break;
            }
            case DISTRIBUTE_WORK:
            case WAIT_FOR_RESULT: {
                DataWorkResult result = DataWorkResult.fromBytes(data);
                if (result == null) return;
                if (result.getWorkId() != currentTaskId) return;
                if(face){
                    for(Slave s : slaves){
                        if (s.getSlaveName().equals(src)){
                            byte[] resultBytes = result.getData();
                            ByteBuffer buffer = ByteBuffer.wrap(resultBytes);
                            if(buffer.getInt()==target){
                                double acc = buffer.getDouble();
                                if(getResultAcc()>acc){
                                    setResultAcc(acc);
                                    setResultSeq(buffer.getInt());
                                }
                            }
                            s.finish();
                            if(incr()>=200){
                                workerHandler.post(() -> completeTask(currentTaskId));
                            }
                            break;
                        }
                    }
                }else {
                    boolean allSlaveResultGot = false;
                    int numComplete = 0;
                    for (Slave s : slaves) {
                        if (s.getSlaveName().equals(src)){
                            s.setSlaveResult(result);
                            numComplete++;
                        } else if (s.getWorkResult() != null)
                            numComplete++;
                        if(numComplete==slaves.size()-1){
                            allSlaveResultGot = true;
                        }
                    }
                    if (allSlaveResultGot) {
                        currState.postValue(MasterState.COMPUTE_RESULT);
                        workerHandler.post(() -> completeTask(currentTaskId));
                    }
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

    private long[] matrixResult;

    long[] getMatrixResult(){
        return matrixResult;
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
            Boolean isBig = this.isBigMat.getValue();
            assert isBig!= null;
            if(isBig){
                int[] vec = genVector();
                long[] a = matrixMultiplyVector(genMatrix(),vec,true);
                long[] b = matrixMultiplyVector(genMatrix(),vec,true);
                matrixResult = paste(a,b);
            }else{
                matrixResult = matrixMultiplyVector(smallMat,smallVector,false);
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

    private final int bigRow = 600;
    private final int bigCol = 1000;
    private final int smallRow = 3;
    private final int smallCol = 2;
    private final int range = 128;

    int[][] smallMat = {{1,7},{2,8},{3,9},{4,9},{5,8},{6,7}};
    int[][] smallMat1 = {{1,7},{2,8},{3,9}};
    int[][] smallMat2 = {{4,9},{5,8},{6,7}};
    int[] smallVector = {1,2};

    private int[][] genMatrix(){
        int[][] res = new int[bigRow][bigCol];
        Random random = new Random();
        for(int i = 0; i<bigRow; i++){
            for(int j = 0; j<bigCol; j++){
                res[i][j] = random.nextInt(range);
            }
        }
        return res;
    }

    private int[] genVector(){
        int[] res = new int[bigCol];
        Random random = new Random();
        for(int n = 0; n<bigCol; n++){
            res[n] = random.nextInt(range);
        }
        return res;
    }

    private long[] matrixMultiplyVector(int[][] a, int[] b, boolean isBig){
        int x = isBig ? bigRow : smallRow;
        long[] result = new long[x];
        int n = isBig ? bigCol : smallCol;
        for(int k = 0; k<x; k++){
            for(int c = 0; c<n; c++){
                result[k] += a[k][c]*(long)b[c];
            }
        }
        return result;
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

    private byte[] getOneImage(int seq) {
        String fileName = Environment.getExternalStorageDirectory().getPath() + "/faces/test/img (" + seq + ").jpg";
        File file = new File(fileName);
        int size = (int) file.length();
        int totalSize = 2 * Helper.INTEGER_SIZE+size;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.putInt(size);
        buffer.putInt(seq);
        try {
            InputStream in = new FileInputStream(file);
            byte[] fileBytes = new byte[size];
            in.read(fileBytes);
            buffer.put(fileBytes);
            return buffer.array();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[20];
    }

    byte[] dataToBytes(int[][] a, int[] b, int seq, boolean isBig){
        int row = isBig ? bigRow : smallRow;
        int col = isBig ? bigCol : smallCol;
        int totalSize = Helper.INTEGER_SIZE*(row*col)+Helper.INTEGER_SIZE*col+3*Helper.INTEGER_SIZE;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.putInt(seq);
        buffer.putInt(row);
        buffer.putInt(col);
        for(int r = 0; r<row; r++){
            for(int c = 0; c<col; c++){
                buffer.putInt(a[r][c]);
            }
        }
        for(int c = 0; c<col; c++){
            buffer.putInt(b[c]);
        }
        return buffer.array();
    }

    private long[] vectorSub(long[] a, long[] b){
        long[] res = new long[a.length];
        for(int n = 0; n<a.length; n++){
            res[n] = a[n]-b[n];
        }
        return res;
    }

    private int[][] matPlus(int[][] a, int[][] b){
        int[][] res = new int[a.length][a[0].length];
        for(int r = 0; r<a.length; r++){
            for(int c = 0; c<a[0].length; c++){
                res[r][c] = a[r][c]+b[r][c];
            }
        }
        return res;
    }

    private long[] paste(long[] a, long[] b){
        long[] c = new long[a.length+b.length];
        System.arraycopy(a,0,c,0,a.length);
        System.arraycopy(b,0,c, a.length, b.length);
        return c;
    }

    private Queue<Integer> jobQueue = new LinkedList<>();

    private synchronized void enqueue(int i){
        jobQueue.offer(i);
    }

    private synchronized int dequeue(){
        return jobQueue.isEmpty() ? -1 : jobQueue.poll();
    }

    private synchronized boolean isEmpty(){
        return jobQueue.isEmpty();
    }

    private void distributeTask(int taskId) {
        Boolean face = this.face.getValue();
        assert face != null;
        if (face) {
            byte type = 5;
            int jobNum = 200;
            for(int i = 1; i<=jobNum; i++){
                enqueue(i);
            }
//            int initNumber = 10;
//            for(int i = 0; i< initNumber; i++){
//                for(Slave s : slaves){
//                    s.setSlaveTask(new DataWorkContent(taskId, type, getOneImage(dequeue())));
//                }
//            }
            currState.postValue(MasterState.WAIT_FOR_RESULT);
            while(!isEmpty()){
                ecs.submit(new EachHelper(FaceUtil.faceDetector, FaceUtil.faceRecognizer, dequeue()));
                for(Slave s : slaves){
                    if(isEmpty()){
                        break;
                    }
                    if(!s.isBusy()){
                        s.setSlaveTask(new DataWorkContent(taskId, type, getOneImage(dequeue())));
                    }
                }
                try {
                    double[] localResult = ecs.take().get();
                    if((int) localResult[1]==target){
                        if(getResultAcc()>localResult[2]){
                            setResultAcc(localResult[2]);
                            setResultSeq((int) localResult[0]);
                        }
                    }
                    if(incr()>=200){
                        Integer currentTaskId = this.currentTaskId.getValue();
                        assert currentTaskId != null;
                        workerHandler.post(() -> completeTask(currentTaskId));
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Boolean isBig = isBigMat.getValue();
            assert isBig != null;
            byte type = 6;
            Slave s1 = slaves.get(0);
            int[][] firstHalf = isBig ? genMatrix() : smallMat1;
            int[] vec = isBig ? genVector() : smallVector;
            byte[] data1 = dataToBytes(firstHalf, vec,0,isBig);
            DataWorkContent content1 = new DataWorkContent(taskId,type, data1);
            s1.setSlaveTask(content1);
            Slave s2 = slaves.get(1);
            int[][] secondHalf = isBig ? genMatrix() : smallMat2;
            byte[] data2 = dataToBytes(secondHalf, vec,1,isBig);
            DataWorkContent content2 = new DataWorkContent(taskId,type, data2);
            s2.setSlaveTask(content2);
            Slave s3 = slaves.get(2);
            byte[] data3 = dataToBytes(matPlus(firstHalf,secondHalf), vec,2,isBig);
            DataWorkContent content3 = new DataWorkContent(taskId,type, data3);
            s3.setSlaveTask(content3);
            currState.postValue(MasterState.WAIT_FOR_RESULT);
        }
    }

    private void completeTask(int taskId) {
        Integer currentTaskId = this.currentTaskId.getValue();
        assert currentTaskId != null;
        if (currentTaskId != taskId) return;
        Boolean face = this.face.getValue();
        assert face != null;
        if(face){
            for(Slave s : slaves){
                ByteBuffer buffer = ByteBuffer.allocate(Helper.INTEGER_SIZE);
                buffer.putInt(-1);
                byte type = 5;
                s.setSlaveTask(new DataWorkContent(currentTaskId, type, buffer.array()));
                s.setFinished();
            }
            faceResult.postValue(getResultSeq());
        }else{
            Boolean isBig = isBigMat.getValue();
            assert isBig != null;
            int size = isBig ? bigRow : smallRow;
            long[] r1 = null, r2 = null, r3 = null;
            for (Slave s : slaves) {
                DataWorkResult result = s.getWorkResult();
                if(result!=null){
                    ByteBuffer buffer = ByteBuffer.wrap(result.getData());
                    int id = buffer.getInt();
                    long[] temp = new long[size];
                    for(int n = 0; n<size; n++){
                        temp[n] = buffer.getLong();
                    }
                    switch (id) {
                        case 0 :
                            r1 = temp;
                            break;
                        case 1 :
                            r2 = temp;
                            break;
                        case 2 :
                            r3 = temp;
                            break;
                    }
                }
            }
            if(r1==null){
                matrixResult = paste(vectorSub(r3,r2),r2);
            }else if(r2 == null) {
                matrixResult = paste(r1, vectorSub(r3,r1));
            }else {
                matrixResult = paste(r1,r2);
            }
        }
        taskEnd.postValue(System.currentTimeMillis());
        currState.postValue(MasterState.IDLE);
    }

}

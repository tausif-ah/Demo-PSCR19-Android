package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.Constants;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.Message;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingName;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

public class MessagingFragmentViewModel extends ViewModel {
    private static MessagingFragmentViewModel DEFAULT_INSTANCE;
    private static final String INITIATOR = "nist.p_70nanb17h188.demo.pscr19.gui.messaging.MessagingFragmentViewModel";

    public synchronized static void init() {
        if (DEFAULT_INSTANCE == null)
            DEFAULT_INSTANCE = new MessagingFragmentViewModel();
    }

    public static MessagingFragmentViewModel getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    MutableLiveData<Boolean> usingPnt = new MutableLiveData<>();
    MutableLiveData<Boolean> autoPlay = new MutableLiveData<>();
    MutableLiveData<Boolean> recording = new MutableLiveData<>();
    MutableLiveData<MessagingName> selectedName = new MutableLiveData<>();
    MutableLiveData<String> text = new MutableLiveData<>();
    MutableLiveData<String> searchInput = new MutableLiveData<>();
    ArrayList<MessageViewModel> messages = new ArrayList<>();

    // adapter for messages

    private MessagingFragmentViewModel() {
        this.usingPnt.setValue(true);
        this.autoPlay.setValue(true);
        this.recording.setValue(false);
        text.setValue("");
        // subscribe to channel.
        Name n = Constants.getDefaultSubscription();
        if (n != null) NetLayer.subscribe(n, this::dataReceived, INITIATOR);
        // listen to name changes

        messages.add(new MessageViewModel(new Message(System.currentTimeMillis(), 0, new Name(-126), new Name(-101), "test", new Name[0], Message.MessageType.MSG, "", Helper.getRandomString(30, 100, Helper.CANDIDATE_CHARSET_LETTERS_NUMBERS_SPACES).getBytes())));
        messages.add(new MessageViewModel(new Message(System.currentTimeMillis(), 0, new Name(-126), new Name(-200), "test", new Name[]{new Name(-201), new Name(-202), new Name(-203)}, Message.MessageType.MSG, "", Helper.getRandomString(30, 100, Helper.CANDIDATE_CHARSET_LETTERS_NUMBERS_SPACES).getBytes())));
        messages.add(new MessageViewModel(new Message(System.currentTimeMillis(), 2500, new Name(-126), new Name(-200), "test", new Name[]{new Name(-201), new Name(-202), new Name(-203)}, Message.MessageType.PNT, "", Helper.getRandomString(30, 100, Helper.CANDIDATE_CHARSET_LETTERS_NUMBERS_SPACES).getBytes())));

//                mp.setDataSource(MyApplication.getDefaultInstance().getApplicationContext(), Uri.fromFile(f));
//                Log.d("AAAA", "f=%s", f.getAbsolutePath());

//        File d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        ArrayList<String> files = new ArrayList<>();
//        for (File f : d.listFiles(f -> f.getName().endsWith(".aac"))) {
//            MediaPlayer mp = new MediaPlayer();
//            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//            try {
//                retriever.setDataSource(f.getAbsolutePath());
//                Log.d("AAAA", "duration 1: %s", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
//                mp.setDataSource(f.getAbsolutePath());
//                mp.prepare();
//                Log.d("AAAA", "duration: %d", mp.getDuration());
//                mp.release();
//            } catch (IOException | RuntimeException e) {
//                e.printStackTrace();
//            }
//            files.add(f.getAbsolutePath());
//            Log.d("AAAA", "f=%s, delete=%b", f, f.delete());
//        }
    }

    void flipAutoPlay() {
        assert autoPlay.getValue() != null;
        autoPlay.postValue(!autoPlay.getValue());
    }

    void flipPnt() {
        assert usingPnt.getValue() != null;
        usingPnt.postValue(!usingPnt.getValue());
    }

    @NonNull
    MessageViewModel getMessageAtPosition(int position) {
        return messages.get(position);
    }

    int getMessageListSize() {
        return messages.size();
    }

//    private MediaRecorder recorder;
//    private File recordingFile;

    synchronized void setRecording(boolean recording) {
//        this.recording.postValue(recording);
//        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        if (recording) {
//            if (recorder != null) return;
//            try {
//                recorder = new MediaRecorder();
//                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
////                Environment.
//                recordingFile = File.createTempFile("recorded_", ".aac", path);
//                Log.d("AAAA", "file=%s", recordingFile.getAbsolutePath());
//                recorder.setOutputFile(recordingFile.getAbsolutePath());
//                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
//                recorder.prepare();
//                recorder.start();
//            } catch (IOException e) {
//                e.printStackTrace();
//                recorder.release();
//                recorder = null;
//            }
//        } else {
//            if (recorder == null) return;
//            recorder.stop();
//            recorder.release();
//            recorder = null;
//            MediaScannerConnection.scanFile(MyApplication.getDefaultInstance().getApplicationContext(), new String[]{recordingFile.getAbsolutePath()}, null, null);
//            Log.d("AAAA", "file size: %d", recordingFile.length());
//        }
    }


    private void dataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, @NonNull String initiator) {
        // ignore the messages that I sent.
        if (initiator.equals(INITIATOR)) return;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Message msg = Message.read(buffer);
        if (msg != null) {
            Log.d("AAAA", "msg.duration: %d", msg.getDuration());

            MediaPlayer player = new MediaPlayer();
            player.setDataSource(new MemoryMediaDataSource(msg.getContent()));
            try {
                player.prepare();
                player.start();
            } catch (IOException e) {
                Log.e("AAAA", e, "");
            }

        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}

package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

import android.media.MediaPlayer;
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import nist.p_70nanb17h188.demo.pscr19.logic.Consumer;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.Message;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;

class AudioManager {
    private static final String TAG = "AudioManager";
    private final Consumer<MessageViewModel> messagePlayStateChangedHandler;
    private MediaRecorder recorder;
    private File recordingFile;

    private MessageViewModel currentMessage = null;
    private MediaPlayer currentPlayer = null;
    private boolean autoPlay = MessagingFragmentViewModel.DEFAULT_AUTO_PLAY;
    private ArrayList<MessageViewModel> pendingMessages = new ArrayList<>();

    AudioManager(Consumer<MessageViewModel> messagePlayStateChangedHandler) {
        this.messagePlayStateChangedHandler = messagePlayStateChangedHandler;
    }

    synchronized void addMessage(MessageViewModel viewModel) {
        if (viewModel.getMessage().getType() == Message.MessageType.PNT && !viewModel.isPlayed())
            pendingMessages.add(viewModel);
        if (autoPlay && currentMessage == null) playNext();
    }

    synchronized void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
        if (autoPlay) playNext();
    }

    synchronized void clear() {
        stopPlayer();
        pendingMessages.clear();
    }

    synchronized void play(MessageViewModel viewModel) {
        stopPlayer();
        pendingMessages.remove(viewModel);
        startPlay(viewModel);
    }

    synchronized void startRecording() {
        if (recorder != null) return;
        if (currentPlayer != null) {
            Log.d(TAG, "pausing current player");
            currentPlayer.pause();
        }
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            recordingFile = File.createTempFile("recorded_", ".aac");
            recorder.setOutputFile(recordingFile.getAbsolutePath());
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in starting a record.");
            recorder.release();
            recorder = null;
        }
    }

    synchronized byte[] stopRecording() {
        if (recorder == null) return new byte[0];
        recorder.stop();
        recorder.release();

        byte[] bytes = new byte[(int) recordingFile.length()];
        try (FileInputStream fis = new FileInputStream(recordingFile)) {
            int start = 0;
            while (bytes.length - start > 0) {
                int size = fis.read(bytes, start, bytes.length - start);
                if (size < 0) {
                    Log.e(TAG, "Failed in reading record file. EOF when start=%d, total len=%d", start, bytes.length);
                    return new byte[0];
                }
                start += size;
            }
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in reading record file.");
            return new byte[0];
        }
        boolean deleted = recordingFile.delete();
        if (!deleted) Log.d(TAG, "Delete record file %s failed", recordingFile.getAbsolutePath());
        recordingFile = null;
        recorder = null;

        // resume audio play
        if (currentPlayer != null) {
            Log.d(TAG, "resuming current player");
            currentPlayer.start();
        }
        return bytes;

    }

    private void stopPlayer() {
        if (currentMessage == null) return;
        currentPlayer.stop();
        currentPlayer.release();
        currentPlayer = null;
        currentMessage.setPlaying(false);
        currentMessage.setPlayed(true);
        messagePlayStateChangedHandler.accept(currentMessage);
        currentMessage = null;
    }

    private void startPlay(MessageViewModel viewModel) {
        currentMessage = viewModel;
        currentMessage.setPlaying(true);
        currentMessage.setPlayed(true);
        messagePlayStateChangedHandler.accept(currentMessage);

        currentPlayer = new MediaPlayer();
        currentPlayer.setDataSource(new MemoryMediaDataSource(viewModel.getMessage().getContent()));
        try {
            currentPlayer.prepare();
            currentPlayer.setOnCompletionListener(mp -> playNext());
            if (recorder == null) currentPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, e, "Failed in preparing media player");
            stopPlayer();
        }
    }

    private synchronized void playNext() {
        stopPlayer();
        if (autoPlay && !pendingMessages.isEmpty()) {
            MessageViewModel vm = pendingMessages.remove(0);
            play(vm);
        }
    }
}

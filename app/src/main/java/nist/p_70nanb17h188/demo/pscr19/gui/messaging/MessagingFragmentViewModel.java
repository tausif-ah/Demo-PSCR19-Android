package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.media.MediaMetadataRetriever;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import nist.p_70nanb17h188.demo.pscr19.Device;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.Action;
import nist.p_70nanb17h188.demo.pscr19.logic.Consumer;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.Constants;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.Message;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingName;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;
import nist.p_70nanb17h188.demo.pscr19.logic.net.NetLayer;

public class MessagingFragmentViewModel extends ViewModel {
    private static final String INITIATOR = "nist.p_70nanb17h188.demo.pscr19.gui.messaging.MessagingFragmentViewModel";
    static final boolean DEFAULT_AUTO_PLAY = false;

    private static MessagingFragmentViewModel DEFAULT_INSTANCE;
    MutableLiveData<Boolean> usingPnt = new MutableLiveData<>();
    MutableLiveData<Boolean> autoPlay = new MutableLiveData<>();
    MutableLiveData<Boolean> recording = new MutableLiveData<>();
    MutableLiveData<MessagingName> selectedName = new MutableLiveData<>();
    MutableLiveData<String> text = new MutableLiveData<>();
    MutableLiveData<String> searchInput = new MutableLiveData<>();
    private ArrayList<MessageViewModel> messages = new ArrayList<>();
    private Consumer<MessageViewModel> messageAddedHandler = null;
    private Action messageClearedHandler = null;
    private Action namespaceChangedHandler = null;
    private Consumer<Integer> messageUpdatedHandler = null;
    private AudioManager audioManager;
    private BroadcastReceiver onNamespaceChanged = (context, intent) -> {
        for (MessageViewModel msg : messages) msg.bindValues();
        if (namespaceChangedHandler != null) namespaceChangedHandler.accept();
    };

    private MessagingFragmentViewModel() {
        this.usingPnt.setValue(true);
        this.autoPlay.setValue(DEFAULT_AUTO_PLAY);
        this.recording.setValue(false);
        text.setValue("");
        // subscribe to channel.
        Name n = Constants.getDefaultSubscription();
        if (n != null) NetLayer.subscribe(n, this::dataReceived, INITIATOR);
        // listen to name changes
        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).registerReceiver(onNamespaceChanged, new IntentFilter().addAction(MessagingNamespace.ACTION_APPNAME_CHANGED).addAction(MessagingNamespace.ACTION_NAMESPACE_CHANGED));

        audioManager = new AudioManager(this::messagePlayStateChanged);
    }

    // adapter for messages

    public synchronized static void init() {
        if (DEFAULT_INSTANCE == null) DEFAULT_INSTANCE = new MessagingFragmentViewModel();
    }

    public static MessagingFragmentViewModel getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private void messagePlayStateChanged(MessageViewModel viewModel) {
        int idx = messages.indexOf(viewModel);
        if (idx >= 0 && messageUpdatedHandler != null) messageUpdatedHandler.accept(idx);
    }

    void flipAutoPlay() {
        assert autoPlay.getValue() != null;
        boolean newAutoPlay = !autoPlay.getValue();
        autoPlay.postValue(newAutoPlay);
        audioManager.setAutoPlay(newAutoPlay);
        Helper.notifyUser(LogType.Info, "Auto play turned %s", newAutoPlay ? "ON" : "OFF");
    }

    void flipPnt() {
        assert usingPnt.getValue() != null;
        usingPnt.postValue(!usingPnt.getValue());
    }

    void setMessageAddedHandler(@Nullable Consumer<MessageViewModel> messageAddedHandler) {
        this.messageAddedHandler = messageAddedHandler;
    }

    void setMessagesClearedHandler(@Nullable Action messageClearedHandler) {
        this.messageClearedHandler = messageClearedHandler;
    }

    void setNamespaceChangedHandler(@Nullable Action namespaceChangedHandler) {
        this.namespaceChangedHandler = namespaceChangedHandler;
    }

    void setMessageUpdatedHandler(@Nullable Consumer<Integer> messageUpdatedHandler) {
        this.messageUpdatedHandler = messageUpdatedHandler;
    }

    @NonNull
    MessageViewModel getMessageAtPosition(int position) {
        return messages.get(position);
    }

    int getMessageListSize() {
        return messages.size();
    }


    private void dataReceived(@NonNull Name src, @NonNull Name dst, @NonNull byte[] data, @NonNull String initiator) {
        // ignore the messages that I sent.
        if (initiator.equals(INITIATOR)) return;
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Message msg = Message.read(buffer);
        if (msg != null) {
            MessageViewModel toAdd = new MessageViewModel(msg);
            messages.add(toAdd);
            audioManager.addMessage(toAdd);
            if (messageAddedHandler != null) messageAddedHandler.accept(toAdd);
        }
    }

    void clearMessages() {
        messages.clear();
        audioManager.clear();
        if (messageClearedHandler != null) messageClearedHandler.accept();
    }

    synchronized void setRecording(boolean recording) {
        if (selectedName.getValue() == null) {
            Helper.notifyUser(LogType.Error, "Need to select a role to send to");
            return;
        }
        Name myName = Constants.getDefaultSubscription();
        assert myName != null;

        this.recording.postValue(recording);
        if (recording) {
            audioManager.startRecording();
        } else {
            byte[] buf = audioManager.stopRecording();
            if (buf.length == 0) return;
            MemoryMediaDataSource mmd = new MemoryMediaDataSource(buf);
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(mmd);
            long duration = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

            Message msg = new Message(System.currentTimeMillis(), duration, myName, selectedName.getValue().getName(), Device.getName(), new Name[0], Message.MessageType.PNT, "data:audio/aac;codecs=aac;base64,", buf);
            MessageViewModel toAdd = new MessageViewModel(msg);
            toAdd.setPlayed(true);
            messages.add(toAdd);
            if (messageAddedHandler != null) messageAddedHandler.accept(toAdd);
            ByteBuffer buffer = ByteBuffer.allocate(msg.getWriteSize());
            msg.write(buffer);
            NetLayer.sendData(myName, selectedName.getValue().getName(), buffer.array(), true, INITIATOR);
        }
    }

    boolean sendTextMessage() {
        if (selectedName.getValue() == null) {
            Helper.notifyUser(LogType.Error, "Need to select a role to send to");
            return false;
        }
        Name myName = Constants.getDefaultSubscription();
        assert myName != null;
        String message = text.getValue();
        if (message == null) message = "";
        Message msg = new Message(System.currentTimeMillis(), 0, myName, selectedName.getValue().getName(), Device.getName(), new Name[0], Message.MessageType.MSG, "", message.getBytes(Helper.DEFAULT_CHARSET));
        MessageViewModel viewModel = new MessageViewModel(msg);
        messages.add(viewModel);
        if (messageAddedHandler != null) messageAddedHandler.accept(viewModel);
        ByteBuffer buffer = ByteBuffer.allocate(msg.getWriteSize());
        msg.write(buffer);
        NetLayer.sendData(myName, selectedName.getValue().getName(), buffer.array(), true, INITIATOR);
        return true;
    }

    void playMessage(MessageViewModel viewModel) {
        audioManager.play(viewModel);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).unregisterReceiver(onNamespaceChanged);

    }
}

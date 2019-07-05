package nist.p_70nanb17h188.demo.pscr19.gui.messaging;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.R;
import nist.p_70nanb17h188.demo.pscr19.gui.WrapLinearLayoutManager;
import nist.p_70nanb17h188.demo.pscr19.gui.net.Constants;
import nist.p_70nanb17h188.demo.pscr19.gui.net.NameListArrayAdapter;
import nist.p_70nanb17h188.demo.pscr19.imc.BroadcastReceiver;
import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.imc.IntentFilter;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.Message;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingName;
import nist.p_70nanb17h188.demo.pscr19.logic.app.messaging.MessagingNamespace;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;
import nist.p_70nanb17h188.demo.pscr19.logic.net.Name;

public class MessagingFragment extends Fragment {
    private static final float BTN_MIN_HEIGHT_DP = 20;
    private static final float BTN_MIN_WIDTH_DP = 40;
    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private MessagingFragmentViewModel viewModel = MessagingFragmentViewModel.getDefaultInstance();
    private MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();
    private float scale;
    private FragmentActivity activity;
    private AutoCompleteTextView inputName;
    private NameListArrayAdapter inputNameAdapter;
    private PntButton btnPnt;
    private Button btnTarget;
    private TextView txtWarning;
    private EditText inputMessage;
    private InputMethodManager imm;
    private FloatingActionButton btnToBottom;
    private ImageButton btnAutoPlay;
    private ImageButton btnPntSwitch;
    private RecyclerView listMessages;
    private RecyclerView.Adapter<MessageViewHolder> listMessageAdapter = new RecyclerView.Adapter<MessageViewHolder>() {
        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_messaging_message, parent, false);
            return new MessageViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder messageViewHolder, int i) {
            messageViewHolder.bind(viewModel.getMessageAtPosition(i));
        }

        @Override
        public int getItemCount() {
            return viewModel.getMessageListSize();
        }
    };

    public MessagingFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messaging, container, false);
        activity = getActivity();
        assert activity != null;
        assert getContext() != null;
        imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        scale = getContext().getResources().getDisplayMetrics().density;

        inputName = view.findViewById(R.id.messaging_dst_input);
        btnToBottom = view.findViewById(R.id.messaging_btn_to_bottom);
        btnAutoPlay = view.findViewById(R.id.messaging_auto_play);
        btnTarget = view.findViewById(R.id.messaging_btn_target);
        btnPntSwitch = view.findViewById(R.id.messaging_pnt_switch);
        inputMessage = view.findViewById(R.id.messaging_txt_input);
        btnPnt = view.findViewById(R.id.messaging_pnt);
        txtWarning = view.findViewById(R.id.messaging_txt_warning);
        listMessages = view.findViewById(R.id.messaging_messages);

        inputName.setText(viewModel.searchInput.getValue(), TextView.BufferType.EDITABLE);
        inputNameAdapter = new NameListArrayAdapter(view.getContext(), false, new ArrayList<>());
        inputName.setAdapter(inputNameAdapter);
        inputName.setOnItemClickListener(this::inputNameItemClicked);
        inputName.addTextChangedListener(inputNameTextChanged);
        inputName.setOnFocusChangeListener(this::inputNameFocusChanged);

        btnAutoPlay.setOnClickListener(this::btnAutoPlayClicked);

        btnPntSwitch.setOnClickListener(this::btnPntSwitchClicked);

        btnPnt.setOnPressListener(this::btnPntPressed);
        btnPnt.setOnClickListener(v -> {
        }); // do nothing, just let phone play click sound

        inputMessage.setOnEditorActionListener(this::inputMessageEditorActionTriggered);
        inputMessage.setText(viewModel.text.getValue(), TextView.BufferType.EDITABLE);
        inputMessage.addTextChangedListener(inputMessageTextChanged);
        inputMessage.setOnFocusChangeListener(this::inputMessageFocusChanged);

        btnToBottom.setOnClickListener(this::btnToBottomClicked);

        listMessages.setAdapter(listMessageAdapter);
        listMessages.setLayoutManager(new WrapLinearLayoutManager(view.getContext()));

        viewModel.autoPlay.observe(this, this::autoPlayUpdated);
        viewModel.usingPnt.observe(this, this::usingPntUpdated);
        viewModel.recording.observe(this, this::recordingUpdated);
        viewModel.selectedName.observe(this, this::selectedNameUpdated);

        selectedNameUpdated(viewModel.selectedName.getValue());
        namespaceUpdated(null, null);


        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).registerReceiver(namespaceUpdateReceiver,
                new IntentFilter().addAction(MessagingNamespace.ACTION_NAMESPACE_CHANGED).addAction(MessagingNamespace.ACTION_APPNAME_CHANGED));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.usingPnt.removeObservers(this);
        viewModel.autoPlay.removeObservers(this);
        viewModel.recording.removeObservers(this);
        viewModel.selectedName.removeObservers(this);
        // text and search input do not have observers
        Context.getContext(MessagingNamespace.CONTEXT_MESSAGINGNAMESPACE).unregisterReceiver(namespaceUpdateReceiver);
    }


    /**
     * Events from UI
     */
    private void inputNameItemClicked(AdapterView<?> parent, View view, int position, long id) {
        Name n = inputNameAdapter.getItem(position);
        if (n == null) viewModel.selectedName.postValue(null);
        else
            viewModel.selectedName.postValue(MessagingNamespace.getDefaultInstance().getName(n));
        if (viewModel.usingPnt.getValue() == Boolean.FALSE) {
            inputMessage.requestFocus();
        } else {
            imm.hideSoftInputFromWindow(inputName.getWindowToken(), 0);
        }
    }

    private TextWatcher inputNameTextChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            viewModel.searchInput.setValue(s.toString());
        }
    };

    private void inputNameFocusChanged(View view, boolean hasFocus) {
        if (!hasFocus) imm.hideSoftInputFromWindow(inputName.getWindowToken(), 0);
    }

    private void btnAutoPlayClicked(View v) {
        viewModel.flipAutoPlay();
    }

    private void btnPntSwitchClicked(View v) {
        viewModel.flipPnt();
    }

    private void btnPntPressed(View v, boolean pressed) {
        viewModel.setRecording(pressed);
    }

    private boolean inputMessageEditorActionTriggered(TextView v, int actionId, KeyEvent event) {
        Helper.notifyUser(LogType.Info, "editorAction: id=%d, msg=%s", actionId, inputMessage.getText());
        return true;

    }

    private TextWatcher inputMessageTextChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            viewModel.text.setValue(s.toString());
        }
    };

    private void inputMessageFocusChanged(View view, boolean hasFocus) {
        if (hasFocus) imm.showSoftInput(view, 0);
        else imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void btnToBottomClicked(View v) {

    }

    /**
     * Events from ViewModel
     */

    private void selectedNameUpdated(MessagingName name) {
        if (name == null) {
            btnTarget.setVisibility(View.INVISIBLE);
            txtWarning.setVisibility(View.VISIBLE);
            btnPnt.setEnabled(false);
        } else {
            btnTarget.setVisibility(View.VISIBLE);
            txtWarning.setVisibility(View.INVISIBLE);
            btnTarget.setBackgroundTintList(getResources().getColorStateList(Constants.getNameTypeColorResource(name.getType()), activity.getTheme()));
            String[] nameIncidents = namespace.getNameIncidents(name.getName());
            btnTarget.setText(String.format(Locale.US, "%s %s", name.getAppName(), nameIncidents.length == 0 ? "" : Arrays.toString(nameIncidents)));
            btnPnt.setEnabled(true);
        }
        recordingUpdated(viewModel.recording.getValue());
    }

    private void usingPntUpdated(Boolean usingPnt) {
        assert usingPnt != null;
        if (usingPnt) {
            btnPntSwitch.setImageResource(R.drawable.ic_messaging_msg);
            inputMessage.setVisibility(View.INVISIBLE);
            btnPnt.setVisibility(View.VISIBLE);
        } else {
            btnPntSwitch.setImageResource(R.drawable.ic_messaging_pnt);
            inputMessage.setVisibility(View.VISIBLE);
            btnPnt.setVisibility(View.INVISIBLE);
            inputMessage.requestFocus();
            imm.showSoftInput(inputMessage, 0);
        }
    }

    private void recordingUpdated(Boolean recording) {
        assert recording != null;
        btnPnt.setText(viewModel.selectedName.getValue() == null || !recording
                ? R.string.messaging_push_to_record
                : R.string.messaging_release_to_send);
    }

    private void autoPlayUpdated(Boolean autoPlay) {
        assert autoPlay != null;
        btnAutoPlay.setImageResource(autoPlay ? R.drawable.ic_messaging_no_auto_play : R.drawable.ic_messaging_auto_play);
    }


    /**
     * Events from Context
     */
    private BroadcastReceiver namespaceUpdateReceiver = this::namespaceUpdated;

    private void namespaceUpdated(Context context, Intent intent) {
        MessagingNamespace namespace = MessagingNamespace.getDefaultInstance();

        inputNameAdapter.clearNames();
        namespace.forEachName(n -> inputNameAdapter.addName(n.getName()));
        inputNameAdapter.getFilter().filter(inputName.getText());
        MessagingName selectedName = viewModel.selectedName.getValue();
        if (selectedName != null) {
            MessagingName mn = namespace.getName(viewModel.selectedName.getValue().getName());
            viewModel.selectedName.postValue(mn);
        }
    }

    /**
     * ViewHolder
     */
    private class MessageViewHolder extends RecyclerView.ViewHolder {
        private Button btnSender, btnReceiver, btnAudio;
        private FlexboxLayout containerAttachment;
        private TextView txtTime, txtMessage;
        private ImageView imgAttachment;


        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            btnSender = itemView.findViewById(R.id.messaging_btn_sender);
            btnReceiver = itemView.findViewById(R.id.messaging_btn_receiver);
            btnAudio = itemView.findViewById(R.id.messaging_btn_voice);
            imgAttachment = itemView.findViewById(R.id.messaging_img_attachment);

            containerAttachment = itemView.findViewById(R.id.messaging_container_attachment);
            txtTime = itemView.findViewById(R.id.messaging_txt_time);
            txtMessage = itemView.findViewById(R.id.messaging_txt_message);
//            btnSender.setMinimumHeight();
        }

        void bind(MessageViewModel viewModel) {

            containerAttachment.removeAllViews();
            MessageViewModel.NameAttribute[] carriedNameAttributes = viewModel.getNameCarryAttributes();
            if (carriedNameAttributes.length == 0) {
                imgAttachment.setVisibility(View.GONE);
                containerAttachment.setVisibility(View.GONE);
            } else {
                imgAttachment.setVisibility(View.VISIBLE);
                containerAttachment.setVisibility(View.VISIBLE);
                for (MessageViewModel.NameAttribute attribute : carriedNameAttributes) {
                    Button btn = new Button(containerAttachment.getContext());
                    bindButton(btn, attribute);
                    if (attribute.getType() == null) btn.setEnabled(false);
                    btn.setTransformationMethod(null);
                    btn.setMinHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BTN_MIN_HEIGHT_DP, getResources().getDisplayMetrics()));
                    btn.setMinimumHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BTN_MIN_HEIGHT_DP, getResources().getDisplayMetrics()));
                    btn.setMinWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BTN_MIN_WIDTH_DP, getResources().getDisplayMetrics()));
                    btn.setMinimumWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BTN_MIN_WIDTH_DP, getResources().getDisplayMetrics()));
//                    btn.setOnClickListener();
                    containerAttachment.addView(btn, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
            }
            bindButton(btnSender, viewModel.getSenderAttribute());
            bindButton(btnReceiver, viewModel.getReceiverAttribute());
            txtTime.setText(DEFAULT_TIME_FORMAT.format(new Date(viewModel.getMessage().getSendTime())));
            if (viewModel.getMessage().getType() == Message.MessageType.PNT) {
                txtMessage.setVisibility(View.GONE);
                btnAudio.setVisibility(View.VISIBLE);
                btnAudio.setText(String.format(Locale.US, "%.3f", viewModel.getMessage().getDuration() / 1000.0));
            } else {
                txtMessage.setVisibility(View.VISIBLE);
                txtMessage.setText(new String(viewModel.getMessage().getContent(), Helper.DEFAULT_CHARSET));
                btnAudio.setVisibility(View.GONE);
            }


        }

        private void bindButton(Button btn, MessageViewModel.NameAttribute attribute) {
            btn.setText(attribute.getAppName());
            btn.setBackgroundTintList(getResources().getColorStateList(Constants.getNameTypeColorResource(attribute.getType()), activity.getTheme()));

        }
    }


}

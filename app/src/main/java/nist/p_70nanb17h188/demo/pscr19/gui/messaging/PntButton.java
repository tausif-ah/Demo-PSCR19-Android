package nist.p_70nanb17h188.demo.pscr19.gui.messaging;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

public class PntButton extends AppCompatButton {
    public interface OnPressListener {
        void onPress(PntButton btn, boolean pressed);
    }

    public PntButton(Context context) {
        super(context);
    }

    public PntButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PntButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (onPressListener != null) onPressListener.onPress(this, true);
                break;
            case MotionEvent.ACTION_UP:
                if (onPressListener != null) onPressListener.onPress(this, false);
                break;
        }
        return super.onTouchEvent(event);
    }


    private OnPressListener onPressListener;

    public void setOnPressListener(@NonNull OnPressListener listener) {
        onPressListener = listener;
    }


    @Override
    public boolean performClick() {
        return super.performClick();
    }
}

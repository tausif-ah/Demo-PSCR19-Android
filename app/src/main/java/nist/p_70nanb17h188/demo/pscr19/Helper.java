package nist.p_70nanb17h188.demo.pscr19;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Random;

import nist.p_70nanb17h188.demo.pscr19.imc.Context;
import nist.p_70nanb17h188.demo.pscr19.imc.Intent;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;

public class Helper {

    /**
     * A common interface shared between the logic and the gui, mostly for ui
     * updates like notifications.
     */
    public static final String CONTEXT_USER_INTERFACE = "nist.p_70nanb17h188.demo.pscr19.gui";

    /**
     * An action to let the gui notify the user something is happening. It
     * should have extra {@link #EXTRA_NOTIFICATION_CONTENT} ({@link String})
     * for the text to be notified. It also has an extra
     * {@link #EXTRA_NOTIFICATION_TYPE} ({@link LogType}) for the type of the
     * notification.
     */
    public static final String ACTION_NOTIFY_USER = "nist.p_70nanb17h188.demo.pscr19.gui.notifyUser";
    public static final String EXTRA_NOTIFICATION_CONTENT = "notificationContent";
    public static final String EXTRA_NOTIFICATION_TYPE = "notificationType";

    public static void notifyUser(@NonNull LogType type, @NonNull String content) {
        Context.getContext(CONTEXT_USER_INTERFACE).sendBroadcast(new Intent(ACTION_NOTIFY_USER).putExtra(EXTRA_NOTIFICATION_CONTENT, content).putExtra(EXTRA_NOTIFICATION_TYPE, type));
    }

    public static void notifyUser(@NonNull LogType type, @NonNull String fmt, Object... params) {
        notifyUser(type, String.format(Locale.US, fmt, params));
    }

    public static final Random DEFAULT_RANDOM = new Random();
    public static final String CANDIDATE_CHARSET_NUMBERS = "0123456789";
    public static final String CANDIDATE_CHARSET_HEX_NUMBERS = "0123456789ABCDEF";
    public static final String CANDIDATE_CHARSET_LETTERS_CAPITALIZED = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String CANDIDATE_CHARSET_LETTERS_UNCAPITALIZED = "abcdefghijklmnopqrstuvwxyz";
    public static final String CANDIDATE_CHARSET_LETTERS = CANDIDATE_CHARSET_LETTERS_UNCAPITALIZED + CANDIDATE_CHARSET_LETTERS_CAPITALIZED;
    public static final String CANDIDATE_CHARSET_LETTERS_NUMBERS = CANDIDATE_CHARSET_LETTERS + CANDIDATE_CHARSET_NUMBERS;
    public static final String CANDIDATE_CHARSET_LETTERS_NUMBERS_SPACES = CANDIDATE_CHARSET_LETTERS_NUMBERS + " \t\n";
    public static final int INTEGER_SIZE, LONG_SIZE, DOUBLE_SIZE;
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            INTEGER_SIZE = Integer.BYTES;
            LONG_SIZE = Long.BYTES;
            DOUBLE_SIZE = Double.BYTES;
        } else {
            INTEGER_SIZE = 4;
            LONG_SIZE = 8;
            DOUBLE_SIZE = 8;
        }
    }

    private Helper() {
    }

    public static void printStackTrace(PrintStream ps) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < elements.length; i++) {
            StackTraceElement s = elements[i];
            ps.printf("\tat %s.%s(%s:%d)%n", s.getClassName(), s.getMethodName(), s.getFileName(), s.getLineNumber());
        }
    }

    @NonNull
    public static String getHexString(@NonNull byte[] buf) {
        return getHexString(buf, 0, buf.length);
    }

    @NonNull
    public static String getHexString(@NonNull byte[] buf, int start, int len) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < len; i++) {
            byte b = buf[start + i];
            ret.append(CANDIDATE_CHARSET_HEX_NUMBERS.charAt((b >> 4) & 0xF));
            ret.append(CANDIDATE_CHARSET_HEX_NUMBERS.charAt(b & 0xF));
        }
        return ret.toString();

    }

    @NonNull
    public static String getRandomString(int lengthMin, int lengthMax, @NonNull String candidateCharSet) {
        if (lengthMin <= 0 || lengthMax < lengthMin) {
            throw new IllegalArgumentException("lengthMin and lengthMax should be > 0 and lengthMax should > lengthMin");
        }

        int length = lengthMin == lengthMax ? lengthMin : DEFAULT_RANDOM.nextInt(lengthMax - lengthMin) + lengthMin;
        char[] ret = new char[length];
        for (int i = 0; i < length; i++) {
            ret[i] = candidateCharSet.charAt(DEFAULT_RANDOM.nextInt(candidateCharSet.length()));
        }
        return new String(ret);
    }

    public static long getPositiveRandom() {
        for (; ; ) {
            long v = DEFAULT_RANDOM.nextLong();
            if (v == Long.MIN_VALUE || v == 0) continue;
            return v < 0 ? -v : v;
        }
    }

    public static int getStringWriteSize(String str) {
        return str.getBytes(DEFAULT_CHARSET).length + Helper.INTEGER_SIZE;
    }

    public static void writeString(ByteBuffer buffer, String str) {
        byte[] bStr = str.getBytes(DEFAULT_CHARSET);
        buffer.putInt(bStr.length);
        buffer.put(bStr);
    }

    @Nullable
    public static String readString(ByteBuffer buffer) {
        if (buffer.remaining() < INTEGER_SIZE) return null;
        int length = buffer.getInt();
        if (buffer.remaining() < length) return null;
        byte[] bStr = new byte[length];
        buffer.get(bStr);
        return new String(bStr, DEFAULT_CHARSET);
    }

    public static int getByteArrayWriteSize(byte[] content) {
        return Helper.INTEGER_SIZE + content.length;
    }

    public static void writeByteArray(ByteBuffer buffer, byte[] content) {
        buffer.putInt(content.length);
        buffer.put(content);
    }

    @Nullable
    public static byte[] readByteArray(ByteBuffer buffer) {
        if (buffer.remaining() < INTEGER_SIZE) return null;
        int length = buffer.getInt();
        if (buffer.remaining() < length) return null;
        byte[] content = new byte[length];
        buffer.get(content);
        return content;
    }

    public static void checkValidSendDataParams(@NonNull byte[] data, int start, int len) {
        if (len < 0 || start + len > data.length)
            throw new IllegalArgumentException(String.format(Locale.US, "wrong start(%d) or len(%d) value, data.length=%d", start, len, data.length));
    }


}

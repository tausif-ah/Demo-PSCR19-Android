package nist.p_70nanb17h188.demo.pscr19.logic;

import android.os.Build;
import android.support.annotation.NonNull;

import java.io.PrintStream;
import java.util.Random;

public class Helper {
    public static final Random DEFAULT_RANDOM = new Random();
    public static final String CANDIDATE_CHARSET_NUMBERS = "0123456789";
    public static final String CANDIDATE_CHARSET_LETTERS_CAPITALIZED = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String CANDIDATE_CHARSET_LETTERS_UNCAPITALIZED = "abcdefghijklmnopqrstuvwxyz";
    public static final String CANDIDATE_CHARSET_LETTERS = CANDIDATE_CHARSET_LETTERS_UNCAPITALIZED + CANDIDATE_CHARSET_LETTERS_CAPITALIZED;
    public static final String CANDIDATE_CHARSET_LETTERS_NUMBERS = CANDIDATE_CHARSET_LETTERS + CANDIDATE_CHARSET_NUMBERS;
    public static final String CANDIDATE_CHARSET_LETTERS_NUMBERS_SPACES = CANDIDATE_CHARSET_LETTERS_NUMBERS + " \t\n";
    public static final int INTEGER_SIZE;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            INTEGER_SIZE = Integer.BYTES;
        } else {
            INTEGER_SIZE = 4;
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
    public static String getRandomString(int lengthMin, int lengthMax, @NonNull String candidateCharSet) {
        if (lengthMin <= 0 || lengthMax < lengthMin)
            throw new IllegalArgumentException("lengthMin and lengthMax should be > 0 and lengthMax should > lengthMin");

        int length = lengthMin == lengthMax ? lengthMin : DEFAULT_RANDOM.nextInt(lengthMax - lengthMin) + lengthMin;
        char[] ret = new char[length];
        for (int i = 0; i < length; i++) {
            ret[i] = candidateCharSet.charAt(DEFAULT_RANDOM.nextInt(candidateCharSet.length()));
        }
        return new String(ret);
    }

}

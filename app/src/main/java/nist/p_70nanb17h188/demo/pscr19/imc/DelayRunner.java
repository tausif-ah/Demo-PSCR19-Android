package nist.p_70nanb17h188.demo.pscr19.imc;

import android.os.Handler;
import android.os.Looper;

public class DelayRunner implements Runnable {

    public interface ParameterizedRunnable {
        void run(Object... params);
    }

    private class ParameterizedRunnableRunner implements Runnable {
        private final ParameterizedRunnable r;
        private final Object[] params;

        ParameterizedRunnableRunner(ParameterizedRunnable r, Object[] params) {
            this.r = r;
            this.params = params;
        }

        @Override
        public void run() {
            r.run(params);
        }
    }

    private static DelayRunner DEFAULT_INSTANCE;

    public synchronized static DelayRunner getDefaultInstance() {
        if (DEFAULT_INSTANCE == null) {
            DEFAULT_INSTANCE = new DelayRunner();
            DEFAULT_INSTANCE.start();
            while (DEFAULT_INSTANCE.handler == null) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        }
        return DEFAULT_INSTANCE;
    }

    private Handler handler;

    private DelayRunner() {
    }

    private void start() {
        Thread loopThread = new Thread(this);
        loopThread.setDaemon(true);
        loopThread.start();
    }

    public void post(Runnable r) {
        handler.post(r);
    }

    public void postDelayed(long delay, Runnable r) {
        handler.postDelayed(r, delay);
    }

    public void postAtTime(long time, Runnable r) {
        handler.postAtTime(r, time);
    }

    public void post(ParameterizedRunnable r, Object... params) {
        handler.post(new ParameterizedRunnableRunner(r, params));
    }

    public void postDelayed(long delay, ParameterizedRunnable r, Object... params) {
        handler.postDelayed(new ParameterizedRunnableRunner(r, params), delay);
    }

    public void postAtTime(long time, ParameterizedRunnable r, Object... params) {
        handler.postAtTime(new ParameterizedRunnableRunner(r, params), time);
    }


    @Override
    public void run() {
        Looper.prepare();

        handler = new android.os.Handler();
        Looper.loop();
    }


}

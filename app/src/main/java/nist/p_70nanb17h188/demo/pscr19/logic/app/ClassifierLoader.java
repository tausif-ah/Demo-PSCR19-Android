package nist.p_70nanb17h188.demo.pscr19.logic.app;

import android.os.Environment;

import java.io.File;

import nist.p_70nanb17h188.demo.pscr19.FaceUtil;
import nist.p_70nanb17h188.demo.pscr19.Helper;
import nist.p_70nanb17h188.demo.pscr19.logic.log.Log;
import nist.p_70nanb17h188.demo.pscr19.logic.log.LogType;


public class ClassifierLoader implements Runnable {
    private static final String TAG = "ClassifierLoader";

    public void run() {
        synchronized (FaceUtil.faceRecognizer) {
//            String path = Environment.getExternalStorageDirectory().getPath()+"/faces/reco.yml";
            File file = new File(Environment.getExternalStorageDirectory(), "faces/reco.yml");
//            File file = new File(path);
            if (file.exists()) {
                Log.d(TAG, "Start loading face recognition classifier");
                FaceUtil.faceRecognizer.load(file.getAbsolutePath());
                Log.d(TAG, "Face recognition classifier loaded");
                Helper.notifyUser(LogType.Info, "Face recognition classifier loaded");
            } else {
                Log.e(TAG, "Cannot find classifier file: %s", file.getAbsolutePath());
                Helper.notifyUser(LogType.Error, "Cannot find classifier file: %s", file.getAbsolutePath());
            }
        }
    }

}

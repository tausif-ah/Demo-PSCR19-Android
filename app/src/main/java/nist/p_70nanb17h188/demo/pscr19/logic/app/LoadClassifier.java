package nist.p_70nanb17h188.demo.pscr19.logic.app;

import android.os.Environment;

import java.io.File;

import nist.p_70nanb17h188.demo.pscr19.FaceUtil;


public class LoadClassifier implements Runnable {

    public void run(){
        synchronized (FaceUtil.faceRecognizer){
            String path = Environment.getExternalStorageDirectory().getPath()+"/faces/reco.yml";
            File file = new File(path);
            if(file.exists()){
                FaceUtil.faceRecognizer.load(path);
            }
        }
    }

}

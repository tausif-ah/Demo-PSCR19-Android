package nist.p_70nanb17h188.demo.pscr19.logic.app;

import android.os.Environment;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_face;
import org.bytedeco.javacpp.opencv_objdetect;

import java.util.concurrent.Callable;

import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.resize;

/**
 * Created by yuxuanxing on 4/15/18.
 */

public class EachHelper implements Callable<double[]> {

    opencv_face.FaceRecognizer faceRecognizer;
    opencv_objdetect.CascadeClassifier faceDetector;

    int start;

    public EachHelper(opencv_objdetect.CascadeClassifier faceDetector, opencv_face.FaceRecognizer faceRecognizer, int start){
        this.faceDetector = faceDetector;
        this.faceRecognizer = faceRecognizer;
        this.start = start;
    }

    @Override
    public double[] call() throws Exception {
        return local();
    }

    double[] local(){
        double[] result = new double[2];//seq prediction confidence id
        double maxLevel = 50000;
        while(start<=200){
            String testDir = Environment.getExternalStorageDirectory().getPath()+"/Movies/faces/test/img ("+start+").jpg";
            opencv_core.Mat testImage = detectFaces(testDir);
            if(testImage==null){
                return result;
            }
            IntPointer label = new IntPointer(1);
            DoublePointer reliability = new DoublePointer(1);
            faceRecognizer.predict(testImage, label, reliability);
            int prediction = label.get(0);
            double acceptanceLevel = reliability.get(0);
            if(prediction==1&&acceptanceLevel<maxLevel){
                result[0] = start;
                result[1] = acceptanceLevel;
                maxLevel = acceptanceLevel;
            }
            start++;
        }
        return result;
    }

    opencv_core.Mat detectFaces(String filePath){
        opencv_core.Mat grey = imread(filePath,CV_LOAD_IMAGE_GRAYSCALE);
        opencv_core.RectVector detectedFaces = new opencv_core.RectVector();
        faceDetector.detectMultiScale(grey, detectedFaces, 1.1, 1, 0, new opencv_core.Size(150,150), new opencv_core.Size(500,500));
        opencv_core.Rect rectFace = detectedFaces.get(0);
        if(rectFace==null){
            return null;
        }
        opencv_core.Mat capturedFace = new opencv_core.Mat(grey, rectFace);
        resize(capturedFace, capturedFace, new opencv_core.Size(160,160));
        return capturedFace;
    }

}

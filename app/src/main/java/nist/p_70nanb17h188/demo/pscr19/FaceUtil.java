package nist.p_70nanb17h188.demo.pscr19;

import android.os.Environment;

import org.bytedeco.javacpp.opencv_face;
import org.bytedeco.javacpp.opencv_objdetect;

import java.io.File;

import static org.bytedeco.javacpp.opencv_face.createEigenFaceRecognizer;

public class FaceUtil {

    private static final String detectorPath = Environment.getExternalStorageDirectory().getPath()+"/faces/frontalface.xml";

    private static File file = new File(detectorPath);

    public static final opencv_objdetect.CascadeClassifier faceDetector = file.exists()?new opencv_objdetect.CascadeClassifier(detectorPath):null;

    public static final opencv_face.FaceRecognizer faceRecognizer = createEigenFaceRecognizer();

}

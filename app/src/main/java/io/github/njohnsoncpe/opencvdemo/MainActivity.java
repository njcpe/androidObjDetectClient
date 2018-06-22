package io.github.njohnsoncpe.opencvdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    Mat raw, lowRes, greyScale;
    MatOfByte matOfByte;
    byte[] imgArray;
    String imgStr64;



/*      NETWORKING    */

/*      NETWORKING - END        */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Start Camera
        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.myCameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                switch (status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        raw = inputFrame.rgba();
        //Imgproc.resize(raw, lowRes, lowRes.size()); //Resize to 480x270 resolution
        //Imgproc.cvtColor(lowRes, greyScale, Imgproc.COLOR_RGB2GRAY); //Convert to greyscale colorspace
        //Do image processing and encoding here

        //Scenario 1 Offloading
        matOfByte = new MatOfByte(); //Create Image buffer for encoding
        Imgcodecs.imencode(".jpeg", raw, matOfByte); //Encode greyscale img into memory buffer
        imgArray = matOfByte.toArray();
        imgStr64 = new String(Base64.encode(imgArray, Base64.DEFAULT));

        //resize to display
        //Imgproc.cvtColor(greyScale, greyScale, Imgproc.COLOR_GRAY2BGR);
        //Imgproc.resize(greyScale, lowRes,raw.size());
        return raw;
    }


    @Override
    public void onCameraViewStopped() {
        raw.release();
        lowRes.release();
        greyScale.release();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Size downScale = new Size(480,270);
        raw = new Mat(width, height, CvType.CV_8UC4);
        lowRes = new Mat(downScale, CvType.CV_8UC4);
        greyScale = new Mat(downScale, CvType.CV_8UC1);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase != null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(), "OpenCV Init Fail", Toast.LENGTH_SHORT).show();
        }else{
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase != null){
            cameraBridgeViewBase.disableView();
        }
    }
}

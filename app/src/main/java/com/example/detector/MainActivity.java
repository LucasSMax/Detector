package com.example.detector;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Imgproc.*;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static
    {
        System.loadLibrary("opencv_java");
    }

    private static String nome;
    private static String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    Mat mRgba, mOut, mGray, mCrop;
    File mPredictorFile, mCascadeFile;
    private Bitmap bmp = null;
    private MatOfRect faces = new MatOfRect();
    private CascadeClassifier cascadeFace;
    private int absoluteFaceSize;
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status)
            {
                case BaseLoaderCallback.SUCCESS:{
                    javaCameraView.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        try
        {
            InputStream is2 = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
            FileOutputStream os2 = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is2.read(buffer)) != -1)
            {
                os2.write(buffer, 0, bytesRead);
            }
            is2.close();
            os2.close();

            cascadeFace = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug())
        {
            Log.i(TAG, "Good");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }
        else
        {
            Log.i(TAG, "Bad");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, baseLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mOut = new Mat(height, width, CvType.CV_8UC4);
        mCrop = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        absoluteFaceSize = (int) (height * 0.2);

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mOut.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);


        if(cascadeFace != null)
        {
            cascadeFace.detectMultiScale(mGray, faces, 1.1,2,2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }


        for (Rect rect : faces.toArray()) {
            mCrop = new Mat(mGray, rect);
            Imgproc.resize(mCrop,mCrop,new Size(96,96));
            bmp = Bitmap.createBitmap(mCrop.cols(), mCrop.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mCrop, bmp);
            updateImage();

            if(nome != null) {
                Core.putText(mRgba, nome, new Point((rect.y +  10), (rect.x +  10)),
                        Core.FONT_HERSHEY_COMPLEX, 1.0, new Scalar(0, 255, 0));
                nome = null;
            }
            Core.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(0, 255, 0, 255), 3);
        }
        return mRgba;
    }

    private void updateImage()
    {
        final String image = imageToString();
        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        Call<ImageClass> call = apiInterface.uploadImage(image);
        call.enqueue(new Callback<ImageClass>() {
            @Override
            public void onResponse(Call<ImageClass> call, Response<ImageClass> response) {
                ImageClass imageClass = response.body();
                //Toast.makeText(MainActivity.this, "Server response: "+imageClass.getResponse(), Toast.LENGTH_LONG).show();
               nome = imageClass.getResponse();
            }

            @Override
            public void onFailure(Call<ImageClass> call, Throwable t) {
                //Toast.makeText(MainActivity.this, "Server error", Toast.LENGTH_LONG).show();
            }
        });

    }

    private String imageToString()
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
        byte[] imgByte = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgByte, Base64.DEFAULT);
    }

}

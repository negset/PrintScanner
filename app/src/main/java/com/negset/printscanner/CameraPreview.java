package com.negset.printscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CameraPreview implements SurfaceHolder.Callback, Camera.PictureCallback {
    private final Context context;
    private final SurfaceView surfaceView;
    private Camera camera;
    private Timer afTimer;
    private static final double ASPECT_RATIO = Math.sqrt(2);

    public CameraPreview(Context context, SurfaceView surfaceView) {
        this.context = context;
        this.surfaceView = surfaceView;
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            int openCameraType = Camera.CameraInfo.CAMERA_FACING_BACK;
            if (openCameraType <= Camera.getNumberOfCameras()) {
                camera = Camera.open(openCameraType);
                camera.setPreviewDisplay(holder);
            } else {
                Log.e("CameraPreview", "cannot bind camera.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        afTimer = new Timer();
        afTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (camera != null) {
                    camera.autoFocus(null);
                }
            }
        }, 1000, 5000);
        Log.d("CameraPreview", "timer created.");

        // set aspect ratio of SurfaceView
//        ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
//        double ratio = (double) surfaceView.getWidth() / surfaceView.getHeight();
//        if (ratio > ASPECT_RATIO) {
//            layoutParams.width = (int) (surfaceView.getHeight() * ASPECT_RATIO);
//        } else {
//            layoutParams.height = (int) (surfaceView.getWidth() / ASPECT_RATIO);
//        }
//        surfaceView.setLayoutParams(layoutParams);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int width, int height) {
        Log.d("CameraPreview", "surfaceChanged: (" + width + ", " + height + ")");
        double ratio = setPreviewSize(width, height);
        if (ratio > 0) setPictureSize(ratio);
        camera.startPreview();
    }

    private double setPreviewSize(int width, int height) {
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        for (Camera.Size size : sizes) {
            if (size.width <= width && size.height <= height) {
                Log.d("CameraPreview", "set preview size: (" + size.width + ", " + size.height + ")");
                params.setPreviewSize(size.width, size.height);
                try {
                    camera.setParameters(params);
                } catch (RuntimeException e) {
                    Log.e("CameraPreview", "cannot set preview size.");
                }
                return (double) size.width / size.height;
            }
        }
        return -1;
    }

    private double setPictureSize(double aspectRatio) {
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (ratio == aspectRatio) {
                Log.d("CameraPreview", "set picture size: (" + size.width + ", " + size.height + ")");
                params.setPictureSize(size.width, size.height);
                camera.setParameters(params);
                return ratio;
            }
        }
        return -1;
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        afTimer.cancel();
        Log.d("CameraPreview", "timer canceled.");

        camera.cancelAutoFocus();
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, null);
        String title = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date(System.currentTimeMillis()));
        MediaStore.Images.Media.insertImage(context.getContentResolver(), bmp, title, null);
        camera.startPreview();
    }

    void autoFocus() {
        camera.autoFocus(null);
    }

    void takePicture() {
        camera.autoFocus((success, camera) -> camera.takePicture(null, null, this));
    }
}

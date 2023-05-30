package com.negset.printscanner;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CameraActivity extends AppCompatActivity implements SensorEventListener {
    private CameraPreview cameraPreview;
    private SurfaceView surfaceView;
    private ImageView imageAzimuthOver;
    private ImageView imageAzimuthUnder;
    private ImageView imagePitchOver;
    private ImageView imagePitchUnder;
    private ImageView imageRollOver;
    private ImageView imageRollUnder;

    /* orientation */
    private SensorManager sensorManager;
    private final float[] inRotationMatrix = new float[9];
    private final float[] outRotationMatrix = new float[9];
    private final float[] orientation = new float[3];
    private float[] targetOrientation;
    private static final double ORIENTATION_THRESHOLD = 5;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        surfaceView = findViewById(R.id.surface_camera_preview);
        cameraPreview = new CameraPreview(this, surfaceView);
        surfaceView.setOnTouchListener((v, e) -> {
            int action = e.getAction() & MotionEvent.ACTION_MASK;
            if (action == MotionEvent.ACTION_DOWN) {
                cameraPreview.autoFocus();
            }
            return true;
        });

        imagePitchOver = findViewById(R.id.imageview_pitch_over);
        imagePitchUnder = findViewById(R.id.imageview_pitch_under);
        imageRollOver = findViewById(R.id.imageview_roll_over);
        imageRollUnder = findViewById(R.id.imageview_roll_under);
        imageAzimuthOver = findViewById(R.id.imageview_azimuth_over);
        imageAzimuthUnder = findViewById(R.id.imageview_azimuth_under);

        FloatingActionButton fabScan = findViewById(R.id.fab_scan);
        fabScan.setOnClickListener(view -> cameraPreview.takePicture());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        Log.d("CameraActivity", "onDestroy");
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        sensorManager.unregisterListener(this, sensor);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            if (sensorEvent.values[0] == 0 && sensorEvent.values[1] == 0 &&
                    sensorEvent.values[2] == 0 && sensorEvent.values[3] == 1) {
                // skip unreliable values
                Log.d("CameraActivity", "SensorEvent skipped");
                return;
            }

            SensorManager.getRotationMatrixFromVector(inRotationMatrix, sensorEvent.values);
            SensorManager.remapCoordinateSystem(inRotationMatrix,
                    SensorManager.AXIS_X, SensorManager.AXIS_Y, outRotationMatrix);
            SensorManager.getOrientation(outRotationMatrix, orientation);
            onOrientationChanged(orientation);
        }
    }

    private void onOrientationChanged(float[] orientation) {
        if (targetOrientation == null) {
            // first call
            targetOrientation = orientation.clone();
            return;
        }
        double azimuthDelta = getDeltaDegrees(orientation[0], targetOrientation[0]);
        double pitchDelta = getDeltaDegrees(orientation[1], targetOrientation[1]);
        double rollDelta = getDeltaDegrees(orientation[2], targetOrientation[2]);

        hideAllImageViews();

        if (Math.abs(azimuthDelta) > Math.abs(pitchDelta)
                && Math.abs(azimuthDelta) > Math.abs(rollDelta)) {
            // azimuth
            if (azimuthDelta > ORIENTATION_THRESHOLD) {
                // over
                Log.d("CameraActivity", "current image: azimuth over");
                showImageView(imageAzimuthOver);
            } else if (azimuthDelta < -ORIENTATION_THRESHOLD) {
                // under
                Log.d("CameraActivity", "current image: azimuth under");
                showImageView(imageAzimuthUnder);
            }
        } else if (Math.abs(pitchDelta) > Math.abs(rollDelta)) {
            // pitch
            if (pitchDelta > ORIENTATION_THRESHOLD) {
                // over
                Log.d("CameraActivity", "current image: pitch over");
                showImageView(imagePitchOver);
            } else if (pitchDelta < -ORIENTATION_THRESHOLD) {
                // under
                Log.d("CameraActivity", "current image: pitch under");
                showImageView(imagePitchUnder);
            }
        } else {
            // roll
            if (rollDelta > ORIENTATION_THRESHOLD) {
                // over
                Log.d("CameraActivity", "current image: roll over");
                showImageView(imageRollOver);
            } else if (rollDelta < -ORIENTATION_THRESHOLD) {
                // under
                Log.d("CameraActivity", "current image: roll under");
                showImageView(imageRollUnder);
            }
        }
    }

    /**
     * Computes delta angle of target and actual in degrees.
     *
     * @param target target in radians (-π to π)
     * @param actual actual in radians (-π to π)
     * @return delta in degrees (-180 to 180)
     */
    private double getDeltaDegrees(double actual, double target) {
        double delta = actual - target;
        if (delta > Math.PI)
            delta -= Math.PI * 2;
        else if (delta < -Math.PI)
            delta += Math.PI * 2;
        return Math.toDegrees(delta);
    }

    private void hideAllImageViews() {
        surfaceView.setBackgroundColor(Color.argb(0, 0, 0, 0));
        imagePitchOver.setVisibility(View.INVISIBLE);
        imagePitchUnder.setVisibility(View.INVISIBLE);
        imageRollOver.setVisibility(View.INVISIBLE);
        imageRollUnder.setVisibility(View.INVISIBLE);
        imageAzimuthOver.setVisibility(View.INVISIBLE);
        imageAzimuthUnder.setVisibility(View.INVISIBLE);
    }

    private void showImageView(ImageView imageView) {
        surfaceView.setBackgroundColor(Color.argb(200, 0, 0, 0));
        imageView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
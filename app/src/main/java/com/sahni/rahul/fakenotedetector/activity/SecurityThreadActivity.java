package com.sahni.rahul.fakenotedetector.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sahni.rahul.fakenotedetector.ui.AutoFitTextureView;
import com.sahni.rahul.fakenotedetector.ui.ColourIndicatorView;
import com.sahni.rahul.fakenotedetector.utils.Constants;
import com.sahni.rahul.fakenotedetector.R;
import com.sahni.rahul.fakenotedetector.classifier.SecurityThreadClassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SecurityThreadActivity extends AppCompatActivity {

    private static final String TAG = SecurityThreadActivity.class.getSimpleName();
    private AutoFitTextureView textureView;

    private static final int REQUEST_CAMERA_PERMISSION = 200;


    private FrameLayout frameLayout;
    private TextView resultTextView;
    private TextView timeTextView;
    private ColourIndicatorView colourIndicatorView;
    private ImageView imageView;
    private TextView nextTextView;

    private SecurityThreadClassifier securityThreadClassifier;

    private boolean isFlashOn = false;
    private CameraManager cameraManager;
    private String cameraId;
    private Size previewSize;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    /** Max preview width that is guaranteed by Camera2 API */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /** Max preview height that is guaranteed by Camera2 API */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final String HANDLE_THREAD_NAME = "CameraBackground";
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private boolean isBlueDetected = false;
    private boolean isGreenDetected = false;
    private float bluePrediction = 1f;
    private float greenPrediction = 0f;

    private static final int MAX_SCAN = 4;
    private int scanCount = 0;

    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    setUpCamera(width, height);
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                }
            };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            SecurityThreadActivity.this.cameraDevice = cameraDevice;
            createPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            SecurityThreadActivity.this.cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            SecurityThreadActivity.this.cameraDevice = null;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_classifier);

        textureView = findViewById(R.id.texture_view);
        frameLayout = findViewById(R.id.frame_layout);
        resultTextView = findViewById(R.id.result_text_view);
        timeTextView = findViewById(R.id.time_text_view);
        colourIndicatorView = findViewById(R.id.colourIndicatorView);
        imageView = findViewById(R.id.imageView3);
        nextTextView = findViewById(R.id.next_text_view);
        final ImageView flashImageView = findViewById(R.id.flash_image_view);

        colourIndicatorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick:");
                if(scanCount < MAX_SCAN){
                    scanCount++;
                    if(!isGreenDetected || !isBlueDetected) {
                        classify();
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SecurityThreadActivity.this)
                            .setMessage("Colours not detected!")
                            .setPositiveButton("skip", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    startNextActivity(false);

                                }
                            })
                            .setNegativeButton("Try Again", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    reset();
                                }
                            })
                            .setCancelable(false);
                    builder.create().show();

                }
            }
        });

        colourIndicatorView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "onLongClick:");
                colourIndicatorView.showBlue();
                colourIndicatorView.showGreen();

                greenPrediction = 1f;
                bluePrediction = 0f;

                Toast.makeText(SecurityThreadActivity.this, "Colours Detected!", Toast.LENGTH_SHORT).show();
                nextTextView.setVisibility(View.VISIBLE);

                return true;
            }
        });


        nextTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNextActivity(true);
            }
        });

        flashImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFlashOn){
                    flashImageView.setImageResource(R.drawable.ic_flash_on_black_24dp);
                } else {
                    flashImageView.setImageResource(R.drawable.ic_flash_off_black_24dp);
                }
                isFlashOn = !isFlashOn;
                createPreviewSession();
            }
        });


        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        Log.d(TAG, "onCreate: Initializing classifier");
        try {
            Log.d(TAG, "onCreate: Initializing classifier");
            securityThreadClassifier = new SecurityThreadClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "onCreate: Failed to load model!");
            Toast.makeText(this, "Failed to load Model!",Toast.LENGTH_SHORT).show();
        }


    }

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCamera(int width, int height) {
//        Point displaySize = new Point();
//        getWindowManager().getDefaultDisplay().getSize(displaySize);
//        int maxPreviewWidth = displaySize.x;
//        int maxPreviewHeight = displaySize.y;
//        StreamConfigurationMap streamConfigurationMap = null;
//        int rotatedPreviewWidth = width;
//        int rotatedPreviewHeight = height;

        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);


                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {

                    Point displaySize = new Point();
                    getWindowManager().getDefaultDisplay().getSize(displaySize);
                    int maxPreviewWidth = displaySize.x;
                    int maxPreviewHeight = displaySize.y;
                    StreamConfigurationMap streamConfigurationMap = null;
                    int rotatedPreviewWidth = width;
                    int rotatedPreviewHeight = height;

                    if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                        maxPreviewWidth = MAX_PREVIEW_WIDTH;
                    }

                    if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                        maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                    }


                    Log.d(TAG, "setUpCamera: Back camera");
                    streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    Size largest =
                            Collections.max(
                                    Arrays.asList(streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

//                    previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class),
//                            maxPreviewWidth, maxPreviewHeight);
//                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];

                    previewSize = chooseOptimalSize(
                            streamConfigurationMap.getOutputSizes(SurfaceTexture.class),
                            rotatedPreviewWidth,
                            rotatedPreviewHeight,
                            maxPreviewWidth,
                            maxPreviewHeight,
                            largest);

                    this.cameraId = cameraId;
                    int orientation = getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
                    } else {
                        textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
                    }

//                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                }


            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            if(isFlashOn) {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            } else {
                captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }

            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                CaptureRequest captureRequest = captureRequestBuilder.build();
                                SecurityThreadActivity.this.cameraCaptureSession = cameraCaptureSession;
                                SecurityThreadActivity.this.cameraCaptureSession.setRepeatingRequest(captureRequest,
                                        null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void openBackgroundThread() {
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
//        backgroundHandler.postDelayed(periodicClassify, 2000);
    }

    private void closeBackgroundThread() {
        if (backgroundHandler != null) {
//            backgroundHandler.removeCallbacks(periodicClassify);
            backgroundThread.quitSafely();
            backgroundThread = null;
            backgroundHandler = null;
        }
    }


    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }


    private Size chooseOptimalSize(Size[] outputSizes, int width, int height) {
        double preferredRatio = height / (double) width;
        Size currentOptimalSize = outputSizes[0];
        double currentOptimalRatio = currentOptimalSize.getWidth() / (double) currentOptimalSize.getHeight();
        for (Size currentSize : outputSizes) {
//            Log.d(TAG, "chooseOptimalSize: Width:"+currentSize.getWidth()+"   Height:"+currentSize.getHeight());
//            Log.d(TAG, "chooseOptimalSize:");

            double currentRatio = currentSize.getWidth() / (double) currentSize.getHeight();
            if (Math.abs(preferredRatio - currentRatio) <
                    Math.abs(preferredRatio - currentOptimalRatio)) {
                currentOptimalSize = currentSize;
                currentOptimalRatio = currentRatio;
            }
        }
        return currentOptimalSize;
    }


    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        if (textureView.isAvailable()) {
            setUpCamera(textureView.getWidth(), textureView.getHeight());
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    @Override
    public void onDestroy() {
        securityThreadClassifier.close();
        super.onDestroy();
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(SecurityThreadActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }



    private void classify(){
        if(securityThreadClassifier == null || cameraDevice==null){
            Toast.makeText(this, "Uninitialized SecurityFeatureClassifier or invalid context.", Toast.LENGTH_SHORT).show();
            return;
        }

        int[] location = new int[2];
//        frameLayout.getLocationOnScreen(location);
        frameLayout.getLocationInWindow(location);
        int x = location[0];
        int y = location[1];

        textureView.getLocationOnScreen(location);
        int textureY = location[1];
        Bitmap tempBitmap = textureView.getBitmap();
        Log.d(TAG, "classify: bitmap height "+tempBitmap.getHeight()+ " width "+tempBitmap.getWidth());
        Log.d(TAG, "classify: frame height "+frameLayout.getHeight()+ " width "+frameLayout.getWidth());
        Log.d(TAG, "classify: measured frame height "+frameLayout.getMeasuredHeight()+ " width "+frameLayout.getMeasuredWidth());
        Log.d(TAG, "classify: x and y height(y) "+y+ " width(x) "+x);
        Bitmap bitmap = Bitmap.createBitmap(textureView.getBitmap(), x,Math.abs(textureY-y),frameLayout.getWidth(), frameLayout.getHeight());
        Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, 100, 500, false);
//        imageView.setImageBitmap(resizeBitmap);

        final float result [] = securityThreadClassifier.classifyFrame(resizeBitmap);
        Log.d(TAG, "classify: ");

        float prediction = result[0];
        if(prediction < 0.5 && !isBlueDetected){
            colourIndicatorView.showBlue();
            isBlueDetected = true;
            bluePrediction = prediction;
        } else if(prediction >= 0.5 && !isGreenDetected) {
            colourIndicatorView.showGreen();
            isGreenDetected = true;
            greenPrediction = prediction;
        }

        if(isBlueDetected && isGreenDetected){
            Toast.makeText(this, "Colours Detected!", Toast.LENGTH_SHORT).show();
            nextTextView.setVisibility(View.VISIBLE);
        }
//
//        resultTextView.post(new Runnable() {
//            @Override
//            public void run() {
//                resultTextView.setText(""+result[0]);
//            }
//        });
//        timeTextView.post(new Runnable() {
//            @Override
//            public void run() {
//                timeTextView.setText(""+result[1]);
//            }
//        });
        resultTextView.setText(""+result[0]);
        timeTextView.setText(""+result[1]);

    }

    private void reset(){
        scanCount = 0;
        colourIndicatorView.reset();
        isGreenDetected = false;
        isBlueDetected = false;
        bluePrediction = 1f;
        greenPrediction = 0f;
    }

    private void startNextActivity(boolean isColourDetected){
        Intent intent = new Intent(SecurityThreadActivity.this, SecurityFeatureActivity.class);
        Log.d(TAG, "startNextActivity: "+isColourDetected);
        if(!isColourDetected){
            bluePrediction = 1;
            greenPrediction = 0;
        }
        intent.putExtra(Constants.BLUE_COLOUR_KEY, bluePrediction);
        intent.putExtra(Constants.GREEN_COLOUR_KEY, greenPrediction);
        startActivity(intent);
    }

    private Runnable periodicClassify = new Runnable() {
        @Override
        public void run() {
            classify();
//            backgroundHandler.post(periodicClassify);
        }
    };



    private static Size chooseOptimalSize(
            Size[] choices,
            int textureViewWidth,
            int textureViewHeight,
            int maxWidth,
            int maxHeight,
            Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth
                    && option.getHeight() <= maxHeight
                    && option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth && option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /** Compares two {@code Size}s based on their areas. */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

}


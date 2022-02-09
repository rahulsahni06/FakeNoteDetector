package com.sahni.rahul.fakenotedetector.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.sahni.rahul.fakenotedetector.utils.Constants;
import com.sahni.rahul.fakenotedetector.R;
import com.sahni.rahul.fakenotedetector.classifier.SecurityThreadClassifier;
import com.sahni.rahul.fakenotedetector.databinding.ActivitySecurityFeatureBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class SecurityFeatureActivity extends AppCompatActivity {

    private static final String TAG = SecurityThreadClassifier.class.getSimpleName();
    private static final int RESULT_LOAD_IMAGE = 123;
    private ActivitySecurityFeatureBinding binding;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private static final String HANDLE_THREAD_NAME = "security_features_thread";

    private CameraManager cameraManager;
    private String cameraId;
    private CameraDevice cameraDevice;
    private Size previewSize;
    private CameraCaptureSession captureSession;

    private String croppedImageUri;
    private Bitmap croppedBitmap;

    private float greenPrediction;
    private float bluePrediction;

    private TextureView.SurfaceTextureListener textureListener =
            new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    private CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                }
    };
    private CaptureRequest.Builder captureRequestBuilder;
    private File galleryFolder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_security_feature);
        Intent intent = getIntent();
        greenPrediction = intent.getFloatExtra(Constants.GREEN_COLOUR_KEY, 0f);
        bluePrediction = intent.getFloatExtra(Constants.BLUE_COLOUR_KEY, 1f);
        Log.d(TAG, "onCreate: green: "+greenPrediction+"   blue "+bluePrediction);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_security_feature);
        binding.clickFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] location = new int[2];
                binding.frameLayout.getLocationOnScreen(location);
                int x = location[0];
                int y = location[1];

                binding.textureView.getLocationOnScreen(location);
                int rootY = location[1];

                 croppedBitmap = Bitmap.createBitmap(binding.textureView.getBitmap(), x, Math.abs(rootY-y),
                        binding.frameLayout.getWidth(), binding.frameLayout.getHeight());
                 binding.croppedImageView.setImageBitmap(croppedBitmap);
                 lock();

            }
        });


        binding.clearImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlock();
            }
        });

        binding.nextImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SecurityFeatureActivity.this, SecurityFeatureClassifyActivity.class);
                intent.putExtra(Constants.IMAGE_URI_KEY, saveBitmapTemp());
                intent.putExtra(Constants.BLUE_COLOUR_KEY, bluePrediction);
                intent.putExtra(Constants.GREEN_COLOUR_KEY, greenPrediction);
                startActivity(intent);
            }
        });

        binding.galleryImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
                gallery.setType("image/*");
                startActivityForResult(gallery, RESULT_LOAD_IMAGE);
            }
        });

        createImageGallery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK){
            Uri selectedImageUri = data.getData();
            Intent intent = new Intent(this, SecurityFeatureClassifyActivity.class);
            intent.putExtra(Constants.IMAGE_URI_KEY, selectedImageUri.toString());
            startActivity(intent);

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (binding.textureView.isAvailable()) {
            openCamera(binding.textureView.getWidth(), binding.textureView.getHeight());
        } else {
            binding.textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }


    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        setUpCameraOutputs(width, height);
        try {
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        int maxPreviewWidth = displaySize.x;
        int maxPreviewHeight = displaySize.y;
        StreamConfigurationMap streamConfigurationMap = null;
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics =
                        cameraManager.getCameraCharacteristics(cameraId);

                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK ) {
                    Log.d(TAG, "setUpCamera: Back camera");
                    streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//                    previewSize = chooseOptimalSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class),
//                            maxPreviewWidth, maxPreviewHeight);

                    this.cameraId = cameraId;
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture.class)[0];
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void lock() {
        binding.clickFab.setVisibility(View.INVISIBLE);
        binding.clearImageView.setVisibility(View.VISIBLE);
        binding.nextImageView.setVisibility(View.VISIBLE);
        binding.croppedImageView.setVisibility(View.VISIBLE);

        try {
//            captureSession.capture(captureRequestBuilder.build(),
//                    null, backgroundHandler);
            captureSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlock() {
        binding.clickFab.setVisibility(View.VISIBLE);
        binding.clearImageView.setVisibility(View.INVISIBLE);
        binding.nextImageView.setVisibility(View.INVISIBLE);
        binding.croppedImageView.setVisibility(View.INVISIBLE);
        try {
            captureSession.setRepeatingRequest(captureRequestBuilder.build(),
                    null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = binding.textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);


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
                                captureSession = cameraCaptureSession;
                                captureSession.setRepeatingRequest(captureRequest,
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

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

    }

    private String saveBitmapTemp(){
        String uri = null;
        FileOutputStream outputPhoto = null;
        try {
            File imageFile = createImageFile(galleryFolder);
            outputPhoto = new FileOutputStream(imageFile);

            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);
//            MediaStore.Images.Media.insertImage(getContentResolver(), imageFile.getAbsolutePath(), imageFile.getName(), "");
            galleryAddPic(imageFile);
            uri = Uri.fromFile(imageFile).toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//                    unlock();
            try {
                if (outputPhoto != null) {
                    outputPhoto.flush();
                    outputPhoto.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uri;
    }

    private void startBackgroundThread(){
        backgroundThread = new HandlerThread(HANDLE_THREAD_NAME);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }
    }

    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp + "_";
        return File.createTempFile(imageFileName, ".jpg", galleryFolder);
    }

    private void galleryAddPic(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }



}

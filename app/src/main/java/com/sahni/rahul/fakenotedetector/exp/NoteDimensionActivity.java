package com.sahni.rahul.fakenotedetector.exp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.ux.ArFragment;
import com.sahni.rahul.fakenotedetector.R;

import java.util.Collection;

public class NoteDimensionActivity extends AppCompatActivity {

    private ArFragment arFragment;
//    private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();
    private final String TAG = NoteDimensionActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_dimension);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        arFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                Frame frame = arFragment.getArSceneView().getArFrame();
                Log.d(TAG, "onUpdate:");

                // If there is no frame or ARCore is not tracking yet, just return.
                if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                    Log.d(TAG, "onUpdate: return");

                    return;
                }

                Collection<AugmentedImage> updatedAugmentedImages =
                        frame.getUpdatedTrackables(AugmentedImage.class);
                for (AugmentedImage augmentedImage : updatedAugmentedImages) {
                    switch (augmentedImage.getTrackingState()) {
                        case PAUSED:
                            // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                            // but not yet tracked.
                            String text = "Detected Image " + augmentedImage.getName();
                            Log.d(TAG, "onUpdate: Detected image:  name = "+text+"   index"+augmentedImage.getIndex());

//                            SnackbarHelper.getInstance().showMessage(this, text);
                            break;

                        case TRACKING:
                            Log.d(TAG, "onUpdate: Tracking");
                            // Have to switch to UI Thread to update View.
//                            fitToScanView.setVisibility(View.GONE);
//
//                            // Create a new anchor for newly found images.
//                            if (!augmentedImageMap.containsKey(augmentedImage)) {
//                                AugmentedImageNode node = new AugmentedImageNode(this);
//                                node.setImage(augmentedImage);
//                                augmentedImageMap.put(augmentedImage, node);
//                                arFragment.getArSceneView().getScene().addChild(node);
//                            }
                            break;

                        case STOPPED:
//                            augmentedImageMap.remove(augmentedImage);
                            Log.d(TAG, "onUpdate: Stopped");
                            break;
                    }
                }
            }

        });
    }
}

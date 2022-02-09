package com.sahni.rahul.fakenotedetector.exp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.sahni.rahul.fakenotedetector.R;
import com.sahni.rahul.fakenotedetector.classifier.SecurityThreadClassifier;
import com.sahni.rahul.fakenotedetector.ui.ColourIndicatorView;

import java.io.IOException;

public class ImageActivity extends AppCompatActivity {

    private static final String TAG = ImageActivity.class.getSimpleName();
    private SecurityThreadClassifier securityThreadClassifier;
    private ColourIndicatorView colourIndicatorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            Log.d(TAG, "onCreate: Initializing classifier");
            securityThreadClassifier = new SecurityThreadClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "onCreate: Failed to load model!");
            Toast.makeText(this, "Failed to load Model!",Toast.LENGTH_SHORT).show();
//            finish();
        }

        colourIndicatorView = findViewById(R.id.colour_indicator_view);
        colourIndicatorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        colourIndicatorView.showBlue();
                        colourIndicatorView.showGreen();
                        Log.d(TAG, "onTouch:ACTION_DOWN ");
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        colourIndicatorView.reset();
                        Log.d(TAG, "onTouch:ACTION_UP ");
                        return false;
                }
                return true;
            }
        });


    }

}

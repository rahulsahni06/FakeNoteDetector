package com.sahni.rahul.fakenotedetector.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.sahni.rahul.fakenotedetector.classifier.SecurityFeatureClassifier;
import com.sahni.rahul.fakenotedetector.utils.Constants;
import com.sahni.rahul.fakenotedetector.utils.FileSaver;
import com.sahni.rahul.fakenotedetector.R;
import com.sahni.rahul.fakenotedetector.databinding.ActivityClassifyBinding;

import java.io.IOException;

public class SecurityFeatureClassifyActivity extends AppCompatActivity {

    private static final String TAG = SecurityFeatureClassifyActivity.class.getSimpleName();
    private SecurityFeatureClassifier securityFeatureClassifier;
    private ActivityClassifyBinding binding;
    private float greenPrediction;
    private float bluePrediction;

    private FileSaver fileSaver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate:");
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        greenPrediction = intent.getFloatExtra(Constants.GREEN_COLOUR_KEY, 0f);
        bluePrediction = intent.getFloatExtra(Constants.BLUE_COLOUR_KEY, 1f);
        String imageUriString = intent.getStringExtra(Constants.IMAGE_URI_KEY);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_classify);
        setSupportActionBar(binding.toolbar);
//        binding.toolbar.setTitle("");

        if(greenPrediction >= 0.5 && bluePrediction <= 0.5){
            binding.includeContentClassify.colourImageView.setImageResource(R.drawable.ic_check_black_24dp);
            binding.includeContentClassify.colourTextView.setText("Both colours detected!");
            binding.includeContentClassify.colourTextView.setTextColor(ColorStateList.valueOf(
                    getResources().getColor(R.color.colorPrimaryDark)
            ));
        } else {
            binding.includeContentClassify.colourImageView.setImageResource(R.drawable.ic_error_black_24dp);
            binding.includeContentClassify.colourTextView.setText("Colours not detected!");
            binding.includeContentClassify.colourTextView.setTextColor(ColorStateList.valueOf(
                    getResources().getColor(android.R.color.holo_red_dark)
            ));
        }

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle("");
        }

        fileSaver = new FileSaver(this, true);
        try {
            securityFeatureClassifier = new SecurityFeatureClassifier(this);
            classifyAsyncTask.execute(imageUriString);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "onCreate: Failed to load model!");
            Toast.makeText(this, "Failed to load Model!",Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private AsyncTask<String, Void, Float> classifyAsyncTask = new AsyncTask<String, Void, Float>(){

        @Override
        protected Float doInBackground(String... uriStrings) {
            float prediction = 0;
            try {
//                if(bitmap!= null){
//                    bitmap.recycle();
//                }
//                bitmap.recycle();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                        Uri.parse(uriStrings[0]));
                binding.includeContentClassify.imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.includeContentClassify.imageView.setImageURI(Uri.parse(uriStrings[0]));
                    }
                });

                if(bitmap.getHeight() != 299 || bitmap.getWidth()!= 299) {

                    final Bitmap resizedBitmap = scaleAndAddWhiteBorder(bitmap);
                    final Uri uri = fileSaver.saveBitmapTemp(resizedBitmap);
                    prediction = securityFeatureClassifier.classifyFrame(resizedBitmap);
                    if (uri == null) {
                        Log.d(TAG, "doInBackground: Image not saved!");
                    } else {
                        Log.d(TAG, "doInBackground: Image saved! uri: " + uri.toString());
                    }
                } else {
                    prediction = securityFeatureClassifier.classifyFrame(bitmap);
                }


//                Bitmap resizedBitmap = scaleAndAddWhiteBorder(bitmap);


//                bitmap.recycle();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "doInBackground: Can't predict as failed to convert string to uri!");
            }
            return prediction;
        }

        @Override
        protected void onPostExecute(Float aFloat) {
            binding.includeContentClassify.progressBar.setVisibility(View.INVISIBLE);
            binding.includeContentClassify.resultImageView.setVisibility(View.VISIBLE);

            float finalResult =
                    getFinalResult(aFloat);
            Log.d(TAG, "onPostExecute: classify result: "+aFloat);
            Log.d(TAG, "onPostExecute: finalResult: "+finalResult);
            if(finalResult >= 0.5){
                int color = getResources().getColor(R.color.colorPrimaryDark);

                binding.includeContentClassify.resultImageView.setImageResource(R.drawable.ic_check_black_24dp);
//                binding.includeContentClassify.resultImageView.setImageTintList(ColorStateList.
//                        valueOf(color));
                binding.includeContentClassify.resultTextView.setTextColor(ColorStateList.
                        valueOf(color));
                binding.includeContentClassify.resultTextView.setText(String.format("%.1f", finalResult*100)+"% Genuine");

            } else {
                int color = getResources().getColor(android.R.color.holo_red_dark);

                binding.includeContentClassify.resultImageView.setImageResource(R.drawable.ic_error_black_24dp);
                binding.includeContentClassify.resultTextView.setTextColor(ColorStateList.
                        valueOf(color));
                binding.includeContentClassify.resultTextView.setText(String.format("%.1f", (1-finalResult)*100)+"% Fake");

            }

        }
    };


    private Bitmap scaleAndAddWhiteBorder(Bitmap bmp){
        int height = bmp.getHeight();
        int width = bmp.getWidth();
        int biggerSide = height >= width ? height:width;

        Bitmap bmpWithBorder = Bitmap.createBitmap(biggerSide , biggerSide, bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        canvas.drawBitmap(bmp, (biggerSide-width)/2f, (biggerSide-height)/2f, paint);

        bmpWithBorder = Bitmap.createScaledBitmap(bmpWithBorder, SecurityFeatureClassifier.DIM_IMG_SIZE_X,
                SecurityFeatureClassifier.DIM_IMG_SIZE_Y, true);
        return bmpWithBorder;
    }

    private float getFinalResult(float prediction){
        return (float) (0.25*(1-bluePrediction) + 0.25*greenPrediction + 0.5*prediction);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy:");
        securityFeatureClassifier.close();
    }
}

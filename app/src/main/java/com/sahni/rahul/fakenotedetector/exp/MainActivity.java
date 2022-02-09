package com.sahni.rahul.fakenotedetector.exp;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.sahni.rahul.fakenotedetector.R;
import com.sahni.rahul.fakenotedetector.activity.SecurityFeatureClassifyActivity;
import com.sahni.rahul.fakenotedetector.databinding.ActivityMainBinding;
import com.sahni.rahul.fakenotedetector.utils.Constants;

public class MainActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setSupportActionBar(binding.toolbar);
        binding.includeContentMain.galleryImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
                gallery.setType("image/*");
                startActivityForResult(gallery, RESULT_LOAD_IMAGE);
            }
        });

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

}

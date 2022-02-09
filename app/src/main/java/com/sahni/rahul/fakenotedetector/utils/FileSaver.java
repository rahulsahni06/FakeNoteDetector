package com.sahni.rahul.fakenotedetector.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.sahni.rahul.fakenotedetector.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileSaver {

    private Activity activity;
    private File galleryFolder;

    public FileSaver(Activity activity, boolean isSaveInGallery) {
        this.activity = activity;
        if(isSaveInGallery) {
            createImageGallery();
        }
    }

    private void createImageGallery() {
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        galleryFolder = new File(storageDirectory, activity.getResources().getString(R.string.app_name));
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }
    }

    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "resized_image_" + timeStamp + "_";
        return File.createTempFile(imageFileName, ".jpg", galleryFolder);
    }


    public Uri saveBitmapTemp(Bitmap bitmap){
        Uri uri = null;
        FileOutputStream outputPhoto = null;
        try {
            File imageFile = createImageFile(galleryFolder);
            outputPhoto = new FileOutputStream(imageFile);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);
//            MediaStore.Images.Media.insertImage(getContentResolver(), imageFile.getAbsolutePath(), imageFile.getName(), "");
            galleryAddPic(imageFile);
            uri = Uri.fromFile(imageFile);
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

    private void galleryAddPic(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        activity.sendBroadcast(mediaScanIntent);
    }
}

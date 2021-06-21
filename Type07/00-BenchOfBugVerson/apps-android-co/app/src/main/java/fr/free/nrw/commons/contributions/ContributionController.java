package fr.free.nrw.commons.contributions;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import fr.free.nrw.commons.upload.ShareActivity;
import fr.free.nrw.commons.upload.UploadService;
import timber.log.Timber;

public class ContributionController {
    private Fragment fragment;
    private Activity activity;

    private final static int SELECT_FROM_GALLERY = 1;
    private final static int SELECT_FROM_CAMERA = 2;

    public ContributionController(Fragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
    }

    // See http://stackoverflow.com/a/5054673/17865 for why this is done
    private Uri lastGeneratedCaptureURI;

    private Uri reGenerateImageCaptureURI() {
        String storageState = Environment.getExternalStorageState();
        if(storageState.equals(Environment.MEDIA_MOUNTED)) {

            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Commons/images/" + new Date().getTime() + ".jpg";
            File _photoFile = new File(path);
            try {
                if(!_photoFile.exists()) {
                    _photoFile.getParentFile().mkdirs();
                    _photoFile.createNewFile();
                }

            } catch (IOException e) {
                Timber.e(e, "Could not create file: %s", path);
            }

            return Uri.fromFile(_photoFile);
        }   else {
            throw new RuntimeException("No external storage found!");
        }
    }

    public void startCameraCapture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        lastGeneratedCaptureURI = reGenerateImageCaptureURI();
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, lastGeneratedCaptureURI);
        fragment.startActivityForResult(takePictureIntent, SELECT_FROM_CAMERA);
    }

    public void startGalleryPick() {
        //FIXME: Starts gallery (opens Google Photos)
        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickImageIntent.setType("image/*");
        fragment.startActivityForResult(pickImageIntent, SELECT_FROM_GALLERY);
    }

    public void handleImagePicked(int requestCode, Intent data) {
        Intent shareIntent = new Intent(activity, ShareActivity.class);
        shareIntent.setAction(Intent.ACTION_SEND);
        switch(requestCode) {
            case SELECT_FROM_GALLERY:
                //Handles image picked from gallery
                Uri imageData = data.getData();
                shareIntent.setType(activity.getContentResolver().getType(imageData));
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageData);
                shareIntent.putExtra(UploadService.EXTRA_SOURCE, Contribution.SOURCE_GALLERY);
                break;
            case SELECT_FROM_CAMERA:
                shareIntent.setType("image/jpeg"); //FIXME: Find out appropriate mime type
                shareIntent.putExtra(Intent.EXTRA_STREAM, lastGeneratedCaptureURI);
                shareIntent.putExtra(UploadService.EXTRA_SOURCE, Contribution.SOURCE_CAMERA);
                break;
        }
        Timber.i("Image selected");
        try {
            activity.startActivity(shareIntent);
        } catch (SecurityException e) {
            Timber.e(e, "Security Exception");
        }
    }

    public void saveState(Bundle outState) {
        outState.putParcelable("lastGeneratedCaptureURI", lastGeneratedCaptureURI);
    }

    public void loadState(Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            lastGeneratedCaptureURI = savedInstanceState.getParcelable("lastGeneratedCaptureURI");
        }
    }

}

package fr.free.nrw.commons.upload;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.EventLog;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.category.CategorizationFragment;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.modifications.CategoryModifier;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.modifications.TemplateRemoveModifier;
import timber.log.Timber;

/**
 * Activity for the title/desc screen after image is selected. Also starts processing image
 * GPS coordinates or user location (if enabled in Settings) for category suggestions.
 */
public  class       ShareActivity
        extends     AuthenticatedActivity
        implements  SingleUploadFragment.OnUploadActionInitiated,
        CategorizationFragment.OnCategoriesSaveHandler {

    private CategorizationFragment categorizationFragment;

    private CommonsApplication app;

    private String source;
    private String mimeType;
    private String mediaUriString;

    private Uri mediaUri;
    private Contribution contribution;
    private SimpleDraweeView backgroundImageView;

    private UploadController uploadController;

    private boolean cacheFound;

    private GPSExtractor imageObj;
    private String decimalCoords;

    private boolean useNewPermissions = false;
    private boolean storagePermission = false;
    private boolean locationPermission = false;

    private String title;
    private String description;
    private Snackbar snackbar;

    /**
     * Called when user taps the submit button
     */
    @Override
    public void uploadActionInitiated(String title, String description) {

        this.title = title;
        this.description = description;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Check for Storage permission that is required for upload. Do not allow user to proceed without permission, otherwise will crash
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //See http://stackoverflow.com/questions/33169455/onrequestpermissionsresult-not-being-called-in-dialog-fragment
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 4);
            } else {
                uploadBegins();
            }
        } else {
            uploadBegins();
        }
    }

    private void uploadBegins() {
        if (locationPermission) {
            getFileMetadata(true);
        } else {
            getFileMetadata(false);
        }

        Toast startingToast = Toast.makeText(
                CommonsApplication.getInstance(),
                R.string.uploading_started,
                Toast.LENGTH_LONG
        );
        startingToast.show();

        if (!cacheFound) {
            //Has to be called after apiCall.request()
            app.getCacheData().cacheCategory();
            Timber.d("Cache the categories found");
        }

        uploadController.startUpload(title, mediaUri, description, mimeType, source, decimalCoords, new UploadController.ContributionUploadProgress() {
            @Override
            public void onUploadStarted(Contribution contribution) {
                ShareActivity.this.contribution = contribution;
                showPostUpload();
            }
        });
    }

    private void showPostUpload() {
        if(categorizationFragment == null) {
            categorizationFragment = new CategorizationFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.single_upload_fragment_container, categorizationFragment, "categorization")
                .commit();
    }

    @Override
    public void onCategoriesSave(ArrayList<String> categories) {
        if(categories.size() > 0) {
            ModifierSequence categoriesSequence = new ModifierSequence(contribution.getContentUri());

            categoriesSequence.queueModifier(new CategoryModifier(categories.toArray(new String[]{})));
            categoriesSequence.queueModifier(new TemplateRemoveModifier("Uncategorized"));
            categoriesSequence.setContentProviderClient(getContentResolver().acquireContentProviderClient(ModificationsContentProvider.AUTHORITY));
            categoriesSequence.save();
        }

        // FIXME: Make sure that the content provider is up
        // This is the wrong place for it, but bleh - better than not having it turned on by default for people who don't go throughl ogin
        ContentResolver.setSyncAutomatically(app.getCurrentAccount(), ModificationsContentProvider.AUTHORITY, true); // Enable sync by default!

        EventLog.schema(CommonsApplication.EVENT_CATEGORIZATION_ATTEMPT)
                .param("username", app.getCurrentAccount().name)
                .param("categories-count", categories.size())
                .param("files-count", 1)
                .param("source", contribution.getSource())
                .param("result", "queued")
                .log();
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(contribution != null) {
            outState.putParcelable("contribution", contribution);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(categorizationFragment != null && categorizationFragment.isVisible()) {
            EventLog.schema(CommonsApplication.EVENT_CATEGORIZATION_ATTEMPT)
                    .param("username", app.getCurrentAccount().name)
                    .param("categories-count", categorizationFragment.getCurrentSelectedCount())
                    .param("files-count", 1)
                    .param("source", contribution.getSource())
                    .param("result", "cancelled")
                    .log();
        } else {
            EventLog.schema(CommonsApplication.EVENT_UPLOAD_ATTEMPT)
                    .param("username", app.getCurrentAccount().name)
                    .param("source", getIntent().getStringExtra(UploadService.EXTRA_SOURCE))
                    .param("multiple", true)
                    .param("result", "cancelled")
                    .log();
        }
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        app.getMWApi().setAuthCookie(authCookie);

        SingleUploadFragment shareView = (SingleUploadFragment) getSupportFragmentManager().findFragmentByTag("shareView");
        categorizationFragment = (CategorizationFragment) getSupportFragmentManager().findFragmentByTag("categorization");
        if(shareView == null && categorizationFragment == null) {
            shareView = new SingleUploadFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.single_upload_fragment_container, shareView, "shareView")
                    .commitAllowingStateLoss();
        }
        uploadController.prepareService();
    }

    @Override
    protected void onAuthFailure() {
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uploadController = new UploadController();
        setContentView(R.layout.activity_share);
        ButterKnife.bind(this);
        initBack();
        app = CommonsApplication.getInstance();
        backgroundImageView = (SimpleDraweeView)findViewById(R.id.backgroundImage);
        backgroundImageView.setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp, getTheme()))
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_error_outline_black_24dp, getTheme()))
                .build());

        //Receive intent from ContributionController.java when user selects picture to upload
        Intent intent = getIntent();

        if(intent.getAction().equals(Intent.ACTION_SEND)) {
            mediaUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if(intent.hasExtra(UploadService.EXTRA_SOURCE)) {
                source = intent.getStringExtra(UploadService.EXTRA_SOURCE);
            } else {
                source = Contribution.SOURCE_EXTERNAL;
            }
            mimeType = intent.getType();
        }

        if (mediaUri != null) {
            mediaUriString = mediaUri.toString();
            backgroundImageView.setImageURI(mediaUriString);

            //Test SHA1 of image to see if it matches SHA1 of a file on Commons
            try {
                InputStream inputStream = getContentResolver().openInputStream(mediaUri);
                Timber.d("Input stream created from %s", mediaUriString);
                String fileSHA1 = Utils.getSHA1(inputStream);
                Timber.d("File SHA1 is: %s", fileSHA1);

                ExistingFileAsync fileAsyncTask = new ExistingFileAsync(fileSHA1, this);
                fileAsyncTask.execute();

            } catch (IOException e) {
                Timber.d(e, "IO Exception: ");
            }
        }

        if(savedInstanceState != null)  {
            contribution = savedInstanceState.getParcelable("contribution");
        }

        requestAuthToken();

        Timber.d("Uri: %s", mediaUriString);
        Timber.d("Ext storage dir: %s", Environment.getExternalStorageDirectory());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            useNewPermissions = true;
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                storagePermission = true;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermission = true;
            }
        }

        // Check storage permissions if marshmallow or newer
        if (useNewPermissions && (!storagePermission || !locationPermission)) {
            if (!storagePermission && !locationPermission) {
                String permissionRationales = getResources().getString(R.string.storage_permission_rationale) + "\n" + getResources().getString(R.string.location_permission_rationale);
                snackbar = Snackbar.make(findViewById(android.R.id.content), permissionRationales,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(ShareActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 3);
                            }
                        });
                snackbar.show();
                View snackbarView = snackbar.getView();
                TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setMaxLines(3);
            } else if (!storagePermission) {
                Snackbar.make(findViewById(android.R.id.content), R.string.storage_permission_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(ShareActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                            }
                        }).show();
            } else if (!locationPermission) {
                Snackbar.make(findViewById(android.R.id.content), R.string.location_permission_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(ShareActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
                            }
                        }).show();
            }
        } else if (useNewPermissions && storagePermission && !locationPermission) {
            getFileMetadata(true);
        } else if(!useNewPermissions || (storagePermission && locationPermission)) {
            getFileMetadata(true);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            // 1 = Storage (from snackbar)
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getFileMetadata(true);
                }
                return;
            }
            // 2 = Location
            case 2: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getFileMetadata(false);
                }
                return;
            }
            // 3 = Storage + Location
            case 3: {
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getFileMetadata(true);
                }
                if (grantResults.length > 1
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    getFileMetadata(false);
                }
                return;
            }
            // 4 = Storage (from submit button) - this needs to be separate from (1) because only the
            // submit button should bring user to next screen
            case 4: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //It is OK to call this at both (1) and (4) because if perm had been granted at
                    //snackbar, user should not be prompted at submit button
                    getFileMetadata(true);

                    //Uploading only begins if storage permission granted from arrow icon
                    uploadBegins();
                    snackbar.dismiss();
                }
                return;
            }
        }
    }

    /**
     * Gets coordinates for category suggestions, either from EXIF data or user location
     * @param gpsEnabled
     */
    public void getFileMetadata(boolean gpsEnabled) {
        String filePath = FileUtils.getPath(getApplicationContext(), mediaUri);
        Timber.d("Filepath: %s", filePath);
        Timber.d("Calling GPSExtractor");
        if(imageObj == null) {
            imageObj = new GPSExtractor(filePath);
        }

        if (filePath != null && !filePath.equals("")) {
            // Gets image coords from exif data or user location
            decimalCoords = imageObj.getCoords(gpsEnabled);
            useImageCoords();
        }
    }

    /**
     * Initiates retrieval of image coordinates or user coordinates, and caching of coordinates.
     * Then initiates the calls to MediaWiki API through an instance of MwVolleyApi.
     */
    public void useImageCoords() {
        if(decimalCoords != null) {
            Timber.d("Decimal coords of image: %s", decimalCoords);

            // Only set cache for this point if image has coords
            if (imageObj.imageCoordsExists) {
                double decLongitude = imageObj.getDecLongitude();
                double decLatitude = imageObj.getDecLatitude();
                app.getCacheData().setQtPoint(decLongitude, decLatitude);
            }

            MwVolleyApi apiCall = new MwVolleyApi();

            List<String> displayCatList = app.getCacheData().findCategory();
            boolean catListEmpty = displayCatList.isEmpty();

            // If no categories found in cache, call MediaWiki API to match image coords with nearby Commons categories
            if (catListEmpty) {
                cacheFound = false;
                apiCall.request(decimalCoords);
                Timber.d("displayCatList size 0, calling MWAPI %s", displayCatList);
            } else {
                cacheFound = true;
                Timber.d("Cache found, setting categoryList in MwVolleyApi to %s", displayCatList);
                MwVolleyApi.setGpsCat(displayCatList);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            imageObj.unregisterLocationManager();
            Timber.d("Unregistered locationManager");
        }
        catch (NullPointerException e) {
            Timber.d("locationManager does not exist, not unregistered");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uploadController.cleanup();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(categorizationFragment!=null && categorizationFragment.isVisible()) {
                    categorizationFragment.backButtonDialog();
                } else {
                    onBackPressed();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

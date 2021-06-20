package io.evercam.androidapp;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.logentries.android.AndroidLogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import io.evercam.androidapp.authentication.EvercamAccount;
import io.evercam.androidapp.custom.CameraLayout;
import io.evercam.androidapp.custom.CustomProgressDialog;
import io.evercam.androidapp.custom.CustomSnackbar;
import io.evercam.androidapp.custom.CustomedDialog;
import io.evercam.androidapp.dto.AppData;
import io.evercam.androidapp.dto.AppUser;
import io.evercam.androidapp.dto.EvercamCamera;
import io.evercam.androidapp.dto.ImageLoadingStatus;
import io.evercam.androidapp.feedback.KeenHelper;
import io.evercam.androidapp.feedback.LoadTimeFeedbackItem;
import io.evercam.androidapp.tasks.CheckInternetTask;
import io.evercam.androidapp.tasks.LoadCameraListTask;
import io.evercam.androidapp.utils.Commons;
import io.evercam.androidapp.utils.Constants;
import io.evercam.androidapp.utils.DataCollector;
import io.evercam.androidapp.utils.PrefsManager;
import io.evercam.androidapp.utils.PropertyReader;
import io.keen.client.java.KeenClient;

public class CamerasActivity extends ParentAppCompatActivity implements
        ObservableScrollViewCallbacks, OnClickListener
{
    public static CamerasActivity activity = null;
    public MenuItem refresh;

    private static final String TAG = "CamerasActivity";

    public static int camerasPerRow = 2;
    public boolean reloadCameraList = false;

    public CustomProgressDialog reloadProgressDialog;
    private RelativeLayout actionButtonLayout;
    private int lastScrollY;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private FrameLayout mNavAccountItemLayout;
    private FrameLayout mNavSettingsItemLayout;
    private FrameLayout mNavFeedbackItemLayout;
    private FrameLayout mNavAboutItemLayout;
    private FrameLayout mNavScanLayout;
    private FrameLayout mNavLogoutLayout;
    private TextView mUserNameTextView;
    private TextView mUserEmailTextView;
    private TextView mAppVersionTextView;

    /**
     * For user data collection, calculate how long it takes to load camera list
     */
    private Date startTime;
    private float databaseLoadTime = 0;
    private AndroidLogger logger;
    private KeenClient client;

    private enum InternetCheckType
    {
        START, RESTART
    }

    private String usernameOnStop = "";
    private boolean showOfflineOnStop;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.navigation_drawer_layout);

        checkUser();

        setUpGradientToolbarWithHomeButton();
        initNavigationDrawer();

        ObservableScrollView observableScrollView = (ObservableScrollView) findViewById(R.id.cameras_scroll_view);
        observableScrollView.setScrollViewCallbacks(this);

        setUpActionButtons();

        initDataCollectionObjects();

        activity = this;

        /**
         * Use Handler here because we want the title bar/menu get loaded first.
         * When the app starts, it will load cameras to grid view twice:
         * 1. Load cameras that saved locally without image (disabled load image from cache
         * because it blocks UI.)
         * 2. When camera list returned from Evercam, show them on screen with thumbnails,
         * then request for snapshots in background separately.
         *
         * TODO: Check is it really necessary to keep the post delay handler here
         * See if refresh icon stop animating or not.
         */
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                /**
                 * Sometimes Evercam returns the list less than 0.1 sec?
                 * so check it's returned or not before
                 * the first load to avoid loading it twice.
                 */
                io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom.FlowLayout) CamerasActivity.this.findViewById(R.id.cameras_flow_layout);
                if(!(camsLineView.getChildCount() > 0))
                {
                    addAllCameraViews(false, false);
                    if(camsLineView.getChildCount() > 0 && databaseLoadTime == 0 && startTime != null)
                    {
                        databaseLoadTime = Commons.calculateTimeDifferenceFrom(startTime);
                    }
                }
            }
        }, 1);

        // Start loading camera list after menu created(because need the menu
        // showing as animation)
        new CamerasCheckInternetTask(CamerasActivity.this, InternetCheckType.START).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // draw the options defined in the following file
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.camera_list_menu, menu);

        refresh = menu.findItem(R.id.menurefresh);
        refresh.setActionView(R.layout.actionbar_indeterminate_progress);

        return true;
    }

    // Tells that the item has been selected from the menu. Now check and get
    // the selected item and perform the relevant action
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();
        if(itemId == R.id.menurefresh)
        {
            EvercamPlayApplication.sendEventAnalytics(this, R.string.category_menu, R.string.action_refresh, R.string.label_list_refresh);

            if(refresh != null) refresh.setActionView(R.layout.actionbar_indeterminate_progress);

            startCameraLoadingTask();

        }
        else
        {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onRestart()
    {
        super.onRestart();

        if(MainActivity.isUserLogged(this))
        {
            //Reload camera list if default user has been changed, or offline settings has been changed
            if(isUserChanged() || isOfflineSettingChanged())
            {
                new CamerasCheckInternetTask(CamerasActivity.this, InternetCheckType.START).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else
            {
                try
                {
                    new CamerasCheckInternetTask(CamerasActivity.this, InternetCheckType.RESTART).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                }
                catch(RejectedExecutionException e)
                {
                    EvercamPlayApplication.sendCaughtExceptionNotImportant(activity, e);
                }
            } usernameOnStop = "";
        }
        else
        {
            startActivity(new Intent(this, SlideActivity.class));
            finish();
        }
    }

    private boolean isUserChanged()
    {
        String restartedUsername = AppData.defaultUser.getUsername();
        return !usernameOnStop.isEmpty() && !usernameOnStop.equals(restartedUsername);
    }

    private boolean isOfflineSettingChanged()
    {
        return showOfflineOnStop != PrefsManager.showOfflineCameras(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.e(TAG, "onActivityResult " +requestCode + " " + resultCode);
        if(requestCode == Constants.REQUEST_CODE_ADD_CAMERA)
        {
            reloadCameraList = (resultCode == Constants.RESULT_TRUE);
        }
        else if (requestCode == Constants.REQUEST_CODE_DELETE_CAMERA)
        {
            // Don't reset reload variable to false because it's possible set to TRUE when
            // return from shortcut live view
            if(resultCode == Constants.RESULT_TRUE)
            {
                reloadCameraList = true;
            }
        }
        else if(requestCode == Constants.REQUEST_CODE_MANAGE_ACCOUNT)
        {
            reloadCameraList = (resultCode == Constants.RESULT_ACCOUNT_CHANGED);
        }

        if(resultCode == Constants.RESULT_TRANSFERRED)
        {
            reloadCameraList = true;
            CustomSnackbar.showMultiLine(activity, R.string.msg_transfer_success);
        }
        else if(resultCode == Constants.RESULT_ACCESS_REMOVED)
        {
            reloadCameraList = true;
            CustomSnackbar.show(activity, R.string.msg_share_updated);
        }
        else if(resultCode == Constants.RESULT_NO_ACCESS)
        {
            reloadCameraList = true;
            CustomSnackbar.showMultiLine(activity, R.string.msg_no_access);
        }
    }

    private void startLoadingCameras()
    {
        reloadProgressDialog = new CustomProgressDialog(this);
        if(reloadCameraList)
        {
            reloadProgressDialog.show(getString(R.string.loading_cameras));
        }

        startCameraLoadingTask();
    }

    private void checkUser()
    {
        if(AppData.defaultUser == null)
        {
            AppData.defaultUser = new EvercamAccount(this).getDefaultUser();
        }
    }

    private void startCameraLoadingTask()
    {
        if(Commons.isOnline(this))
        {
            LoadCameraListTask loadTask = new LoadCameraListTask(AppData.defaultUser,
                    CamerasActivity.this);
            loadTask.reload = true; // be default do not refresh until there
            // is
            // any change in cameras in database
            loadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else
        {
            CustomedDialog.showInternetNotConnectDialog(CamerasActivity.this);
        }
    }

    // Stop All Camera Views
    public void stopAllCameraViews()
    {
        io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom
                .FlowLayout) this.findViewById(R.id.cameras_flow_layout);
        for(int count = 0; count < camsLineView.getChildCount(); count++)
        {
            LinearLayout linearLayout = (LinearLayout) camsLineView.getChildAt(count);
            CameraLayout cameraLayout = (CameraLayout) linearLayout.getChildAt(0);
            cameraLayout.stopAllActivity();
        }
    }

    private void updateNavDrawerUserInfo()
    {
        AppUser defaultUser = AppData.defaultUser;
        if(defaultUser != null)
        {
            mUserNameTextView.setText(defaultUser.getFirstName() + " " + defaultUser.getLastName());
            mUserEmailTextView.setText(defaultUser.getEmail());
        }
    }

    private void initNavigationDrawer()
    {
        mNavAccountItemLayout = (FrameLayout) findViewById(R.id.navigation_drawer_items_account_layout);
        mNavSettingsItemLayout = (FrameLayout) findViewById(R.id.navigation_drawer_items_settings_layout);
        mNavFeedbackItemLayout = (FrameLayout) findViewById(R.id.navigation_drawer_items_feedback_layout);
        mNavAboutItemLayout = (FrameLayout) findViewById(R.id.navigation_drawer_items_about_layout);
        mNavScanLayout = (FrameLayout) findViewById(R.id.navigation_drawer_items_scan_layout);
        mNavLogoutLayout = (FrameLayout) findViewById(R.id.navigation_drawer_items_logout_layout);

        mUserNameTextView = (TextView) findViewById(R.id.navigation_drawer_title_user_name);
        mUserEmailTextView = (TextView) findViewById(R.id.navigation_drawer_title_user_email);
        mAppVersionTextView = (TextView) findViewById(R.id.navigation_drawer_version_text_view);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                mToolbar,
                R.string.navigation_drawer_opened,
                R.string.navigation_drawer_closed
        )
        {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                // Disables the burger/arrow animation by default
                super.onDrawerSlide(drawerView, 0);
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        // Nav Drawer item click listener
        mNavAccountItemLayout.setOnClickListener(this);
        mNavSettingsItemLayout.setOnClickListener(this);
        mNavFeedbackItemLayout.setOnClickListener(this);
        mNavAboutItemLayout.setOnClickListener(this);
        mNavScanLayout.setOnClickListener(this);
        mNavLogoutLayout.setOnClickListener(this);
        mAppVersionTextView.setText("v" + new DataCollector(this).getAppVersion());
    }

    @Override
    public void onClick(View view)
    {
        //Close the navigation drawer, currently all click listeners are drawer items
        mDrawerLayout.closeDrawer(GravityCompat.START);

        if(view == mNavAccountItemLayout)
        {
            EvercamPlayApplication.sendEventAnalytics(this, R.string.category_menu, R.string.action_manage_account, R.string.label_account);

            startActivityForResult(new Intent(CamerasActivity.this, ManageAccountsActivity.class), Constants.REQUEST_CODE_MANAGE_ACCOUNT);
        }
        else if(view == mNavSettingsItemLayout)
        {
            EvercamPlayApplication.sendEventAnalytics(this, R.string.category_menu, R.string.action_settings, R.string.label_settings);

            startActivity(new Intent(CamerasActivity.this, CameraPrefsActivity.class));
        }
        else if(view == mNavFeedbackItemLayout)
        {
            startActivity(new Intent(CamerasActivity.this, FeedbackActivity.class));
        }
        else if(view == mNavAboutItemLayout)
        {
            Intent aboutIntent = new Intent(CamerasActivity.this, SimpleWebActivity.class);
            aboutIntent.putExtra(Constants.BUNDLE_KEY_URL, getString(R.string.evercam_url));
            startActivity(aboutIntent);
        }
        else if(view == mNavScanLayout)
        {
            startActivityForResult(new Intent(CamerasActivity.this, ScanActivity.class),
                    Constants.REQUEST_CODE_ADD_CAMERA);
        }
        else if(view == mNavLogoutLayout)
        {
            showSignOutDialog();
        }
    }

    private void setUpActionButtons()
    {
        actionButtonLayout = (RelativeLayout) findViewById(R.id
                .action_button_layout);
        final FloatingActionsMenu actionMenu = (FloatingActionsMenu) findViewById(R.id.add_action_menu);
        final FloatingActionButton manuallyAddButton = (FloatingActionButton) findViewById(R.id.add_action_button_manually);
        final FloatingActionButton scanButton = (FloatingActionButton) findViewById(R.id.add_action_button_scan);

        actionMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu
                .OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded()
            {
               dimBackgroundAsAnimation(actionButtonLayout);

                actionButtonLayout.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v)
                    {
                        actionMenu.collapse();
                    }
                });
            }

            @Override
            public void onMenuCollapsed()
            {
                actionButtonLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                actionButtonLayout.setOnClickListener(null);
                actionButtonLayout.setClickable(false);
            }
        });

        manuallyAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                EvercamPlayApplication.sendEventAnalytics(CamerasActivity.this, R.string
                        .category_menu, R.string.action_add_camera, R.string
                        .label_add_camera_manually);

                startActivityForResult(new Intent(CamerasActivity.this, AddEditCameraActivity
                        .class), Constants.REQUEST_CODE_ADD_CAMERA);

                actionMenu.collapse();
            }
        });
        scanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {

                EvercamPlayApplication.sendEventAnalytics(CamerasActivity.this, R.string
                        .category_menu, R.string.action_add_camera, R.string.label_add_camera_scan);

                startActivityForResult(new Intent(CamerasActivity.this, ScanActivity.class),
                        Constants.REQUEST_CODE_ADD_CAMERA);

                actionMenu.collapse();
            }
        });
    }

    private void dimBackgroundAsAnimation(final View view)
    {
        Integer colorFrom = getResources().getColor(android.R.color.transparent);
        Integer colorTo = getResources().getColor(R.color.black_semi_transparent);
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animator)
            {
                view.setBackgroundColor((Integer)animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }

    boolean resizeCameras()
    {
        try
        {
            int screen_width = readScreenWidth(this);
            camerasPerRow = recalculateCameraPerRow();

            io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom
                    .FlowLayout) this.findViewById(R.id.cameras_flow_layout);
            for(int i = 0; i < camsLineView.getChildCount(); i++)
            {
                LinearLayout pview = (LinearLayout) camsLineView.getChildAt(i);
                CameraLayout cameraLayout = (CameraLayout) pview.getChildAt(0);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(android.view
                        .ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams
                        .WRAP_CONTENT);
                params.width = ((i + 1 % camerasPerRow == 0) ? (screen_width - (i %
                        camerasPerRow) * (screen_width / camerasPerRow)) : screen_width /
                        camerasPerRow);
                params.width = params.width - 1; //1 pixels spacing between cameras
                params.height = (int) (params.width / (1.25));
                params.setMargins(1, 1, 0, 0); //1 pixels spacing between cameras
                cameraLayout.setLayoutParams(params);
            }
            return true;
        }
        catch(Exception e)
        {
            Log.e(TAG, e.toString() + "::" + Log.getStackTraceString(e));

            sendToMint(e);

            EvercamPlayApplication.sendCaughtException(this, e);
            CustomedDialog.showUnexpectedErrorDialog(CamerasActivity.this);
        }
        return false;
    }

    private void updateCameraNames()
    {
        try
        {
            io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom
                    .FlowLayout) this.findViewById(R.id.cameras_flow_layout);
            for(int i = 0; i < camsLineView.getChildCount(); i++)
            {
                LinearLayout pview = (LinearLayout) camsLineView.getChildAt(i);
                CameraLayout cameraLayout = (CameraLayout) pview.getChildAt(0);

                cameraLayout.updateTitleIfDifferent();
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, e.toString());
            EvercamPlayApplication.sendCaughtException(this, e);
        }
    }

    // Remove all the cameras so that all activities being performed can be
    // stopped
    public boolean removeAllCameraViews()
    {
        try
        {
            stopAllCameraViews();

            io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom
                    .FlowLayout) this.findViewById(R.id.cameras_flow_layout);
            camsLineView.removeAllViews();

            return true;
        }
        catch(Exception e)
        {
            Log.e(TAG, e.toString() + "::" + Log.getStackTraceString(e));

            sendToMint(e);

            EvercamPlayApplication.sendCaughtException(this, e);
            CustomedDialog.showUnexpectedErrorDialog(CamerasActivity.this);
        }
        return false;
    }

    /**
     * Add all camera views to the main grid page
     *
     * @param reloadImages   reload camera images or not
     * @param showThumbnails show thumbnails that returned by Evercam or not, if true
     *                       and if thumbnail not available, it will request latest snapshot
     *                       instead. If false,
     *                       it will request neither thumbnail nor latest snapshot.
     */
    public boolean addAllCameraViews(final boolean reloadImages, final boolean showThumbnails)
    {
        try
        {
            // Recalculate camera per row
            camerasPerRow = recalculateCameraPerRow();

            io.evercam.androidapp.custom.FlowLayout camsLineView = (io.evercam.androidapp.custom
                    .FlowLayout) this.findViewById(R.id.cameras_flow_layout);

            int screen_width = readScreenWidth(this);

            int index = 0;

            for(final EvercamCamera evercamCamera : AppData.evercamCameraList)
            {
                //Don't show offline camera
                if(!PrefsManager.showOfflineCameras(this) && !evercamCamera.isActive())
                {
                    continue;
                }

                final LinearLayout cameraListLayout = new LinearLayout(this);

                int indexPlus = index + 1;

                if(reloadImages) evercamCamera.loadingStatus = ImageLoadingStatus.not_started;

                final CameraLayout cameraLayout = new CameraLayout(this, evercamCamera,
                        showThumbnails);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(android.view
                        .ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams
                        .WRAP_CONTENT);
                params.width = ((indexPlus % camerasPerRow == 0) ? (screen_width - (index %
                        camerasPerRow) * (screen_width / camerasPerRow)) : screen_width /
                        camerasPerRow);
                params.width = params.width - 1; //1 pixels spacing between cameras
                params.height = (int) (params.width / (1.25));
                params.setMargins(0, 0, 0, 0); //No spacing between cameras
                cameraLayout.setLayoutParams(params);

                cameraListLayout.addView(cameraLayout);

                camsLineView.addView(cameraListLayout, new io.evercam.androidapp.custom
                        .FlowLayout.LayoutParams(0, 0));

                index++;

                new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {

                        Rect cameraBounds = new Rect();
                        cameraListLayout.getHitRect(cameraBounds);

                        Rect offlineIconBounds = cameraLayout.getOfflineIconBounds();
                        int layoutWidth = cameraBounds.right - cameraBounds.left;
                        int offlineStartsAt = offlineIconBounds.left;
                        int offlineIconWidth = offlineIconBounds.right - offlineIconBounds.left;

                        if(layoutWidth > offlineStartsAt + offlineIconWidth*2)
                        {
                            cameraLayout.showOfflineIconAsFloat = false;
                        }
                        else
                        {
                            cameraLayout.showOfflineIconAsFloat = true;
                        }
                    }
                }, 200);
            }

            if(refresh != null) refresh.setActionView(null);

            return true;
        }
        catch(Exception e)
        {
            Log.e(TAG, e.toString(), e);

            sendToMint(e);

            EvercamPlayApplication.sendCaughtException(this, e);
            CustomedDialog.showUnexpectedErrorDialog(CamerasActivity.this);
        }
        return false;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        activity = null;
        removeAllCameraViews();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
        resizeCameras();
    }

    @Override
    public void onStop()
    {
        super.onStop();

        if(AppData.defaultUser != null)
        {
            usernameOnStop = AppData.defaultUser.getUsername();
        }

        showOfflineOnStop = PrefsManager.showOfflineCameras(this);
    }

    public static void logOutDefaultUser(Activity activity)
    {
        getMixpanel().identifyUser(UUID.randomUUID().toString());
        new EvercamAccount(activity).remove(AppData.defaultUser.getEmail(), null);

        // clear real-time default app data
        AppData.reset();

        activity.finish();
        activity.startActivity(new Intent(activity, SlideActivity.class));
    }

    private void showSignOutDialog()
    {
        CustomedDialog.getConfirmLogoutDialog(this, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                EvercamPlayApplication.sendEventAnalytics(CamerasActivity.this,
                        R.string.category_menu, R.string.action_logout, R.string.label_user_logout);
                logOutDefaultUser(CamerasActivity.this);
            }
        }).show();
    }

    public static int readScreenWidth(Activity activity)
    {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public static int readScreenHeight(Activity activity)
    {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    /**
     * Recalculate camera per row preference for the following situations:
     * 1. If it won't influence others and the current user only has one or two cameras,
     * reset the value to be 1.
     * 2. If current user only has one or two cameras, but the device has other accounts
     * logged in, keep the value as it was without overriding it.
     * 3. If the current user has more than two cameras, adjust the value of camera per
     * row to be a proper number based on screen size.
     *
     * @return The recalculated value of camera per row
     */
    public int recalculateCameraPerRow()
    {
        int totalCameras = AppData.evercamCameraList.size();
        boolean isInfluencingOtherUser = false;
        ArrayList<AppUser> userList = new EvercamAccount(this).retrieveUserList();
        if(userList.size() > 1)
        {
            isInfluencingOtherUser = true;
        }

        if(totalCameras != 0 && totalCameras <= 2)
        {
            if(!isInfluencingOtherUser)
            {
                PrefsManager.setCameraPerRow(this, 1);
                return 1;
            }
            else
            {
                return PrefsManager.getCameraPerRow(this, 2);
            }
        }
        else
        {
            int screenWidth = readScreenWidth(this);
            int maxCamerasPerRow = 3;
            int minCamerasPerRow = 1;
            if(screenWidth != 0)
            {
                maxCamerasPerRow = screenWidth / 350;
            }

            int oldCamerasPerRow = PrefsManager.getCameraPerRow(this, 2);
            if(maxCamerasPerRow < oldCamerasPerRow && maxCamerasPerRow != 0)
            {
                PrefsManager.setCameraPerRow(this, maxCamerasPerRow);
                return maxCamerasPerRow;
            }
            else if(maxCamerasPerRow == 0)
            {
                return minCamerasPerRow;
            }
            return oldCamerasPerRow;
        }
    }

    private void initDataCollectionObjects()
    {
        startTime = new Date();
        String logentriesToken = getPropertyReader()
                .getPropertyStr(PropertyReader.KEY_LOGENTRIES_TOKEN);
        if(!logentriesToken.isEmpty())
        {
            logger = AndroidLogger.getLogger(getApplicationContext(), logentriesToken, false);
        }

        client = KeenHelper.getClient(this);
    }

    /**
     * Calculate how long it takes for the user to see the camera list
     */
    public void calculateLoadingTimeAndSend()
    {
        if(startTime != null)
        {
            float timeDifferenceFloat = Commons.calculateTimeDifferenceFrom(startTime);
            Log.d(TAG, "It takes " + databaseLoadTime + " and " + timeDifferenceFloat + " seconds" +
                    " to load camera list");
            startTime = null;

            String username = "";
            if(AppData.defaultUser != null)
            {
                username = AppData.defaultUser.getUsername();
            }
            LoadTimeFeedbackItem feedbackItem = new LoadTimeFeedbackItem(this,
                    username, databaseLoadTime, timeDifferenceFloat);
            databaseLoadTime = 0;
            sendToLogentries(logger, feedbackItem.toJson());

            feedbackItem.sendToKeenIo(client);
        }
    }

    class CamerasCheckInternetTask extends CheckInternetTask
    {
        InternetCheckType type;

        public CamerasCheckInternetTask(Context context, InternetCheckType type)
        {
            super(context);
            this.type = type;
        }

        @Override
        protected void onPostExecute(Boolean hasNetwork)
        {
            if(hasNetwork)
            {
                if(type == InternetCheckType.START)
                {
                    updateNavDrawerUserInfo();
                    startLoadingCameras();
                }
                else if(type == InternetCheckType.RESTART)
                {
                    if(reloadCameraList)
                    {
                        removeAllCameraViews();
                        startLoadingCameras();
                        reloadCameraList = false;
                    }
                    else
                    {
                        // Re-calculate camera per row because screen size
                        // could change because of screen rotation.
                        int camsOldValue = camerasPerRow;
                        camerasPerRow = recalculateCameraPerRow();
                        if(camsOldValue != camerasPerRow)
                        {
                            removeAllCameraViews();
                            addAllCameraViews(true, true);
                        }

                        // Refresh camera names in case it's changed from camera
                        // live view
                        updateCameraNames();
                    }
                }
            }
            else
            {
                CustomedDialog.showInternetNotConnectDialog(CamerasActivity.this);
            }
        }
    }

    private void showActionButtons(boolean show)
    {
        actionButtonLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * ObservableScrollView callbacks
     */
    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging)
    {
        //Log.e(TAG, "onScrollChanged: " + scrollY + " " + firstScroll + " " + dragging);
        lastScrollY = scrollY;
    }

    @Override
    public void onDownMotionEvent()
    {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState)
    {
        //Log.e(TAG, "onUpOrCancelMotionEvent: " + scrollState);
        if (scrollState == ScrollState.UP)
        {
            //Fix the bug that it's UP if swiping down when the view reaches screen top
            if(lastScrollY != 0)
            {
                if(toolbarIsShown())
                {
                    hideToolbar();
                    showActionButtons(false);
                }
            }
        }
        else if (scrollState == ScrollState.DOWN)
        {
            if (toolbarIsHidden())
            {
                showToolbar();
                showActionButtons(true);
            }
        }
    }
}

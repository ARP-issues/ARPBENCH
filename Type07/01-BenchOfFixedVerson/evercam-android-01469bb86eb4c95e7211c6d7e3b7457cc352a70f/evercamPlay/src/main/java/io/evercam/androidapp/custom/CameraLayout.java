package io.evercam.androidapp.custom;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import io.evercam.androidapp.ParentAppCompatActivity;
import io.evercam.androidapp.R;
import io.evercam.androidapp.dto.AppData;
import io.evercam.androidapp.dto.CameraStatus;
import io.evercam.androidapp.dto.EvercamCamera;
import io.evercam.androidapp.dto.ImageLoadingStatus;
import io.evercam.androidapp.video.VideoActivity;

public class CameraLayout extends LinearLayout
{
    private static final String TAG = "CameraLayout";

    public RelativeLayout cameraRelativeLayout;

    public Context context;
    public EvercamCamera evercamCamera;

    /**
     * Tells whether application has ended or not.
     * If it is true, all tasks must end and no further
     * processing should be done in any thread.
     */
    private boolean end = false;
    private ImageView snapshotImageView;
    private ImageView offlineImage = null;
    private GradientTitleLayout gradientLayout;
    public boolean showOfflineIconAsFloat = false;

    /**
     * Handler for the handling the next request. It will call the image loading
     * thread so that it can proceed with next step.
     */
    public final Handler handler = new Handler();

    public CameraLayout(final Activity activity, EvercamCamera camera, boolean showThumbnails)
    {
        super(activity.getApplicationContext());
        this.context = activity.getApplicationContext();

        try
        {
            evercamCamera = camera;

            this.setOrientation(LinearLayout.VERTICAL);
            this.setGravity(Gravity.START);
            this.setBackgroundColor(getResources().getColor(R.color.custom_light_gray));

            cameraRelativeLayout = new RelativeLayout(context);
            RelativeLayout.LayoutParams ivParams = new RelativeLayout.LayoutParams(android.view
                    .ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams
                    .MATCH_PARENT);
            cameraRelativeLayout.setLayoutParams(ivParams);

            this.addView(cameraRelativeLayout);

            snapshotImageView = new ImageView(context);
            RelativeLayout.LayoutParams imageViewParams = new RelativeLayout.LayoutParams
                    (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            snapshotImageView.setLayoutParams(imageViewParams);
            snapshotImageView.setBackgroundColor(Color.TRANSPARENT);
            snapshotImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            cameraRelativeLayout.addView(snapshotImageView);

            offlineImage = new ImageView(context);
            RelativeLayout.LayoutParams offlineImageParams = new RelativeLayout.LayoutParams
                    (android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            offlineImageParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            offlineImageParams.addRule(RelativeLayout.CENTER_VERTICAL);
            offlineImage.setLayoutParams(offlineImageParams);
            cameraRelativeLayout.addView(offlineImage);
            offlineImage.setImageResource(R.drawable.cam_unavailable);
            offlineImage.setVisibility(View.INVISIBLE);

            gradientLayout = new GradientTitleLayout(activity);
            gradientLayout.setTitle(evercamCamera.getName());
            cameraRelativeLayout.addView(gradientLayout);

            cameraRelativeLayout.setClickable(true);

            // Show thumbnail returned from Evercam
            if(showThumbnails)
            {
                showThumbnail();
            }
            cameraRelativeLayout.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    VideoActivity.startPlayingVideoForCamera(activity, evercamCamera.getCameraId());
                }
            });
        }
        catch(OutOfMemoryError e)
        {
            Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));
        }
        catch(Exception e)
        {
            Log.e(TAG, e.toString() + "::" + Log.getStackTraceString(e));

            ParentAppCompatActivity.sendToMint(e);
        }
    }

    public Rect getOfflineIconBounds()
    {
        Rect bounds = new Rect();
        gradientLayout.getOfflineImageView().getHitRect(bounds);
        return bounds;
    }

    public void updateTitleIfDifferent()
    {
        for(EvercamCamera camera : AppData.evercamCameraList)
        {
            if(evercamCamera.getCameraId().equals(camera.getCameraId()))
            {
                gradientLayout.setTitle(camera.getName());
            }
        }
    }

    // Stop the image loading process. May be need to end current activity
    public boolean stopAllActivity()
    {
        end = true;

        return true;
    }

    // Image loaded form camera and now set the controls appearance and text
    // accordingly
    private void setLayoutForLiveImageReceived()
    {
        evercamCamera.setStatus(CameraStatus.ACTIVE);
        offlineImage.setVisibility(View.INVISIBLE);

        handler.removeCallbacks(LoadImageRunnable);
    }

    public boolean showThumbnail()
    {
        if(evercamCamera.hasThumbnailUrl())
        {
            Picasso.with(context).load(evercamCamera.getThumbnailUrl()).fit().into(snapshotImageView);

            if(!evercamCamera.isActive())
            {
                showGreyImage();
                showOfflineIcon();
            }

            //TODO: Remove this (Disabled thumbnail saving because it uses too much memory)
//            //Save the thumbnail, it will be showing before live view get loaded
//            new Thread(new SaveImageRunnable(context, evercamCamera.getThumbnailUrl(),
//                    evercamCamera.getCameraId())).start();

            return true;
        }
        else
        {
            showOfflineIcon();
            offlineImage.setVisibility(View.VISIBLE);
            snapshotImageView.setBackgroundColor(Color.GRAY);
            gradientLayout.removeGradientShadow();
            CameraLayout.this.evercamCamera.loadingStatus = ImageLoadingStatus.live_not_received;
            handler.postDelayed(LoadImageRunnable, 0);
        }
        return false;
    }

    private void showOfflineIcon()
    {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run()
            {
                gradientLayout.showOfflineIcon(true, showOfflineIconAsFloat);
            }
        }, 300);
    }

    // Image not received form cache, Evercam nor camera side. Set the controls
    // appearance and text accordingly
    private void setLayoutForNoImageReceived()
    {
        if(!evercamCamera.isActive())
        {
            showGreyImage();

            showOfflineIcon();
        }

        // animation must have been stopped when image loaded from cache
        handler.removeCallbacks(LoadImageRunnable);
    }

    public Runnable LoadImageRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                if(end) return;

                if(evercamCamera.loadingStatus == ImageLoadingStatus.not_started)
                {
                    if(evercamCamera.isActive())
                    {
                        //showAndSaveLiveSnapshot();
                    }
                }
                else if(evercamCamera.loadingStatus == ImageLoadingStatus.live_received)
                {
                    setLayoutForLiveImageReceived();
                }
                else if(evercamCamera.loadingStatus == ImageLoadingStatus.live_not_received)
                {
                    setLayoutForNoImageReceived();
                }
            }
            catch(OutOfMemoryError e)
            {
                Log.e(TAG, e.toString() + "-::OOM::-" + Log.getStackTraceString(e));

                handler.postDelayed(LoadImageRunnable, 5000);
            }
            catch(Exception e)
            {
                Log.e(TAG, e.toString() + "::" + Log.getStackTraceString(e));
                if(!end)
                {
                    handler.postDelayed(LoadImageRunnable, 5000);
                }
            }
        }
    };

    private void showGreyImage()
    {
        snapshotImageView.setAlpha(0.5f);
    }
}
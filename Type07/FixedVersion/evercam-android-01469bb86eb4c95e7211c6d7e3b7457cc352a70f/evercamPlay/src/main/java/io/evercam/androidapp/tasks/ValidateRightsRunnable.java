package io.evercam.androidapp.tasks;

import android.app.Activity;
import android.util.Log;

import io.evercam.Camera;
import io.evercam.EvercamException;
import io.evercam.Right;
import io.evercam.androidapp.sharing.SharingActivity;
import io.evercam.androidapp.utils.Constants;

public class ValidateRightsRunnable implements Runnable
{
    private final String TAG = "ValidateSharingRunnable";
    private String mCameraId;
    private Activity mActivity;

    public ValidateRightsRunnable(Activity activity, String cameraId)
    {
        mCameraId = cameraId;
        this.mActivity = activity;
    }

    @Override
    public void run()
    {
        try
        {
            Camera camera = Camera.getById(mCameraId, false);
            Right right = camera.getRights();
            Log.e(TAG, right.toString());
            if(mActivity instanceof SharingActivity)
            {
                if(!right.isFullRight())
                {
                    reloadCamerasOnUi();
                }
            }
        }
        catch(EvercamException e)
        {
            e.printStackTrace();
            reloadCamerasOnUi();
        }
    }

    private void reloadCamerasOnUi()
    {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                mActivity.setResult(Constants.RESULT_NO_ACCESS);
                mActivity.finish();
            }
        });
    }
}

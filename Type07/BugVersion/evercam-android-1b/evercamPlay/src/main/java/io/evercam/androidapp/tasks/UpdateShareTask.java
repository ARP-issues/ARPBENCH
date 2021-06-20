package io.evercam.androidapp.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import io.evercam.CameraShare;
import io.evercam.CameraShareInterface;
import io.evercam.CameraShareRequest;
import io.evercam.EvercamException;
import io.evercam.androidapp.R;
import io.evercam.androidapp.custom.CustomProgressDialog;
import io.evercam.androidapp.custom.CustomSnackbar;
import io.evercam.androidapp.custom.CustomToast;
import io.evercam.androidapp.dto.AppData;
import io.evercam.androidapp.sharing.SharingActivity;
import io.evercam.androidapp.utils.Constants;

public class UpdateShareTask extends AsyncTask<Void, Void, Boolean>
{
    private final String TAG = "PatchShareTask";
    private CameraShareInterface shareInterface;
    private String  mNewRights;
    private Activity activity;
    private CustomProgressDialog customProgressDialog;
    private String errorMessage;
    private boolean userDeletedSelf = false;

    public UpdateShareTask(Activity activity, CameraShareInterface shareInterface, String newRights)
    {
        this.activity = activity;
        this.shareInterface = shareInterface;
        mNewRights = newRights;
    }

    @Override
    protected void onPreExecute()
    {
        errorMessage = activity.getString(R.string.unknown_error);
        customProgressDialog = new CustomProgressDialog(activity);
        customProgressDialog.show(activity.getString(R.string.patching_camera));
    }

    @Override
    protected Boolean doInBackground(Void... params)
    {
        return updateShare();
    }

    @Override
    protected void onPostExecute(Boolean isSuccess)
    {
        customProgressDialog.dismiss();

        if(isSuccess)
        {
            if(!userDeletedSelf)
            {
                CustomSnackbar.show(activity, R.string.msg_share_updated);

                //Update share list in the sharing activity
                FetchShareListTask.launch(SharingActivity.evercamCamera.getCameraId(), activity);
            }
            else //If user deleted his own access, reload camera list
            {
                if(activity instanceof SharingActivity)
                {
                    activity.setResult(Constants.RESULT_ACCESS_REMOVED);
                    activity.finish();
                }
            }
        }
        else
        {
            CustomToast.showInCenterLong(activity, errorMessage);
        }
    }

    protected boolean updateShare()
    {
        //Delete share / share request
        if(mNewRights == null)
        {
            return deleteShare();
        }
        else //Patch camera share / share request
        {
            /**
             * The rights string should never be empty
             * If it's empty, don't break the app and print error message
             */
            if(mNewRights.isEmpty())
            {
                Log.e(TAG, "Right to patch is empty");
            }
            else
            {
                return patchShare(mNewRights);
            }
        }
        return false;
    }

    protected boolean deleteShare()
    {
        try
        {
            if(shareInterface instanceof CameraShare)
            {
                String cameraId = ((CameraShare) shareInterface).getCameraId();
                String userEmail = ((CameraShare) shareInterface).getUserEmail();
                boolean isDeleted = CameraShare.delete(cameraId, userEmail);
                if(((CameraShare) shareInterface).getUserId().equals(AppData.defaultUser.getUsername()))
                {
                    userDeletedSelf = true;
                }
                return isDeleted;
            }
            else if(shareInterface instanceof CameraShareRequest)
            {
                String cameraId = ((CameraShareRequest) shareInterface).getCameraId();
                String userEmail = ((CameraShareRequest) shareInterface).getEmail();
                return CameraShareRequest.delete(cameraId, userEmail);
            }
        }
        catch(EvercamException e)
        {
            errorMessage = e.getMessage();
        }

        return false;
    }

    protected boolean patchShare(String newRights)
    {
        CameraShareInterface patchedShare = null;

        try
        {
            if(shareInterface instanceof CameraShare)
            {
                String cameraId = ((CameraShare) shareInterface).getCameraId();
                String userEmail = ((CameraShare) shareInterface).getUserEmail();

                patchedShare = CameraShare.patch(cameraId, userEmail, newRights);
            }
            else if(shareInterface instanceof CameraShareRequest)
            {
                String cameraId = ((CameraShareRequest) shareInterface).getCameraId();
                String userEmail = ((CameraShareRequest) shareInterface).getEmail();
                patchedShare = CameraShareRequest.patch(cameraId, userEmail, newRights);
            }

            if(patchedShare != null) return true;
        }
        catch(EvercamException e)
        {
            errorMessage = e.getMessage();
        }

        return false;
    }

    public static void launch(Activity activity, CameraShareInterface shareInterface, String newRights)
    {
        new UpdateShareTask(activity, shareInterface, newRights)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

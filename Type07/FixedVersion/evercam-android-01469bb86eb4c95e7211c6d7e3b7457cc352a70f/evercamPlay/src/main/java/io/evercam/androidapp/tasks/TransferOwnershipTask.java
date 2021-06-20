package io.evercam.androidapp.tasks;

import android.app.Activity;
import android.os.AsyncTask;

import io.evercam.Camera;
import io.evercam.EvercamException;
import io.evercam.androidapp.R;
import io.evercam.androidapp.custom.CustomProgressDialog;
import io.evercam.androidapp.custom.CustomToast;
import io.evercam.androidapp.sharing.SharingActivity;
import io.evercam.androidapp.utils.Constants;

public class TransferOwnershipTask extends AsyncTask<Void, Void, Boolean>
{
    private Activity activity;
    private CustomProgressDialog customProgressDialog;
    private String errorMessage;
    private String cameraId;
    private String userId;

    public TransferOwnershipTask(Activity activity, String cameraId, String userId)
    {
        this.activity = activity;
        this.cameraId = cameraId;
        this.userId = userId;
    }

    @Override
    protected void onPreExecute()
    {
        errorMessage = activity.getString(R.string.unknown_error);
        customProgressDialog = new CustomProgressDialog(activity);
        customProgressDialog.show(activity.getString(R.string.msg_transferring));
    }

    @Override
    protected Boolean doInBackground(Void... params)
    {
        try
        {
            Camera.transfer(cameraId, userId);

            return true;
        }
        catch(EvercamException e)
        {
            errorMessage = e.getMessage();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean success)
    {
        customProgressDialog.dismiss();

        if(success)
        {
            if(activity instanceof SharingActivity)
            {
                activity.setResult(Constants.RESULT_TRANSFERRED);
                activity.finish();
            }
        }
        else
        {
            CustomToast.showInCenterLong(activity, errorMessage);
        }
    }

    public static void launch(Activity activity, String cameraId, String userId)
    {
        new TransferOwnershipTask(activity, cameraId, userId)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

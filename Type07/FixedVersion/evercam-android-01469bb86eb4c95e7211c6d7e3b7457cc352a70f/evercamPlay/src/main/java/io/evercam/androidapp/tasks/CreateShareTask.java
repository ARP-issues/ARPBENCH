package io.evercam.androidapp.tasks;

import android.app.Activity;
import android.os.AsyncTask;

import io.evercam.CameraShare;
import io.evercam.CameraShareInterface;
import io.evercam.CameraShareRequest;
import io.evercam.EvercamException;
import io.evercam.androidapp.R;
import io.evercam.androidapp.custom.CustomProgressDialog;
import io.evercam.androidapp.custom.CustomToast;
import io.evercam.androidapp.sharing.CreateShareActivity;
import io.evercam.androidapp.utils.Constants;

public class CreateShareTask extends AsyncTask<Void, Void, CameraShareInterface>
{
    private Activity activity;
    private CustomProgressDialog customProgressDialog;
    private String errorMessage;
    private String userId;
    private String cameraId;
    private String rights;
    private String message;

    public CreateShareTask(Activity activity, String userId, String camerId, String rights, String message)
    {
        this.activity = activity;
        this.userId = userId;
        this.cameraId = camerId;
        this.rights = rights;
        this.message = message;
    }

    @Override
    protected void onPreExecute()
    {
        errorMessage = activity.getString(R.string.unknown_error);
        customProgressDialog = new CustomProgressDialog(activity);
        customProgressDialog.show(activity.getString(R.string.msg_sharing));
    }

    @Override
    protected CameraShareInterface doInBackground(Void... params)
    {
        try
        {
            return CameraShare.create(cameraId, userId, rights, message);
        }
        catch(EvercamException e)
        {
            errorMessage = e.getMessage();
        }
        return null;
    }

    @Override
    protected void onPostExecute(CameraShareInterface shareInterface)
    {
        customProgressDialog.dismiss();

        if(shareInterface != null)
        {
            if(shareInterface instanceof CameraShare)
            {
                activity.setResult(Constants.RESULT_SHARE_CREATED);
            }
            else if(shareInterface instanceof CameraShareRequest)
            {
                activity.setResult(Constants.RESULT_SHARE_REQUEST_CREATED);
            }

            if(activity instanceof CreateShareActivity)
            {
                activity.finish();
            }
        }
        else
        {
            CustomToast.showInCenterLong(activity, errorMessage);
        }
    }

    public static void launch(Activity activity, String userId, String cameraId, String rights, String message)
    {
        new CreateShareTask(activity, userId, cameraId, rights, message)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

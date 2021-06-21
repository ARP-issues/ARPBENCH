package io.evercam.androidapp.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import io.evercam.CameraShare;
import io.evercam.CameraShareInterface;
import io.evercam.CameraShareRequest;
import io.evercam.EvercamException;
import io.evercam.androidapp.sharing.SharingActivity;

public class FetchShareListTask extends AsyncTask<Void, Void, ArrayList<CameraShareInterface>>
{
    private final String TAG = "FetchShareListTask";
    private final String cameraId;
    private Activity activity;

    public FetchShareListTask(String cameraId, Activity activity)
    {
        this.cameraId = cameraId;
        this.activity = activity;
    }

    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();
    }

    @Override
    protected ArrayList<CameraShareInterface> doInBackground(Void... params)
    {
        ArrayList<CameraShareInterface> shareList = new ArrayList<>();

        try
        {
            shareList.addAll(CameraShare.getByCamera(cameraId));
            shareList.addAll(CameraShareRequest.get(cameraId, CameraShareRequest.STATUS_PENDING));
        }
        catch(EvercamException e)
        {
            Log.e(TAG, e.getMessage());
        }

        return shareList;
    }

    @Override
    protected void onPostExecute(ArrayList<CameraShareInterface> cameraShareList)
    {
        if(activity instanceof SharingActivity)
        {
            ((SharingActivity) activity).sharingListFragment
                    .updateShareListOnUi(cameraShareList);
        }
    }

    public static void launch(String cameraId, Activity activity)
    {
        new FetchShareListTask(cameraId, activity)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

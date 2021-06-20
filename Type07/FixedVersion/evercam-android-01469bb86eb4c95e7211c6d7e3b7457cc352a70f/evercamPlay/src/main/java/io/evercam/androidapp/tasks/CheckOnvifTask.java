package io.evercam.androidapp.tasks;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.Locale;

import io.evercam.Camera;
import io.evercam.EvercamException;
import io.evercam.Model;
import io.evercam.androidapp.dto.EvercamCamera;
import io.evercam.androidapp.video.VideoActivity;

public class CheckOnvifTask extends AsyncTask<Void, Void, Boolean>
{
    private final String TAG = "CheckOnvifTask";
    private WeakReference<VideoActivity> videoActivityWeakReference;
    private EvercamCamera mEvercamCamera;
    private String modelId;
    private String cameraId;

    public CheckOnvifTask(VideoActivity videoActivity, EvercamCamera camera)
    {
        this.mEvercamCamera = camera;
        videoActivityWeakReference = new WeakReference<>(videoActivity);
        this.modelId = camera.getModel().toLowerCase(Locale.UK);
        this.cameraId = camera.getCameraId();
    }

    @Override
    protected void onPreExecute()
    {
        getVideoActivity().isPtz = false;
    }

    @Override
    protected Boolean doInBackground(Void... params)
    {
        try
        {
            Model model = Model.getById(modelId);
            if(model.isOnvif() && model.isPTZ())
            {
                Camera camera = Camera.getById(cameraId, false);
                if(camera.getRights().isFullRight())
                {
                    return true;
                }
            }
        }
        catch(EvercamException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean hasPtz)
    {
        if(getVideoActivity() != null)
        {
            getVideoActivity().isPtz = hasPtz;
            getVideoActivity().showPtzControl(hasPtz);

            new LoadPresetsTask(getVideoActivity(), mEvercamCamera)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private VideoActivity getVideoActivity()
    {
        return videoActivityWeakReference.get();
    }
}

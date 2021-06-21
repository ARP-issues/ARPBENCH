package io.evercam.androidapp.tasks;

import android.os.AsyncTask;
import android.util.Log;

import io.evercam.PTZControl;
import io.evercam.PTZException;

public class PTZMoveTask extends AsyncTask<Void, Void, Void>
{
    private final String TAG = "PTZMoveTask";
    private PTZControl ptzControl;

    public PTZMoveTask(PTZControl ptzControl)
    {
        this.ptzControl = ptzControl;
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        try
        {
            ptzControl.move();
        }
        catch(PTZException e)
        {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    public static void launch(PTZControl ptzControl)
    {
        new PTZMoveTask(ptzControl).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

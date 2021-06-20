package io.evercam.androidapp.tasks;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.evercam.PTZException;
import io.evercam.PTZPreset;
import io.evercam.androidapp.dto.EvercamCamera;
import io.evercam.androidapp.video.VideoActivity;

public class LoadPresetsTask extends AsyncTask<Void, Void, Void>
{
    private WeakReference<VideoActivity> videoActivityWeakReference;
    private String cameraId;

    public LoadPresetsTask(VideoActivity videoActivity, EvercamCamera camera)
    {
        videoActivityWeakReference = new WeakReference<>(videoActivity);
        this.cameraId = camera.getCameraId();
    }

    @Override
    protected Void doInBackground(Void... params)
    {
        try
        {
            ArrayList<PTZPreset> allPresets = PTZPreset.getAllPresets(cameraId);

            if(getVideoActivity() != null)
            {
                getVideoActivity().presetList = allPresets;
            }

            ArrayList<PTZPreset> customPresets = removeSystemPresetsFrom(allPresets);

            if(customPresets.size() > 0)
            {
                getVideoActivity().presetList = customPresets;
            }
        }
        catch(PTZException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private ArrayList<PTZPreset> removeSystemPresetsFrom(ArrayList<PTZPreset> allPresets)
    {
        ArrayList<PTZPreset> customPresets = new ArrayList<>();
        if(allPresets.size() > 0)
        {
            //Exclude presets with token >= 33 and only keep those user defined presets
            for(PTZPreset preset : allPresets)
            {
                int tokenInt = Integer.valueOf(preset.getToken());
                if(tokenInt < 33)
                {
                    customPresets.add(preset);
                }
            }
        }
        return customPresets;
    }

    private VideoActivity getVideoActivity()
    {
        return videoActivityWeakReference.get();
    }
}

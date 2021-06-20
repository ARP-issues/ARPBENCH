package io.evercam.androidapp.tasks;

import android.os.AsyncTask;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.lang.ref.WeakReference;

import io.evercam.androidapp.AddEditCameraActivity;
import io.evercam.androidapp.EvercamPlayApplication;

public class PortCheckTask extends AsyncTask<Void, Void, Boolean>
{
    public enum PortType{HTTP, RTSP};

    private final static String TAG = "PortCheckTask";

    private String mIp;
    private String mPort;
    private PortType mPortType;
    private WeakReference<AddEditCameraActivity> activityWeakReference;

    public PortCheckTask(String ip, String port, AddEditCameraActivity activity, PortType type)
    {
        this.mIp = ip;
        this.mPort = port;
        this.mPortType = type;
        this.activityWeakReference = new WeakReference<>(activity);
    }

    @Override
    protected Boolean doInBackground(Void... params)
    {
        try
        {
            return isPortOpen(mIp, mPort);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            EvercamPlayApplication.sendCaughtException(getActivity(), e);
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean isOpen)
    {
        if(getActivity() != null)
        {
            if(mPortType.equals(PortType.HTTP))
            {
                getActivity().updateHttpPortStatus(isOpen);
            }
            else if (mPortType.equals(PortType.RTSP))
            {
                getActivity().updateRtspPortStatus(isOpen);
            }
        }
    }

    private AddEditCameraActivity getActivity()
    {
        return activityWeakReference.get();
    }

    public static boolean isPortOpen(String ip, String port)
    {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://tuq.in/tools/port.txt?ip=" + ip + "&port=" + port).build();
        try
        {
            Response response = client.newCall(request).execute();
            String responseString = response.body().string();
            return responseString.equalsIgnoreCase("true");
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }
}

package io.evercam.androidapp.tasks;

import android.app.Activity;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.evercam.androidapp.custom.CustomSnackbar;
import io.evercam.androidapp.permission.Permission;
import io.evercam.androidapp.photoview.SnapshotManager;
import io.evercam.androidapp.video.VideoActivity;

public class CaptureSnapshotRunnable implements Runnable
{
    private final String TAG = "CaptureSnapshotRunnable";

    private Activity activity;
    private String cameraId;
    private String path;
    private Bitmap bitmap;

    public CaptureSnapshotRunnable(Activity activity, String cameraId,
                                   SnapshotManager.FileType fileType, Bitmap bitmap)
    {
        this.activity = activity;
        this.cameraId = cameraId;
        this.path = SnapshotManager.createFilePath
                (cameraId, fileType);
        this.bitmap = bitmap;
    }

    public String capture(Bitmap snapshotBitmap)
    {
        if(Permission.isGranted(activity, Permission.STORAGE))
        {
            if(activity instanceof VideoActivity)
            {
                ((VideoActivity) activity).setTempSnapshotBitmap(null);
            }

            if(snapshotBitmap != null)
            {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                snapshotBitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

                File f = new File(path);

                try
                {
                    f.createNewFile();
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(bytes.toByteArray());
                    fo.close();
                    return f.getPath();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        else
        {
            if(activity instanceof VideoActivity)
            {
                ((VideoActivity) activity).setTempSnapshotBitmap(snapshotBitmap);
            }

            Permission.request(activity, new String[]{Permission.STORAGE},
                    Permission.REQUEST_CODE_STORAGE);
        }
        return "";
    }

    @Override
    public void run()
    {
        if(bitmap != null)
        {
            final String savedPath = capture(bitmap);

            if(!savedPath.isEmpty())
            {
                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        CustomSnackbar.showSnapshotSaved(activity, cameraId);
                    }
                });
            }
        }
    }
}

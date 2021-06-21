package io.evercam.androidapp.tasks;

import android.app.Activity;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.evercam.androidapp.custom.CustomToast;
import io.evercam.androidapp.photoview.SnapshotManager;

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
                        CustomToast.showSuperSnapshotSaved(activity, cameraId);
                    }
                });
            }
        }
    }
}

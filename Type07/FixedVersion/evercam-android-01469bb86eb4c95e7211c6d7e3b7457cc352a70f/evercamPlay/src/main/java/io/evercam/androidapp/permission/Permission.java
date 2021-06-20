package io.evercam.androidapp.permission;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

public class Permission
{
    public final static String STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public final static String CONTACTS = "android.permission.GET_ACCOUNTS";
    public final static int REQUEST_CODE_STORAGE = 200;
    public final static int REQUEST_CODE_CONTACTS = 300;


    /**
     * Check if the permission is granted or not on Android M (6.0) or above
     * Always return true for Android versions below 6.0, the permission to be checked
     * has to be defined in AndroidManifest.xml
     */
    public static boolean isGranted(Activity activity, String permission)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Request system permissions on Android 6.0 or above by showing a confirm dialog.
     * The dialog result should be handled in correspondent activities' onRequestPermissionsResult()
     */
    public static void request(Activity activity, String[] permissions, int requestCode)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            activity.requestPermissions(permissions, requestCode);
        }
    }
}

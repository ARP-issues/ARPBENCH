package io.evercam.androidapp.sharing;

import android.app.Activity;
import android.content.Context;

import io.evercam.CameraShareInterface;
import io.evercam.Right;
import io.evercam.androidapp.R;
import io.evercam.androidapp.tasks.UpdateShareTask;

public class RightsStatus
{
    private final String TAG = "RightsStatus";
    private String description;
    private String rightString = "";
    private Activity activity;
    private static int fullRightsStringId = R.string.full_rights;
    private static int readOnlyStringId = R.string.read_only;
    private static int noAccessStringId = R.string.no_access;

    public RightsStatus(Activity activity, String description)
    {
        this.activity = activity;
        this.description = description;
        String fullRightsDescription = activity.getString(fullRightsStringId);
        String readOnlyDescription = activity.getString(readOnlyStringId);
        String noAccessDescription = activity.getString(noAccessStringId);

        if(description.equals(fullRightsDescription))
        {
            rightString = Right.FULL_RIGHTS;
        }
        else if(description.equals(readOnlyDescription))
        {
            rightString = Right.READ_ONLY;
        }
        else if(description.equals(noAccessDescription))
        {
            rightString = null;
        }
    }

    public String getRightString()
    {
        return rightString;
    }

    public void updateOnShare(CameraShareInterface shareInterface)
    {
        UpdateShareTask.launch(activity, shareInterface, rightString);
    }

    public static String[] getDefaultItems(Context context)
    {
        return new String[] {context.getString(readOnlyStringId),
                context.getString(fullRightsStringId)};
    }

    public static CharSequence[] getFullItems(Context context)
    {
        return new CharSequence[] {context.getString(fullRightsStringId),
                context.getString(readOnlyStringId)
                , context.getString(noAccessStringId)};
    }
}

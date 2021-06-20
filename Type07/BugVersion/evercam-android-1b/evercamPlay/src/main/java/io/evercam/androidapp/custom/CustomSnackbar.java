package io.evercam.androidapp.custom;

import android.app.Activity;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

import io.evercam.androidapp.R;

public class CustomSnackbar
{
    public static void show(Activity activity, int messageId)
    {
        //TODO: Replace Snackbar with Android design API
        Snackbar snackbar = Snackbar.with(activity).text(messageId)
                .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                .color(activity.getResources().getColor(R.color.dark_gray_background));
        SnackbarManager.show(snackbar);
    }

    public static void showMultiLine(Activity activity, int messageId)
    {
        //TODO: Replace Snackbar with Android design API
        Snackbar snackbar = Snackbar.with(activity).text(messageId)
                .type(SnackbarType.MULTI_LINE)
                .duration(Snackbar.SnackbarDuration.LENGTH_LONG)
                .color(activity.getResources().getColor(R.color.dark_gray_background));
        SnackbarManager.show(snackbar);
    }
}

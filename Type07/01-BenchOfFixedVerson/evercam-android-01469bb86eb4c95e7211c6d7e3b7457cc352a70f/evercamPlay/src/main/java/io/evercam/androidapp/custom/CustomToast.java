package io.evercam.androidapp.custom;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import com.github.johnpersano.supertoasts.SuperToast;

public class CustomToast
{
    public static void showInCenter(Context context, String message)
    {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void showInCenter(Context context, int message)
    {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void showInCenterLong(Context context, String message)
    {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void showInCenterLong(Context context, int message)
    {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void showInCenterExtraLong(Context context, int message)
    {
        SuperToast superToast = new SuperToast(context);
        superToast.setDuration(SuperToast.Duration.EXTRA_LONG);
        superToast.setText(context.getString(message));
        superToast.setGravity(Gravity.CENTER, 0, 0);
        superToast.show();
    }
}

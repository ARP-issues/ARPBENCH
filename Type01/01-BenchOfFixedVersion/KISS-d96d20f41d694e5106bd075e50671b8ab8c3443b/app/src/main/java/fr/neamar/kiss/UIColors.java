package fr.neamar.kiss;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

public class UIColors {
    public static final String COLOR_DEFAULT = "#4caf50";
    public static final int[] COLOR_LIST = new int[]{
            0xFF4CAF50, 0xFFD32F2F, 0xFFC2185B, 0xFF7B1FA2,
            0xFF512DA8, 0xFF303F9F, 0xFF1976D2, 0xFF0288D1,
            0xFF0097A7, 0xFF00796B, 0xFF388E3C, 0xFF689F38,
            0xFFAFB42B, 0xFFFBC02D, 0xFFFFA000, 0xFFF57C00,
            0xFFE64A19, 0xFF5D4037, 0xFF616161, 0xFF455A64,
            0xFF000000
    };

    public static void updateThemePrimaryColor(Activity activity) {
        String notificationBarColorOverride = getNotificationBarColor(activity);

        // Circuit breaker, keep default behavior.
        if (notificationBarColorOverride.equals(COLOR_DEFAULT)) {
            return;
        }

        int notificationBarColor = Color.parseColor(notificationBarColorOverride);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Update status bar color
            window.setStatusBarColor(notificationBarColor);
        }

        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(notificationBarColor));
        }
    }

    private static String getNotificationBarColor(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("notification-bar-color", COLOR_DEFAULT);
    }

    public static String getPrimaryColor(Context context) {
        String primaryColor = PreferenceManager.getDefaultSharedPreferences(context).getString("primary-color", COLOR_DEFAULT);

        // Transparent can't be displayed for text color, replace with light gray.
        if (primaryColor.equals("#00000000") || primaryColor.equals(("#AAFFFFFF"))) {
            return "#BDBDBD";
        }

        return String.format("#%06X", Color.parseColor(primaryColor) & 0xFFFFFF);
    }
}

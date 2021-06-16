package org.ligi.passandroid.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import java.io.File;
import org.ligi.passandroid.R;
import org.ligi.passandroid.model.comparator.PassSortOrder;
import static org.ligi.passandroid.R.string.preference_key_autolight;
import static org.ligi.passandroid.R.string.preference_key_condensed;

public class AndroidSettings implements Settings {
    public final Context context;

    final SharedPreferences sharedPreferences;

    public AndroidSettings(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public PassSortOrder getSortOrder() {
        final String key = context.getString(R.string.preference_key_sort);
        final String stringValue = sharedPreferences.getString(key, "0");
        final int id = Integer.valueOf(stringValue);
        for (PassSortOrder order : PassSortOrder.values()) {
            if (order.getInt() == id) {
                return order;
            }
        }
        return PassSortOrder.DATE_ASC;
    }

    @Override
    public boolean doTraceDroidEmailSend() {
        // will be overridden in test-module
        return true;
    }

    @Override
    public File getPassesDir() {
        return new File(context.getFilesDir().getAbsolutePath(), "passes");
    }

    @Override
    public File getStateDir() {
        return new File(context.getFilesDir(), "state");
    }


    @Override
    public File getShareDir() {
        return new File(Environment.getExternalStorageDirectory(), "tmp/passbook_share_tmp/");
    }

    @Override
    public boolean isCondensedModeEnabled() {
        return sharedPreferences.getBoolean(context.getString(preference_key_condensed), false);
    }

    @Override
    public boolean isAutomaticLightEnabled() {
        return sharedPreferences.getBoolean(context.getString(preference_key_autolight), true);
    }

}


package com.ichi2.anki;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import com.ichi2.async.DeckTask;
import com.ichi2.async.DeckTask.TaskData;

import java.util.Calendar;

import timber.log.Timber;

public class UIUtils {

    public static float getDensityAdjustedValue(Context context, float value) {
        return context.getResources().getDisplayMetrics().density * value;
    }


    public static long getDayStart() {
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) < 4) {
            cal.roll(Calendar.DAY_OF_YEAR, -1);
        }
        cal.set(Calendar.HOUR_OF_DAY, 4);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }


    public static void saveCollectionInBackground(Context context) {
        if (CollectionHelper.getInstance().colIsOpen()) {
            DeckTask.launchDeckTask(DeckTask.TASK_TYPE_SAVE_COLLECTION, new DeckTask.TaskListener() {
                @Override
                public void onPreExecute() {
                    Timber.d("saveCollectionInBackground: start");
                }


                @Override
                public void onPostExecute(TaskData result) {
                    Timber.d("saveCollectionInBackground: finished");
                }


                @Override
                public void onProgressUpdate(TaskData... values) {
                }


                @Override
                public void onCancelled() {
                }
            });
        }
    }
}

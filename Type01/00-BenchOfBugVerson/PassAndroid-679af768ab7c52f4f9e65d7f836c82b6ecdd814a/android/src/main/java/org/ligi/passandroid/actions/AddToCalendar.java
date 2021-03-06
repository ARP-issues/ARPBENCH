package org.ligi.passandroid.actions;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.CalendarContract;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import org.ligi.passandroid.R;
import org.ligi.passandroid.model.pass.Pass;
import org.threeten.bp.ZonedDateTime;

public class AddToCalendar {

    public static void tryAddDateToCalendar(final Pass pass, final Activity activity, final ZonedDateTime date) {

        if (pass.getCalendarTimespan() == null) {
            new AlertDialog.Builder(activity).setMessage(R.string.expiration_date_to_calendar_warning_message)
                                             .setTitle(R.string.expiration_date_to_calendar_warning_title)
                                             .setNegativeButton(android.R.string.cancel, null)
                                             .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                 @Override
                                                 public void onClick(DialogInterface dialog, int which) {
                                                     reallyAddToCalendar(pass, activity, date);
                                                 }
                                             })
                                             .show();
        } else {
            reallyAddToCalendar(pass, activity, date);
        }


    }

    private static void reallyAddToCalendar(Pass pass, Activity activity, ZonedDateTime date) {
        try {
            final Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setType("vnd.android.cursor.item/event");
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.toEpochSecond() * 1000);
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, date.toEpochSecond() * 1000 + 60 * 60 * 1000);
            intent.putExtra("title", pass.getDescription());
            activity.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            // TODO maybe action to install calendar app
            Snackbar.make(activity.getWindow().getDecorView(), R.string.no_calendar_app_found, Snackbar.LENGTH_LONG).show();
        }
    }

}

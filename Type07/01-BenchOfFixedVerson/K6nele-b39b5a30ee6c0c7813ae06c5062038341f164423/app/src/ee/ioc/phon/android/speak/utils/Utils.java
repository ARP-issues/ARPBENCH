/*
 * Copyright 2011-2015, Institute of Cybernetics at Tallinn University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.ioc.phon.android.speak.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.apache.commons.io.FileUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ee.ioc.phon.android.speak.Executable;
import ee.ioc.phon.android.speak.ExecutableString;
import ee.ioc.phon.android.speak.Log;
import ee.ioc.phon.android.speak.R;


/**
 * <p>Some useful static methods.</p>
 *
 * @author Kaarel Kaljurand
 */
public final class Utils {

    private Utils() {
    }

    /**
     * TODO: should we immediately return null if id = 0?
     */
    public static String idToValue(Context context, Uri contentUri, String columnId, String columnUrl, long id) {
        String value = null;
        Cursor c = context.getContentResolver().query(
                contentUri,
                new String[]{columnUrl},
                columnId + "= ?",
                new String[]{String.valueOf(id)},
                null);

        if (c.moveToFirst()) {
            value = c.getString(0);
        }
        c.close();
        return value;
    }


    /**
     * <p>Pretty-prints an integer value which expresses a size
     * of some data.</p>
     */
    public static String getSizeAsString(int size) {
        if (size > FileUtils.ONE_MB) {
            return String.format("%.1fMB", (float) size / FileUtils.ONE_MB);
        }

        if (size > FileUtils.ONE_KB) {
            return String.format("%.1fkB", (float) size / FileUtils.ONE_KB);
        }
        return size + "b";
    }


    /**
     * <p>Returns a bitmap that visualizes the given waveform (byte array),
     * i.e. a sequence of 16-bit integers.</p>
     * <p/>
     * TODO: show to high/low points in other color
     * TODO: show end pause data with another color
     */
    public static Bitmap drawWaveform(byte[] waveBuffer, int w, int h, int start, int end) {
        final Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(b);
        final Paint paint = new Paint();
        paint.setColor(0xFFFFFFFF); // 0xRRGGBBAA
        paint.setAntiAlias(true);
        paint.setStrokeWidth(0);

        final Paint redPaint = new Paint();
        redPaint.setColor(0xFF000080);
        redPaint.setAntiAlias(true);
        redPaint.setStrokeWidth(0);

        final ShortBuffer buf = ByteBuffer.wrap(waveBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        buf.position(0);

        final int numSamples = waveBuffer.length / 2;
        //final int delay = (SAMPLING_RATE * 100 / 1000);
        final int delay = 0;
        int endIndex = end / 2 + delay;
        if (end == 0 || endIndex >= numSamples) {
            endIndex = numSamples;
        }
        int index = start / 2 - delay;
        if (index < 0) {
            index = 0;
        }
        final int size = endIndex - index;
        int numSamplePerPixel = 32;
        int delta = size / (numSamplePerPixel * w);
        if (delta == 0) {
            numSamplePerPixel = size / w;
            delta = 1;
        }

        final float scale = 3.5f / 65536.0f;
        // do one less column to make sure we won't read past
        // the buffer.
        try {
            for (int i = 0; i < w - 1; i++) {
                final float x = i;
                for (int j = 0; j < numSamplePerPixel; j++) {
                    final short s = buf.get(index);
                    final float y = (h / 2) - (s * h * scale);
                    if (s > Short.MAX_VALUE - 10 || s < Short.MIN_VALUE + 10) {
                        // TODO: make it work
                        c.drawPoint(x, y, redPaint);
                    } else {
                        c.drawPoint(x, y, paint);
                    }
                    index += delta;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // this can happen, but we don't care
        }

        return b;
    }


    /**
     * Creates a non-cancelable dialog with two buttons, both finish the activity,
     * one launches the given intent first.
     * TODO: note that we explicitly set the dialog style. This is because if the caller activity's style
     * is Theme.Translucent.NoTitleBar then the dialog is unstyled (maybe an Android bug?)
     */
    public static AlertDialog getLaunchIntentDialog(final Activity activity, String msg, final Intent intent) {
        return new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog)
                .setPositiveButton(activity.getString(R.string.buttonGoToSettings), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        activity.startActivity(intent);
                        activity.finish();
                    }
                })
                .setNegativeButton(activity.getString(R.string.buttonCancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        activity.finish();
                    }
                })
                .setMessage(msg)
                .setCancelable(false)
                .create();
    }


    public static AlertDialog getYesNoDialog(Context context, String confirmationMessage, final Executable ex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setMessage(confirmationMessage)
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.buttonYes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ex.execute();
                    }
                })
                .setNegativeButton(context.getString(R.string.buttonNo), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }


    public static AlertDialog getTextEntryDialog(Context context, String title, String initialText, final ExecutableString ex) {
        final View textEntryView = LayoutInflater.from(context).inflate(R.layout.alert_dialog_text_entry, null);
        final EditText et = (EditText) textEntryView.findViewById(R.id.url_edit);
        et.setText(initialText);
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(textEntryView)
                .setPositiveButton(R.string.buttonOk, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ex.execute(et.getText().toString());
                    }
                })
                .setNegativeButton(R.string.buttonCancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                })
                .create();
    }


    public static String getVersionName(Context c) {
        PackageInfo info = getPackageInfo(c);
        if (info == null) {
            return "?.?.?";
        }
        return info.versionName;
    }


    private static PackageInfo getPackageInfo(Context c) {
        PackageManager manager = c.getPackageManager();
        try {
            return manager.getPackageInfo(c.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Log.e("Couldn't find package information in PackageManager: " + e);
        }
        return null;
    }


    public static String chooseValue(String firstChoice, String secondChoice) {
        if (firstChoice == null) {
            return secondChoice;
        }
        return firstChoice;
    }


    public static String chooseValue(String firstChoice, String secondChoice, String thirdChoice) {
        String choice = chooseValue(firstChoice, secondChoice);
        if (choice == null) {
            return thirdChoice;
        }
        return choice;
    }


    public static List<String> ppBundle(Bundle bundle) {
        return ppBundle("/", bundle);
    }


    private static List<String> ppBundle(String bundleName, Bundle bundle) {
        List<String> strings = new ArrayList<>();
        if (bundle == null) {
            return strings;
        }
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            String name = bundleName + key;
            if (value instanceof Bundle) {
                strings.addAll(ppBundle(name + "/", (Bundle) value));
            } else {
                if (value instanceof Object[]) {
                    strings.add(name + ": " + Arrays.toString((Object[]) value));
                } else if (value instanceof float[]) {
                    strings.add(name + ": " + Arrays.toString((float[]) value));
                } else {
                    strings.add(name + ": " + value);
                }
            }
        }
        return strings;
    }


    /**
     * <p>Traverses the given bundle looking for the given key. The search also
     * looks into embedded bundles and thus differs from {@code Bundle.get(String)}.
     * Returns the first found entry as an object. If the given bundle does not
     * contain the given key then returns {@code null}.</p>
     *
     * @param bundle bundle (e.g. intent extras)
     * @param key    key of a bundle entry (possibly in an embedded bundle)
     * @return first matching key's value
     */
    public static Object getBundleValue(Bundle bundle, String key) {
        for (String k : bundle.keySet()) {
            Object value = bundle.get(k);
            if (value instanceof Bundle) {
                Object deepValue = getBundleValue((Bundle) value, key);
                if (deepValue != null) {
                    return deepValue;
                }
            } else if (key.equals(k)) {
                return value;
            }
        }
        return null;
    }


    public static String makeUserAgentComment(String tag, String versionName, String caller) {
        return tag + "/" + versionName + "; " +
                Build.MANUFACTURER + "/" +
                Build.DEVICE + "/" +
                Build.DISPLAY + "; " +
                caller;
    }
}

/*
 * Kontalk Android client
 * Copyright (C) 2017 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.ui.prefs;

import com.afollestad.materialdialogs.color.ColorChooserDialog;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import org.kontalk.ui.ToolbarActivity;
import org.kontalk.util.Preferences;


/**
 * Special preferences activity invoked from system notifications settings.
 * @author Daniele Ricci
 */
public abstract class BasePreferencesActivity extends ToolbarActivity
    implements ColorChooserDialog.ColorCallback {

    // used only for notification LED color for now
    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        Preferences.setNotificationLEDColor(selectedColor);
    }

    @Override
    public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog) {
    }
}

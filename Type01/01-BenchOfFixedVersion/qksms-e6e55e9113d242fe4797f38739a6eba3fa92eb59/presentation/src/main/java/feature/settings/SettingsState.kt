/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package feature.settings

import android.os.Build
import util.Preferences

data class SettingsState(
        val syncing: Boolean = false,

        val isDefaultSmsApp: Boolean = false,
        val nightModeSummary: String = "",
        val nightModeId: Int = Preferences.NIGHT_MODE_OFF,
        val nightStart: String = "",
        val nightEnd: String = "",
        val black: Boolean = false,
        val autoEmojiEnabled: Boolean = true,
        val notificationsEnabled: Boolean = true,
        val deliveryEnabled: Boolean = false,
        val qkReplyEnabled: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.N,
        val splitSmsEnabled: Boolean = false,
        val stripUnicodeEnabled: Boolean = false,
        val mmsEnabled: Boolean = true,
        val maxMmsSizeSummary: String = "100KB",
        val maxMmsSizeId: Int = 100
)
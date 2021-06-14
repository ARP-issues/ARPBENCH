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
package common.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.EditText
import com.moez.QKSMS.R
import com.uber.autodispose.android.scope
import com.uber.autodispose.kotlin.autoDisposable
import common.util.Colors
import common.util.FontProvider
import injection.appComponent
import io.reactivex.Observable
import javax.inject.Inject

/**
 * Custom implementation of EditText to allow for dynamic text colors
 *
 * Beware of updating to extend AppCompatTextView, as this inexplicably breaks the view in
 * the contacts chip view
 */
class QkEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : EditText(context, attrs) {

    @Inject lateinit var colors: Colors
    @Inject lateinit var fontProvider: FontProvider

    private var textColorObservable: Observable<Int>? = null
    private var textColorHintObservable: Observable<Int>? = null

    init {
        appComponent.inject(this)
        fontProvider.getLato { setTypeface(it, typeface?.style ?: Typeface.NORMAL) }

        context.obtainStyledAttributes(attrs, R.styleable.QkEditText)?.run {
            textColorObservable = when (getInt(R.styleable.QkEditText_textColor, -1)) {
                0 -> colors.textPrimary
                1 -> colors.textSecondary
                2 -> colors.textTertiary
                3 -> colors.textPrimaryOnTheme
                4 -> colors.textSecondaryOnTheme
                5 -> colors.textTertiaryOnTheme
                else -> null
            }
            textColorHintObservable = when (getInt(R.styleable.QkEditText_textColorHint, -1)) {
                0 -> colors.textPrimary
                1 -> colors.textSecondary
                2 -> colors.textTertiary
                else -> null
            }
            recycle()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        textColorObservable
                ?.autoDisposable(scope())
                ?.subscribe { color -> setTextColor(color) }

        textColorHintObservable
                ?.autoDisposable(scope())
                ?.subscribe { color -> setHintTextColor(color) }
    }

}
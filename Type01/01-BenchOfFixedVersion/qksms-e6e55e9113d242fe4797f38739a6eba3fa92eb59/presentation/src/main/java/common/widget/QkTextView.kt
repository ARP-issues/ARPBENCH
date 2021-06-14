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
import android.support.text.emoji.widget.EmojiAppCompatTextView
import android.util.AttributeSet
import com.moez.QKSMS.R
import common.util.Colors
import common.util.FontProvider
import common.util.extensions.getColorCompat
import injection.appComponent
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

open class QkTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : EmojiAppCompatTextView(context, attrs) {

    companion object {
        const val COLOR_PRIMARY = 0
        const val COLOR_SECONDARY = 1
        const val COLOR_TERTIARY = 2
        const val COLOR_PRIMARY_ON_THEME = 3
        const val COLOR_SECONDARY_ON_THEME = 4
        const val COLOR_TERTIARY_ON_THEME = 5
    }

    @Inject lateinit var colors: Colors
    @Inject lateinit var fontProvider: FontProvider

    var textColorObservable: Observable<Int>? = null
        set(value) {
            field = value
            updateSubscription()
        }

    private var textColorDisposable: Disposable? = null
        set(value) {
            field?.dispose()
            field = value
        }

    init {
        if (!isInEditMode) {
            appComponent.inject(this)
            fontProvider.getLato { setTypeface(it, typeface?.style ?: Typeface.NORMAL) }
        }

        context.obtainStyledAttributes(attrs, R.styleable.QkTextView)?.run {
            val colorAttr = getInt(R.styleable.QkTextView_textColor, -1)
            if (isInEditMode) {
                setTextColor(context.getColorCompat(when (colorAttr) {
                    COLOR_PRIMARY -> R.color.textPrimary
                    COLOR_SECONDARY -> R.color.textSecondary
                    COLOR_TERTIARY -> R.color.textTertiary
                    COLOR_PRIMARY_ON_THEME -> R.color.textPrimaryDark
                    COLOR_SECONDARY_ON_THEME -> R.color.textSecondaryDark
                    COLOR_TERTIARY_ON_THEME -> R.color.textTertiaryDark
                    else -> R.color.textPrimary
                }))
            } else {
                textColorObservable = when (colorAttr) {
                    COLOR_PRIMARY -> colors.textPrimary
                    COLOR_SECONDARY -> colors.textSecondary
                    COLOR_TERTIARY -> colors.textTertiary
                    COLOR_PRIMARY_ON_THEME -> colors.textPrimaryOnTheme
                    COLOR_SECONDARY_ON_THEME -> colors.textSecondaryOnTheme
                    COLOR_TERTIARY_ON_THEME -> colors.textTertiaryOnTheme
                    else -> null
                }
            }

            recycle()
        }
    }

    private fun updateSubscription() {
        if (isAttachedToWindow) {
            textColorDisposable = textColorObservable?.subscribe { color ->
                setTextColor(color)
                setLinkTextColor(color)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateSubscription()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        textColorDisposable?.dispose()
    }
}
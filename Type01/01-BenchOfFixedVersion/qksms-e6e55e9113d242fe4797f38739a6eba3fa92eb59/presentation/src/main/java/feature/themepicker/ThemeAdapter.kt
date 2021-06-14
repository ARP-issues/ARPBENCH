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
package feature.themepicker

import android.content.Context
import android.content.res.Resources
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.moez.QKSMS.R
import common.util.Colors
import common.util.extensions.dpToPx
import common.util.extensions.setBackgroundTint
import common.util.extensions.setTint
import common.util.extensions.setVisible
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.theme_list_item.view.*
import kotlinx.android.synthetic.main.theme_palette_list_item.view.*
import common.base.QkAdapter
import common.base.QkViewHolder
import javax.inject.Inject

class ThemeAdapter @Inject constructor(
        private val context: Context,
        private val colors: Colors
) : QkAdapter<List<Int>>() {

    var threadId: Long = 0
        set(value) {
            field = value
            threadIdChanged()
        }

    val colorSelected: Subject<Int> = PublishSubject.create()

    var selectedColor: Int = -1
        set(value) {
            val oldPosition = data.indexOfFirst { it.contains(field) }
            val newPosition = data.indexOfFirst { it.contains(value) }

            field = value

            oldPosition.takeIf { it != -1 }?.let { position -> notifyItemChanged(position) }
            newPosition.takeIf { it != -1 }?.let { position -> notifyItemChanged(position) }
        }

    private var iconTint = 0
    private val disposables = CompositeDisposable()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.theme_palette_list_item, parent, false)
        view.palette.flexWrap = FlexWrap.WRAP
        view.palette.flexDirection = FlexDirection.ROW

        return QkViewHolder(view)
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val palette = getItem(position)
        val view = holder.itemView

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val minPadding = (16 * 6).dpToPx(context)
        val size = if (screenWidth - minPadding > (56 * 5).dpToPx(context)) {
            56.dpToPx(context)
        } else {
            (screenWidth - minPadding) / 5
        }
        val swatchPadding = (screenWidth - size * 5) / 12

        view.palette.removeAllViews()
        view.palette.setPadding(swatchPadding, swatchPadding, swatchPadding, swatchPadding)

        (palette.subList(0, 5) + palette.subList(5, 10).reversed())
                .mapIndexed { index, color ->
                    LayoutInflater.from(context).inflate(R.layout.theme_list_item, view.palette, false).apply {

                        // Send clicks to the selected subject
                        setOnClickListener { colorSelected.onNext(color) }

                        // Apply the color to the view
                        theme.setBackgroundTint(color)

                        // Control the check visibility and tint
                        check.setVisible(color == selectedColor)
                        check.setTint(iconTint)

                        // Update the size so that the spacing is perfectly even
                        layoutParams = (layoutParams as FlexboxLayout.LayoutParams).apply {
                            height = size
                            width = size
                            isWrapBefore = index % 5 == 0
                            setMargins(swatchPadding, swatchPadding, swatchPadding, swatchPadding)
                        }
                    }
                }
                .forEach { theme -> view.palette.addView(theme) }
    }

    private fun threadIdChanged() {
        disposables.clear()
        disposables += colors.textPrimaryOnThemeForConversation(threadId)
                .subscribe { color -> iconTint = color }

    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        disposables.clear()
    }

}
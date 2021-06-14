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
package common

import android.content.Context
import android.support.annotation.ArrayRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import com.moez.QKSMS.R
import common.base.QkAdapter
import common.base.QkViewHolder
import common.util.Colors
import common.util.extensions.setTint
import common.util.extensions.setVisible
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.menu_list_item.view.*
import javax.inject.Inject

data class MenuItem(val title: String, val actionId: Int)

class MenuItemAdapter @Inject constructor(private val context: Context, private val colors: Colors) : QkAdapter<MenuItem>() {

    val menuItemClicks: Subject<Int> = PublishSubject.create()

    private val disposables = CompositeDisposable()

    var selectedItem: Int? = null
        set(value) {
            val old = data.map { it.actionId }.indexOfFirst { it == field }
            val new = data.map { it.actionId }.indexOfFirst { it == value }

            field = value

            old.let { notifyItemChanged(it) }
            new.let { notifyItemChanged(it) }
        }

    fun setData(@ArrayRes titles: Int, @ArrayRes values: Int = -1) {
        val valueInts = if (values != -1) context.resources.getIntArray(values) else null

        data = context.resources.getStringArray(titles)
                .mapIndexed { index, title -> MenuItem(title, valueInts?.getOrNull(index) ?: index) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.menu_list_item, parent, false)

        disposables += colors.theme
                .subscribe { view.check.setTint(it) }

        return QkViewHolder(view)
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val menuItem = getItem(position)
        val view = holder.itemView

        view.clicks().subscribe { menuItemClicks.onNext(menuItem.actionId) }

        view.title.text = menuItem.title
        view.check.setVisible(menuItem.actionId == selectedItem)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        disposables.clear()
    }

}
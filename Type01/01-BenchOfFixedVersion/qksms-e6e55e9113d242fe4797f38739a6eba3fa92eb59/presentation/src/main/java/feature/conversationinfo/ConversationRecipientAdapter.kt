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
package feature.conversationinfo

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.moez.QKSMS.R
import common.Navigator
import common.base.QkRealmAdapter
import common.base.QkViewHolder
import common.util.Colors
import common.util.extensions.setTint
import common.util.extensions.setVisible
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.conversation_recipient_list_item.view.*
import model.Recipient
import javax.inject.Inject

class ConversationRecipientAdapter @Inject constructor(
        private val context: Context,
        private val colors: Colors,
        private val navigator: Navigator
) : QkRealmAdapter<Recipient>() {

    var threadId: Long = 0L

    private val disposables = CompositeDisposable()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QkViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.conversation_recipient_list_item, parent, false)

        disposables += colors.textSecondary
                .subscribe { color -> view.add.setTint(color) }

        return QkViewHolder(view)
    }

    override fun onBindViewHolder(holder: QkViewHolder, position: Int) {
        val recipient = getItem(position)!!
        val view = holder.itemView

        view.setOnClickListener {
            if (recipient.contact == null) {
                navigator.addContact(recipient.address)
            } else {
                view.avatar.callOnClick()
            }
        }

        view.avatar.threadId = threadId
        view.avatar.contact = recipient.contact

        view.name.text = recipient.contact?.name ?: recipient.address

        view.address.text = recipient.address
        view.address.setVisible(recipient.contact != null)

        view.add.setVisible(recipient.contact == null)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        disposables.clear()
    }

}
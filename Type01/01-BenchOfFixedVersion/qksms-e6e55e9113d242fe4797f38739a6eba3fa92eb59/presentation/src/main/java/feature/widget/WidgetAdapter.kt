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
package feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.telephony.PhoneNumberUtils
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.moez.QKSMS.R
import common.util.Colors
import common.util.DateFormatter
import common.util.GlideApp
import common.util.extensions.dpToPx
import feature.main.MainActivity
import injection.appComponent
import model.Contact
import model.Conversation
import model.PhoneNumber
import repository.MessageRepository
import javax.inject.Inject

class WidgetAdapter(intent: Intent) : RemoteViewsService.RemoteViewsFactory {

    companion object {
        private const val MAX_CONVERSATIONS_COUNT = 25
    }

    @Inject lateinit var context: Context
    @Inject lateinit var colors: Colors
    @Inject lateinit var dateFormatter: DateFormatter
    @Inject lateinit var messageRepo: MessageRepository

    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private val smallWidget = intent.getBooleanExtra("small_widget", false)
    private var conversations: List<Conversation> = listOf()
    private val appWidgetManager by lazy { AppWidgetManager.getInstance(context) }

    // Cache colors, load lazily. This entire class is recreated when the widget is recreated,
    // so if the theme changes then these values will be correct in the new instance
    private val theme by lazy { colors.theme.blockingFirst() }
    private val background by lazy { colors.background.blockingFirst() }
    private val textPrimary by lazy { colors.textPrimary.blockingFirst() }
    private val textSecondary by lazy { colors.textSecondary.blockingFirst() }
    private val textTertiary by lazy { colors.textTertiary.blockingFirst() }
    private val textPrimaryOnTheme by lazy { colors.textPrimaryOnTheme.blockingFirst() }

    override fun onCreate() {
        appComponent.inject(this)
    }

    override fun onDataSetChanged() {
        conversations = messageRepo.getConversationsSnapshot()

        val remoteViews = RemoteViews(context.packageName, R.layout.widget)
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
    }

    /**
     * Returns the number of rows to show. If there are more conversations than the max count,
     * we'll return the max count + 1, where the last row just shows "View more conversations"
     */
    override fun getCount(): Int {
        val count = Math.min(conversations.size, MAX_CONVERSATIONS_COUNT)
        val shouldShowViewMore = count < conversations.size
        return count + if (shouldShowViewMore) 1 else 0
    }

    override fun getViewAt(position: Int): RemoteViews {
        return when {
            position >= MAX_CONVERSATIONS_COUNT -> getOverflowView()
            else -> getConversationView(position)
        }
    }

    private fun getConversationView(position: Int): RemoteViews {
        val conversation = conversations[position]

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_list_item)

        // Avatar
        remoteViews.setViewVisibility(R.id.avatar, if (smallWidget) View.GONE else View.VISIBLE)
        remoteViews.setInt(R.id.avatar, "setBackgroundColor", theme)
        remoteViews.setTextColor(R.id.initial, textPrimaryOnTheme)
        remoteViews.setInt(R.id.icon, "setColorFilter", textPrimaryOnTheme)
        remoteViews.setInt(R.id.avatarMask, "setColorFilter", background)

        val contact = conversation.recipients.map { recipient ->
            recipient.contact ?: Contact().apply { numbers.add(PhoneNumber().apply { address = recipient.address }) }
        }.firstOrNull()


        // Use the icon if there's no name, otherwise show an initial
        if (contact?.name.orEmpty().isNotEmpty()) {
            remoteViews.setTextViewText(R.id.initial, contact?.name?.substring(0, 1))
            remoteViews.setViewVisibility(R.id.icon, View.GONE)
        } else {
            remoteViews.setTextViewText(R.id.initial, null)
            remoteViews.setViewVisibility(R.id.icon, View.VISIBLE)
        }

        remoteViews.setImageViewBitmap(R.id.photo, null)
        contact?.numbers?.firstOrNull()?.address?.let { address ->
            val futureGet = GlideApp.with(context)
                    .asBitmap()
                    .load(PhoneNumberUtils.stripSeparators(address))
                    .submit(48.dpToPx(context), 48.dpToPx(context))

            try {
                remoteViews.setImageViewBitmap(R.id.photo, futureGet.get())
            } catch (e: Exception) {
            }
        }

        // Name
        remoteViews.setTextColor(R.id.name, textPrimary)
        remoteViews.setTextViewText(R.id.name, boldText(conversation.getTitle(), !conversation.read))

        // Date
        remoteViews.setTextColor(R.id.date, if (conversation.read) textTertiary else textPrimary)
        remoteViews.setTextViewText(R.id.date, boldText(dateFormatter.getConversationTimestamp(conversation.date), !conversation.read))

        // Snippet
        remoteViews.setTextColor(R.id.snippet, if (conversation.read) textTertiary else textPrimary)
        remoteViews.setTextViewText(R.id.snippet, boldText(conversation.snippet, !conversation.read))

        // Launch conversation on click
        val clickIntent = Intent().putExtra("threadId", conversation.id)
        remoteViews.setOnClickFillInIntent(R.id.conversation, clickIntent)

        return remoteViews
    }

    private fun getOverflowView(): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_loading)
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        view.setTextColor(R.id.loadingText, textSecondary)
        view.setTextViewText(R.id.loadingText, context.getString(R.string.widget_more))
        view.setOnClickPendingIntent(R.id.loading, pendingIntent)
        return view
    }

    private fun boldText(text: String, shouldBold: Boolean): CharSequence {
        return if (shouldBold) {
            SpannableString(text).apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
        } else {
            text
        }
    }

    override fun getLoadingView(): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_loading)
        view.setTextViewText(R.id.loadingText, context.getText(R.string.widget_loading))
        return view
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun onDestroy() {
    }

}
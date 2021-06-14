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
package repository

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.realm.RealmResults
import model.Conversation
import model.Message
import model.MmsPart

interface MessageRepository {

    fun getConversations(archived: Boolean = false): Flowable<List<Conversation>>

    fun getConversationsSnapshot(): List<Conversation>

    fun getBlockedConversations(): Flowable<List<Conversation>>

    fun getConversationAsync(threadId: Long): Conversation

    fun getConversation(threadId: Long): Conversation?

    fun getOrCreateConversation(threadId: Long): Conversation?

    fun getOrCreateConversation(address: String): Maybe<Conversation>

    fun getOrCreateConversation(addresses: List<String>): Maybe<Conversation>

    fun saveDraft(threadId: Long, draft: String)

    fun getMessages(threadId: Long): RealmResults<Message>

    fun getMessage(id: Long): Message?

    fun getMessageForPart(id: Long): Message?

    fun getUnreadCount(): Long

    fun getPart(id: Long): MmsPart?

    fun getPartsForConversation(threadId: Long): RealmResults<MmsPart>

    /**
     * Retrieves the list of messages which should be shown in the notification
     * for a given conversation
     */
    fun getUnreadUnseenMessages(threadId: Long): RealmResults<Message>

    /**
     * Retrieves the list of messages which should be shown in the quickreply popup
     * for a given conversation
     */
    fun getUnreadMessages(threadId: Long): RealmResults<Message>

    /**
     * Updates message-related fields in the conversation, like the date and snippet
     */
    fun updateConversation(threadId: Long)

    fun markArchived(threadId: Long)

    fun markUnarchived(threadId: Long)

    fun markBlocked(threadId: Long)

    fun markUnblocked(threadId: Long)

    fun markAllSeen()

    fun markSeen(threadId: Long)

    fun markRead(threadId: Long)

    /**
     * Persists the SMS and attempts to send it
     */
    fun sendSmsAndPersist(threadId: Long, address: String, body: String)

    /**
     * Attempts to send the SMS message. This can be called if the message has already been persisted
     */
    fun sendSms(message: Message)

    fun insertSentSms(threadId: Long, address: String, body: String): Message

    fun insertReceivedSms(address: String, body: String, sentTime: Long): Message

    /**
     * Marks the message as sending, in case we need to retry sending it
     */
    fun markSending(id: Long)

    fun markSent(id: Long)

    fun markFailed(id: Long, resultCode: Int)

    fun markDelivered(id: Long)

    fun markDeliveryFailed(id: Long, resultCode: Int)

    fun deleteMessage(messageId: Long)

    fun deleteConversation(threadId: Long)

}
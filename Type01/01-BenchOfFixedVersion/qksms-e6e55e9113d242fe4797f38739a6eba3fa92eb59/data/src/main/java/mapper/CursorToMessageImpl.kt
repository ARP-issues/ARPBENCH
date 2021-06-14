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
package mapper

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony.*
import com.google.android.mms.pdu_alt.PduHeaders
import manager.KeyManager
import model.Message
import util.extensions.map
import javax.inject.Inject

class CursorToMessageImpl @Inject constructor(
        private val context: Context,
        private val keys: KeyManager,
        private val cursorToPart: CursorToPart
) : CursorToMessage {

    companion object {
        val URI = Uri.parse("content://mms-sms/complete-conversations")
        val PROJECTION = arrayOf(
                MmsSms.TYPE_DISCRIMINATOR_COLUMN,
                MmsSms._ID,
                Mms.DATE,
                Mms.DATE_SENT,
                Mms.READ,
                Mms.THREAD_ID,
                Mms.LOCKED,

                Sms.ADDRESS,
                Sms.BODY,
                Sms.SEEN,
                Sms.TYPE,
                Sms.STATUS,
                Sms.ERROR_CODE,

                Mms.SUBJECT,
                Mms.SUBJECT_CHARSET,
                Mms.SEEN,
                Mms.MESSAGE_TYPE,
                Mms.MESSAGE_BOX,
                Mms.DELIVERY_REPORT,
                Mms.READ_REPORT,
                MmsSms.PendingMessages.ERROR_TYPE,
                Mms.STATUS)
    }

    override fun map(from: Pair<Cursor, CursorToMessage.MessageColumns>): Message {
        val cursor = from.first
        val columnsMap = from.second

        return Message().apply {
            type = when {
                cursor.getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN) != -1 -> cursor.getString(columnsMap.msgType)
                cursor.getColumnIndex(Mms.SUBJECT) != -1 -> "mms"
                cursor.getColumnIndex(Sms.ADDRESS) != -1 -> "sms"
                else -> "unknown"
            }

            id = keys.newId()
            threadId = cursor.getLong(columnsMap.threadId)
            contentId = cursor.getLong(columnsMap.msgId)
            date = cursor.getLong(columnsMap.date)
            dateSent = cursor.getLong(columnsMap.dateSent)
            read = cursor.getInt(columnsMap.read) != 0
            locked = cursor.getInt(columnsMap.locked) != 0

            when (type) {
                "sms" -> {
                    address = cursor.getString(columnsMap.smsAddress) ?: ""
                    boxId = cursor.getInt(columnsMap.smsType)
                    seen = cursor.getInt(columnsMap.smsSeen) != 0

                    body = columnsMap.smsBody
                            .takeIf { column -> column != -1 } // The column may not be set
                            ?.let { column -> cursor.getString(column) } ?: "" // cursor.getString() may return null

                    errorCode = cursor.getInt(columnsMap.smsErrorCode)
                    deliveryStatus = cursor.getInt(columnsMap.smsStatus)
                }

                "mms" -> {
                    address = getMmsAddress(contentId)
                    boxId = cursor.getInt(columnsMap.mmsMessageBox)
                    date *= 1000L
                    dateSent *= 1000L
                    seen = cursor.getInt(columnsMap.mmsSeen) != 0
                    mmsDeliveryStatusString = cursor.getString(columnsMap.mmsDeliveryReport) ?: ""
                    errorType = if (columnsMap.mmsErrorType != -1) cursor.getInt(columnsMap.mmsErrorType) else 0
                    messageSize = 0
                    readReportString = cursor.getString(columnsMap.mmsReadReport) ?: ""
                    messageType = cursor.getInt(columnsMap.mmsMessageType)
                    mmsStatus = cursor.getInt(columnsMap.mmsStatus)
                    subject = cursor.getString(columnsMap.mmsSubject) ?: ""
                    textContentType = ""
                    attachmentType = Message.AttachmentType.NOT_LOADED

                    parts.addAll(cursorToPart.getPartsCursor(contentId)?.map { cursorToPart.map(it) } ?: listOf())
                }
            }
        }
    }

    override fun getMessagesCursor(): Cursor {
        return context.contentResolver.query(URI, PROJECTION, null, null, "normalized_date desc")
    }

    override fun getMessageCursor(id: Long): Cursor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getMmsAddress(messageId: Long): String {
        val uri = Mms.CONTENT_URI.buildUpon()
                .appendPath(messageId.toString())
                .appendPath("addr").build()

        //TODO: Use Charset to ensure address is decoded correctly
        val projection = arrayOf(Mms.Addr.ADDRESS, Mms.Addr.CHARSET)
        val selection = "${Mms.Addr.TYPE} = ${PduHeaders.FROM}"

        val cursor = context.contentResolver.query(uri, projection, selection, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }

        return ""
    }

}

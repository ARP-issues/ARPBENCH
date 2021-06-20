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

package org.kontalk.service.msgcenter;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jxmpp.jid.Jid;
import org.jxmpp.stringprep.XmppStringprepException;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.kontalk.Kontalk;
import org.kontalk.Log;
import org.kontalk.authenticator.Authenticator;
import org.kontalk.client.BitsOfBinary;
import org.kontalk.client.E2EEncryption;
import org.kontalk.client.EndpointServer;
import org.kontalk.client.GroupExtension;
import org.kontalk.client.KontalkGroupManager;
import org.kontalk.client.OpenPGPSignedMessage;
import org.kontalk.client.OutOfBandData;
import org.kontalk.client.UserLocation;
import org.kontalk.crypto.Coder;
import org.kontalk.crypto.DecryptException;
import org.kontalk.crypto.PersonalKey;
import org.kontalk.crypto.VerifyException;
import org.kontalk.data.Contact;
import org.kontalk.data.GroupInfo;
import org.kontalk.message.AudioComponent;
import org.kontalk.message.CompositeMessage;
import org.kontalk.message.DefaultAttachmentComponent;
import org.kontalk.message.GroupCommandComponent;
import org.kontalk.message.GroupComponent;
import org.kontalk.message.ImageComponent;
import org.kontalk.message.InReplyToComponent;
import org.kontalk.message.LocationComponent;
import org.kontalk.message.MessageComponent;
import org.kontalk.message.RawComponent;
import org.kontalk.message.ReferencedMessage;
import org.kontalk.message.TextComponent;
import org.kontalk.message.VCardComponent;
import org.kontalk.provider.Keyring;
import org.kontalk.provider.MessagesProviderClient;
import org.kontalk.provider.MessagesProviderClient.MessageUpdater;
import org.kontalk.provider.MyMessages;
import org.kontalk.provider.MyMessages.Messages;
import org.kontalk.reporting.ReportingManager;
import org.kontalk.service.msgcenter.group.KontalkGroupController;
import org.kontalk.util.MediaStorage;
import org.kontalk.util.MessageUtils;
import org.kontalk.util.Preferences;
import org.kontalk.util.XMPPUtils;

import static org.kontalk.crypto.DecryptException.DECRYPT_EXCEPTION_INVALID_TIMESTAMP;
import static org.kontalk.service.msgcenter.MessageCenterService.ACTION_MESSAGE;
import static org.kontalk.service.msgcenter.MessageCenterService.EXTRA_CHAT_STATE;
import static org.kontalk.service.msgcenter.MessageCenterService.EXTRA_FROM;
import static org.kontalk.service.msgcenter.MessageCenterService.EXTRA_GROUP_JID;
import static org.kontalk.service.msgcenter.MessageCenterService.EXTRA_TO;


/**
 * Packet listener for message stanzas.
 * @author Daniele Ricci
 */
class MessageListener extends WakefulMessageCenterPacketListener {

    public MessageListener(MessageCenterService instance) {
        super(instance, "-RECV");
    }

    private boolean processGroupMessage(KontalkGroupManager.KontalkGroup group, Stanza packet, CompositeMessage msg, Intent chatStateBroadcast) {

        if (group.checkRequest(packet) && canHandleGroupCommand(packet)) {
            GroupExtension ext = GroupExtension.from(packet);
            String groupJid = ext.getJID();
            String subject = ext.getSubject();

            // group information
            GroupInfo groupInfo = new GroupInfo(groupJid, subject,
                KontalkGroupController.GROUP_TYPE, MyMessages.Groups.MEMBERSHIP_MEMBER);
            msg.addComponent(new GroupComponent(groupInfo));

            // group typing information
            if (chatStateBroadcast != null) {
                chatStateBroadcast.putExtra(EXTRA_GROUP_JID, groupJid);
            }

            if (ext.getType() == GroupExtension.Type.CREATE ||
                    ext.getType() == GroupExtension.Type.PART ||
                    ext.getType() == GroupExtension.Type.SET) {
                GroupCommandComponent groupCmd = new GroupCommandComponent(ext,
                    packet.getFrom().asBareJid().toString(),
                    Authenticator.getSelfJID(getContext()));
                msg.addComponent(groupCmd);
            }

            return true;
        }

        // invalid or unauthorized request
        return false;
    }

    /** Returns true if we can handle this group command (e.g. if we have it in our database). */
    private boolean canHandleGroupCommand(Stanza packet) {
        GroupExtension ext = GroupExtension.from(packet);
        // creation command
        return ext.getType() == GroupExtension.Type.CREATE ||
            // is the owner adding me to the group?
            isAddingMe(ext) ||
            // check that the sender has valid membership
            (isValidMember(ext, packet.getFrom()) &&
            // all other commands require the group to be present in our database
            MessagesProviderClient.isGroupExisting(getContext(), ext.getJID()));
    }

    /** Returns true if the given group command is the owner adding me to the group. */
    private boolean isAddingMe(GroupExtension ext) {
        if (ext.getType() == GroupExtension.Type.SET) {
            String myself = Authenticator.getSelfJID(getContext());
            for (GroupExtension.Member m : ext.getMembers()) {
                if (m.operation == GroupExtension.Member.Operation.ADD &&
                        m.jid.equalsIgnoreCase(myself))
                    return true;
            }
        }
        return false;
    }

    private boolean isValidMember(GroupExtension ext, Jid from) {
        return MessagesProviderClient.isGroupMember(getContext(),
            ext.getJID(), from.asBareJid().toString());
    }

    @Override
    protected void processWakefulStanza(Stanza packet) throws SmackException.NotConnectedException {
        org.jivesoftware.smack.packet.Message m = (org.jivesoftware.smack.packet.Message) packet;

        if (m.getType() == org.jivesoftware.smack.packet.Message.Type.chat) {
            // a preliminary object is created here
            // other info will be filled in by processChatMessage
            Intent chatStateBroadcast = processChatState(m);

            // non-active chat states are not to be processed as messages
            if (chatStateBroadcast == null || ChatState.active.name().equals(chatStateBroadcast.getStringExtra(EXTRA_CHAT_STATE))) {
                processChatMessage(m, chatStateBroadcast);
            }

            if (chatStateBroadcast != null) {
                // we can send the chat state broadcast now
                sendBroadcast(chatStateBroadcast);
            }
        }

        // error message
        else if (m.getType() == org.jivesoftware.smack.packet.Message.Type.error) {
            processErrorMessage(m);
        }
    }

    /**
     * Retrieve the group JID from a message. Must not be encrypted.
     * Used mainly for chat states.
     */
    private String getGroupJid(Message m) {
        // group chat
        KontalkGroupManager.KontalkGroup group;
        try {
            group = KontalkGroupManager
                .getInstanceFor(getConnection()).getGroup(m);
        }
        catch (XmppStringprepException e) {
            Log.w(TAG, "error parsing JID: " + e.getCausingString(), e);
            // report it because it's a big deal
            ReportingManager.logException(e);
            return null;

        }
        if (group != null && group.checkRequest(m) && canHandleGroupCommand(m)) {
            GroupExtension ext = GroupExtension.from(m);
            String groupJid = ext.getJID();
            String subject = ext.getSubject();

            // group information
            GroupInfo groupInfo = new GroupInfo(groupJid, subject,
                KontalkGroupController.GROUP_TYPE, MyMessages.Groups.MEMBERSHIP_MEMBER);
            return groupInfo.getJid();
        }

        return null;
    }

    private Intent processChatState(Message m) {
        // check if there is a composing notification
        ExtensionElement _chatstate = m.getExtension("http://jabber.org/protocol/chatstates");
        if (_chatstate != null) {
            ChatStateExtension chatstate = (ChatStateExtension) _chatstate;

            Jid from = m.getFrom();
            Intent i = new Intent(ACTION_MESSAGE);
            i.putExtra(EXTRA_CHAT_STATE, chatstate.getElementName());
            i.putExtra(EXTRA_FROM, from.toString());
            i.putExtra(EXTRA_TO, m.getTo().toString());

            String groupJid = getGroupJid(m);
            if (groupJid != null) {
                i.putExtra(EXTRA_GROUP_JID, groupJid);
            }

            return i;
        }

        return null;
    }

    /**
     * Process an incoming message packet.
     * @param m the message
     * @param chatStateBroadcast a chat state broadcast that will be filled with missing information (e.g. group info in encrypted message)
     */
    private void processChatMessage(Message m, Intent chatStateBroadcast) throws SmackException.NotConnectedException {
        // delayed deliver extension is the first the be processed
        // because it's used also in delivery receipts
        Date stamp = XMPPUtils.getStanzaDelay(m);

        long serverTimestamp;
        if (stamp != null)
            serverTimestamp = stamp.getTime();
        else
            serverTimestamp = System.currentTimeMillis();

        DeliveryReceipt deliveryReceipt = DeliveryReceipt.from(m);

        // delivery receipt
        if (deliveryReceipt != null) {
            MessageUpdater.forMessage(getContext(), deliveryReceipt.getId(), false)
                .setStatus(Messages.STATUS_RECEIVED, System.currentTimeMillis())
                .commit();
        }

        // incoming message
        else {
            String msgId = m.getStanzaId();
            if (msgId == null)
                msgId = MessageUtils.messageId();

            Jid from = m.getFrom();
            String body = m.getBody();

            // create message
            CompositeMessage msg = new CompositeMessage(
                getContext(),
                msgId,
                serverTimestamp,
                from.toString(),
                false,
                Coder.SECURITY_CLEARTEXT
            );

            // ack request might not be encrypted
            boolean needAck = m.hasExtension(DeliveryReceiptRequest.ELEMENT, DeliveryReceipt.NAMESPACE);

            ExtensionElement _encrypted = m.getExtension(E2EEncryption.ELEMENT_NAME, E2EEncryption.NAMESPACE);

            if (_encrypted != null && _encrypted instanceof E2EEncryption) {
                E2EEncryption mEnc = (E2EEncryption) _encrypted;
                byte[] encryptedData = mEnc.getData();

                // encrypted message
                msg.setEncrypted(true);
                msg.setSecurityFlags(Coder.SECURITY_BASIC);

                if (encryptedData != null) {

                    // decrypt message
                    try {
                        Message innerStanza = decryptMessage(msg, encryptedData);
                        if (innerStanza != null) {
                            // copy some attributes over
                            innerStanza.setTo(m.getTo());
                            innerStanza.setFrom(m.getFrom());
                            innerStanza.setType(m.getType());
                            m = innerStanza;

                            if (!needAck) {
                                // try the decrypted message
                                needAck = m.hasExtension(DeliveryReceiptRequest.ELEMENT, DeliveryReceipt.NAMESPACE);
                            }
                        }
                    }

                    catch (Exception exc) {
                        Log.e(MessageCenterService.TAG, "decryption failed", exc);

                        // raw component for encrypted data
                        // reuse security flags
                        msg.clearComponents();
                        msg.addComponent(new RawComponent(encryptedData, true, msg.getSecurityFlags()));
                    }

                }
            }

            else {

                // use message body
                if (body != null)
                    msg.addComponent(new TextComponent(body));

                // old PGP signature
                ExtensionElement _pgpSigned = m.getExtension(OpenPGPSignedMessage.ELEMENT_NAME, OpenPGPSignedMessage.NAMESPACE);
                if (_pgpSigned instanceof OpenPGPSignedMessage) {
                    OpenPGPSignedMessage pgpSigned = (OpenPGPSignedMessage) _pgpSigned;
                    byte[] signedData = pgpSigned.getData();

                    // signed message
                    msg.setSecurityFlags(Coder.SECURITY_BASIC_SIGNED);

                    if (signedData != null) {
                        // check signature
                        try {
                            checkSignedMessage(msg, pgpSigned.getData());
                            // at this point our message should be filled with the verified body
                        }

                        catch (Exception exc) {
                            Log.e(MessageCenterService.TAG, "signature check failed", exc);
                            // TODO what to do here?
                            msg.setSecurityFlags(msg.getSecurityFlags() |
                                Coder.SECURITY_ERROR_INVALID_SIGNATURE);
                        }
                    }
                }

            }

            // out of band data
            ExtensionElement _media = m.getExtension(OutOfBandData.ELEMENT_NAME, OutOfBandData.NAMESPACE);
            if (_media instanceof OutOfBandData) {
                File previewFile = null;

                OutOfBandData media = (OutOfBandData) _media;
                String mime = media.getMime();
                String fetchUrl = media.getUrl();
                long length = media.getLength();
                boolean encrypted = media.isEncrypted();

                // bits-of-binary for preview
                ExtensionElement _preview = m.getExtension(BitsOfBinary.ELEMENT_NAME, BitsOfBinary.NAMESPACE);
                if (_preview != null && _preview instanceof BitsOfBinary) {
                    BitsOfBinary preview = (BitsOfBinary) _preview;
                    String previewMime = preview.getType();
                    if (previewMime == null)
                        previewMime = MediaStorage.THUMBNAIL_MIME_NETWORK;

                    String filename = null;

                    if (ImageComponent.supportsMimeType(previewMime)) {
                        filename = ImageComponent.buildMediaFilename(previewMime);
                    }

                    try {
                        if (filename != null) previewFile =
                            MediaStorage.writeInternalMedia(getContext(),
                                filename, preview.getContents());
                    }
                    catch (IOException e) {
                        Log.w(MessageCenterService.TAG, "error storing thumbnail", e);
                        // we are going to need a filename anyway
                        previewFile = MediaStorage.getInternalMediaFile(getContext(), filename);
                    }
                }

                MessageComponent<?> attachment;

                if (mime == null) {
                    // try to guess MIME from URL
                    mime = MediaStorage.getType(fetchUrl);
                }

                if (ImageComponent.supportsMimeType(mime)) {
                    if (previewFile == null) {
                        // no bits of binary, generate a filename anyway so the thumbnail will be generated
                        // from the original file once downloaded
                        String filename = ImageComponent.buildMediaFilename(mime);
                        previewFile = MediaStorage.getInternalMediaFile(getContext(), filename);
                    }

                    msg.clearComponents();
                    // cleartext only for now
                    attachment = new ImageComponent(mime, previewFile, null, fetchUrl, length,
                        encrypted, encrypted ? Coder.SECURITY_BASIC : Coder.SECURITY_CLEARTEXT);
                }

                else if (VCardComponent.supportsMimeType(mime)) {
                    msg.clearComponents();
                    // cleartext only for now
                    attachment = new VCardComponent(previewFile, null, fetchUrl, length,
                        encrypted, encrypted ? Coder.SECURITY_BASIC : Coder.SECURITY_CLEARTEXT);
                }

                else if (AudioComponent.supportsMimeType(mime)) {
                    msg.clearComponents();
                    attachment = new AudioComponent(mime, null, fetchUrl, length,
                        encrypted, encrypted ? Coder.SECURITY_BASIC : Coder.SECURITY_CLEARTEXT);
                }

                else {
                    msg.clearComponents();
                    attachment = new DefaultAttachmentComponent(mime, null, fetchUrl, length,
                        encrypted, encrypted ? Coder.SECURITY_BASIC : Coder.SECURITY_CLEARTEXT);
                }

                // TODO other types

                msg.addComponent(attachment);

                // add a dummy body if none was found
                /*
                if (body == null) {
                    msg.addComponent(new TextComponent(CompositeMessage
                        .getSampleTextContent((Class<? extends MessageComponent<?>>)
                            attachment.getClass(), mime)));
                }
                */
            }

            ExtensionElement _location = m.getExtension(UserLocation.ELEMENT_NAME, UserLocation.NAMESPACE);
            if (_location != null && _location instanceof UserLocation) {
                UserLocation location = (UserLocation) _location;
                msg.addComponent(new LocationComponent(location.getLatitude(),
                    location.getLongitude(), location.getText(), location.getStreet()));
            }

            ExtensionElement _fwd = m.getExtension(Forwarded.ELEMENT, Forwarded.NAMESPACE);
            if (_fwd != null && _fwd instanceof Forwarded) {
                // we actually use only the stanza id for looking up the referenced message in our database.
                // The forwarded stanza was included for compatibility with other XMPP clients.
                // Although technically it's a waste of space, and the replied message will
                // not be displayed if it is deleted
                Forwarded fwd = (Forwarded) _fwd;

                Stanza fwdMsg = fwd.getForwardedStanza();
                if (fwdMsg != null && fwdMsg.getStanzaId() != null) {
                    ReferencedMessage referencedMsg = ReferencedMessage
                        .load(getContext(), fwdMsg.getStanzaId());
                    if (referencedMsg != null)
                        msg.addComponent(new InReplyToComponent(referencedMsg));
                }
            }

            // group chat
            KontalkGroupManager.KontalkGroup group;
            try {
                group = KontalkGroupManager
                    .getInstanceFor(getConnection()).getGroup(m);
            }
            catch (XmppStringprepException e) {
                Log.w(TAG, "error parsing JID: " + e.getCausingString(), e);
                // report it because it's a big deal
                ReportingManager.logException(e);
                return;

            }
            if (group != null && !processGroupMessage(group, m, msg, chatStateBroadcast)) {
                // invalid group command
                Log.w(TAG, "invalid or unauthorized group command");
                return;
            }

            if (msg.getComponents().size() == 0) {
                Log.w(TAG, "message has no content, discarding");
                return;
            }

            // 1-to-1 message with a chat state
            // set contact as typing if necessary
            if (!msg.hasComponent(GroupComponent.class) && chatStateBroadcast != null) {
                Contact.setTyping(from.toString(),
                    ChatState.composing.name().equals(chatStateBroadcast.getStringExtra(EXTRA_CHAT_STATE)));
            }

            msg.setStatus(needAck ? Messages.STATUS_INCOMING : Messages.STATUS_CONFIRMED);

            Uri msgUri = incoming(msg);

            if (needAck) {
                // send ack :)
                sendReceipt(msgUri, msgId, from);
            }
        }
    }

    private void processErrorMessage(Message m) {
        DeliveryReceipt deliveryReceipt = DeliveryReceipt.from(m);

        // delivery receipt error
        if (deliveryReceipt != null) {
            // mark indicated message as incoming and try again
            MessageUpdater.forMessage(getContext(), deliveryReceipt.getId(), true)
                .setStatus(Messages.STATUS_INCOMING, System.currentTimeMillis())
                .commit();

            // send receipt again
            sendReceipt(null, deliveryReceipt.getId(), m.getFrom());
        }

        String id = m.getStanzaId();
        if (id != null) {
            MessageUpdater.forMessage(getContext(), m.getStanzaId(), false)
                .setStatus(Messages.STATUS_NOTDELIVERED, System.currentTimeMillis())
                .commit();
        }
    }

    private void sendReceipt(Uri msgUri, String msgId, Jid from) {
        DeliveryReceipt receipt = new DeliveryReceipt(msgId);
        org.jivesoftware.smack.packet.Message ack =
            new org.jivesoftware.smack.packet.Message(from,
                org.jivesoftware.smack.packet.Message.Type.chat);
        ack.addExtension(receipt);

        long storageId = 0;
        if (msgUri != null) {
            // will use this to mark this message as confirmed
            storageId = ContentUris.parseId(msgUri);
        }

        sendMessage(ack, storageId);
    }

    private Message decryptMessage(CompositeMessage msg, byte[] encryptedData) throws Exception {
        // message stanza
        Message m = null;

        try {
            Context context = getContext();
            PersonalKey key = Kontalk.get(context).getPersonalKey();

            EndpointServer server = getServer();
            if (server == null)
                server = Preferences.getEndpointServer(context);

            Coder coder = Keyring.getDecryptCoder(context, server, key, msg.getSender(true));

            // decrypt
            Coder.DecryptOutput result = coder.decryptText(encryptedData, true);

            String contentText;

            if (XMPPUtils.XML_XMPP_TYPE.equalsIgnoreCase(result.mime)) {
                m = XMPPUtils.parseMessageStanza(result.cleartext);

                if (result.timestamp != null && !checkDriftedDelay(m, result.timestamp))
                    result.errors.add(new DecryptException(DECRYPT_EXCEPTION_INVALID_TIMESTAMP,
                        "Drifted timestamp"));

                contentText = m.getBody();
            }
            else {
                contentText = result.cleartext;
            }

            // clear components (we are adding new ones)
            msg.clearComponents();
            // decrypted text
            if (contentText != null)
                msg.addComponent(new TextComponent(contentText));

            if (result.errors.size() > 0) {

                int securityFlags = msg.getSecurityFlags();

                for (DecryptException err : result.errors) {

                    int code = err.getCode();
                    switch (code) {

                        case DecryptException.DECRYPT_EXCEPTION_INTEGRITY_CHECK:
                            securityFlags |= Coder.SECURITY_ERROR_INTEGRITY_CHECK;
                            break;

                        case DecryptException.DECRYPT_EXCEPTION_VERIFICATION_FAILED:
                            securityFlags |= Coder.SECURITY_ERROR_INVALID_SIGNATURE;
                            break;

                        case DecryptException.DECRYPT_EXCEPTION_INVALID_DATA:
                            securityFlags |= Coder.SECURITY_ERROR_INVALID_DATA;
                            break;

                        case DecryptException.DECRYPT_EXCEPTION_INVALID_SENDER:
                            securityFlags |= Coder.SECURITY_ERROR_INVALID_SENDER;
                            break;

                        case DecryptException.DECRYPT_EXCEPTION_INVALID_RECIPIENT:
                            securityFlags |= Coder.SECURITY_ERROR_INVALID_RECIPIENT;
                            break;

                        case DecryptException.DECRYPT_EXCEPTION_INVALID_TIMESTAMP:
                            securityFlags |= Coder.SECURITY_ERROR_INVALID_TIMESTAMP;
                            break;

                    }

                }

                msg.setSecurityFlags(securityFlags);
            }

            msg.setEncrypted(false);

            return m;
        }
        catch (Exception exc) {
            // pass over the message even if encrypted
            // UI will warn the user about that and wait
            // for user decisions
            int securityFlags = msg.getSecurityFlags();

            if (exc instanceof DecryptException) {

                int code = ((DecryptException) exc).getCode();
                switch (code) {

                    case DecryptException.DECRYPT_EXCEPTION_DECRYPT_FAILED:
                    case DecryptException.DECRYPT_EXCEPTION_PRIVATE_KEY_NOT_FOUND:
                        securityFlags |= Coder.SECURITY_ERROR_DECRYPT_FAILED;
                        break;

                    case DecryptException.DECRYPT_EXCEPTION_INTEGRITY_CHECK:
                        securityFlags |= Coder.SECURITY_ERROR_INTEGRITY_CHECK;
                        break;

                    case DecryptException.DECRYPT_EXCEPTION_INVALID_DATA:
                        securityFlags |= Coder.SECURITY_ERROR_INVALID_DATA;
                        break;

                }

                msg.setSecurityFlags(securityFlags);
            }

            throw exc;
        }
    }

    private void checkSignedMessage(CompositeMessage msg, byte[] signedData) throws Exception {
        try {
            Context context = getContext();
            EndpointServer server = getServer();
            if (server == null)
                server = Preferences.getEndpointServer(context);

            // retrieve a coder for verifying against the server key
            Coder coder = Keyring.getVerifyCoder(context, server, msg.getSender(true));

            // decrypt
            Coder.VerifyOutput result = coder.verifyText(signedData, true);
            String contentText = result.cleartext;

            // clear components (we are adding new ones)
            msg.clearComponents();
            // decrypted text
            if (contentText != null)
                msg.addComponent(new TextComponent(contentText));

            if (result.errors.size() > 0) {

                int securityFlags = msg.getSecurityFlags();

                for (VerifyException err : result.errors) {

                    int code = err.getCode();
                    switch (code) {

                        case VerifyException.VERIFY_EXCEPTION_VERIFICATION_FAILED:
                            securityFlags |= Coder.SECURITY_ERROR_INVALID_SIGNATURE;
                            break;

                        case VerifyException.VERIFY_EXCEPTION_INVALID_DATA:
                            securityFlags |= Coder.SECURITY_ERROR_INVALID_DATA;
                            break;

                    }

                }

                msg.setSecurityFlags(securityFlags);
            }
        }
        catch (Exception exc) {
            // pass over the message even if encrypted
            // UI will warn the user about that and wait
            // for user decisions
            int securityFlags = msg.getSecurityFlags();

            if (exc instanceof VerifyException) {

                int code = ((VerifyException) exc).getCode();
                switch (code) {

                    case VerifyException.VERIFY_EXCEPTION_INVALID_DATA:
                        securityFlags |= Coder.SECURITY_ERROR_INVALID_DATA;
                        break;

                }

                msg.setSecurityFlags(securityFlags);
            }

            throw exc;
        }
    }

    private static boolean checkDriftedDelay(Message m, Date expected) {
        Date stamp = XMPPUtils.getStanzaDelay(m);
        if (stamp != null) {
            long time = stamp.getTime();
            long now = expected.getTime();
            long diff = Math.abs(now - time);
            return (diff < Coder.TIMEDIFF_THRESHOLD);
        }

        // no timestamp found
        return true;
    }

}

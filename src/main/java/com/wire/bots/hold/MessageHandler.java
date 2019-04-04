package com.wire.bots.hold;

import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.AttachmentMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.tools.Logger;

import java.util.UUID;

public class MessageHandler extends MessageHandlerBase {
    private final Database db;
    private final Database._Access owner;

    MessageHandler(Database._Access owner, Database db) {
        this.db = db;
        this.owner = owner;
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID messageId = UUID.fromString(msg.getMessageId());
        UUID conversationId = UUID.fromString(msg.getConversationId());
        UUID senderId = UUID.fromString(msg.getUserId());
        String clientId = msg.getClientId();
        Logger.info("onText(%s:%s) sender %s:%s",
                owner.userId,
                owner.clientId,
                senderId,
                clientId);

        try {
            db.insertTextRecord(conversationId, messageId, senderId, msg.getText());
        } catch (Exception e) {
            Logger.error("OnText: %s %s ex: %s", conversationId, messageId, e);
        }
    }

    public void onImage(WireClient client, ImageMessage msg) {
        UUID messageId = UUID.fromString(msg.getMessageId());
        UUID conversationId = UUID.fromString(msg.getConversationId());
        UUID senderId = UUID.fromString(msg.getUserId());
        String uri = null;
        Logger.debug("onImage: %s %s %s %s %s",
                conversationId,
                messageId,
                senderId,
                msg.getName(),
                msg.getMimeType());
        try {
            db.insertAssetRecord(conversationId,
                    messageId,
                    senderId,
                    msg.getMimeType(),
                    uri);
        } catch (Exception e) {
            Logger.error("onImage: %s %s %s", conversationId, messageId, e);
        }
    }

    @Override
    public void onAttachment(WireClient client, AttachmentMessage msg) {
        UUID messageId = UUID.fromString(msg.getMessageId());
        UUID conversationId = UUID.fromString(msg.getConversationId());
        UUID senderId = UUID.fromString(msg.getUserId());
        String uri = null;
        Logger.debug("onAttachment: %s %s %s %s %s",
                conversationId,
                messageId,
                senderId,
                msg.getName(),
                msg.getMimeType());
        try {
            db.insertAssetRecord(conversationId,
                    messageId,
                    senderId,
                    msg.getMimeType(),
                    uri);
        } catch (Exception e) {
            Logger.error("onAttachment: %s %s %s", conversationId, messageId, e);
        }
    }
}

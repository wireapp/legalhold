package com.wire.bots.hold;

import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.AttachmentMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.tools.Logger;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.util.UUID;

public class MessageHandler extends MessageHandlerBase {
    private final Database db;

    MessageHandler(Database db) {
        this.db = db;
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID messageId = UUID.fromString(msg.getMessageId());
        UUID conversationId = UUID.fromString(msg.getConversationId());
        UUID senderId = UUID.fromString(msg.getUserId());
        String userId = client.getId();
        String text = msg.getText();
        String time = msg.getTime();

        Logger.info("onText %s -> %s msg:%s at %s",
                senderId,
                userId,
                messageId,
                time);
        try {
            boolean insertTextRecord = db.insertTextRecord(conversationId, messageId, senderId, time, text);
            if (!insertTextRecord) {
                String error = String.format("Failed to persist msg: %s, userId: %s, senderId: %s",
                        messageId,
                        userId,
                        senderId);
                throw new RuntimeException(error);
            }
        } catch (PSQLException e) {
            Logger.debug("onText: msg: %s code: %d, %s", messageId, e.getErrorCode(), e);
            ServerErrorMessage err = e.getServerErrorMessage();
            String constraint = err.getConstraint();
            if (constraint == null || !constraint.equals("hold_pkey")) {
                String error = String.format("OnText: %s %s ex: %s", userId, messageId, e);
                throw new RuntimeException(error);
            }
        } catch (Exception e) {
            String error = String.format("OnText: %s %s ex: %s", userId, messageId, e);
            throw new RuntimeException(error);
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

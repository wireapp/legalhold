package com.wire.bots.hold;

import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.AttachmentMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.tools.Logger;

import java.util.ArrayList;
import java.util.UUID;

public class MessageHandler extends MessageHandlerBase {
    private final Database db;

    MessageHandler(Database db) {
        this.db = db;
    }

    @Override
    public void onNewConversation(WireClient client) {
        UUID conversationId = client.getConversationId();
        String userId = client.getId();

        try {
            Logger.info("onNewConversation: user: %s, conv: %s", userId, conversationId);
        } catch (Exception e) {
            String error = String.format("onNewConversation: %s ex: %s", userId, e);
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
        UUID conversationId = client.getConversationId();
        String userId = client.getId();

        try {
            for (String memberId : userIds) {
                Logger.info("onMemberJoin: %s, user: %s, member: %s",
                        conversationId,
                        userId,
                        memberId);
            }
        } catch (Exception e) {
            String error = String.format("onMemberJoin: %s ex: %s", userId, e);
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onMemberLeave(WireClient client, ArrayList<String> userIds) {
        UUID conversationId = client.getConversationId();
        String userId = client.getId();

        try {
            for (String memberId : userIds) {
                Logger.info("onMemberLeave: %s, user: %s, member: %s",
                        conversationId,
                        userId,
                        memberId);
            }
        } catch (Exception e) {
            String error = String.format("onMemberLeave: %s ex: %s", userId, e);
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID messageId = UUID.fromString(msg.getMessageId());
        UUID conversationId = UUID.fromString(msg.getConversationId());
        UUID senderId = UUID.fromString(msg.getUserId());
        String userId = client.getId();
        String text = msg.getText();
        String time = msg.getTime();

        Logger.info("onText %s, %s -> %s msg:%s at %s",
                conversationId,
                senderId,
                userId,
                messageId,
                time);
//        try {
//            boolean insertTextRecord = db.insertTextRecord(conversationId, messageId, senderId, time, text);
//            if (!insertTextRecord) {
//                String error = String.format("Failed to persist msg: %s, userId: %s, senderId: %s",
//                        messageId,
//                        userId,
//                        senderId);
//                throw new RuntimeException(error);
//            }
//        } catch (PSQLException e) {
//            Logger.debug("onText: msg: %s code: %d, %s", messageId, e.getErrorCode(), e);
//            ServerErrorMessage err = e.getServerErrorMessage();
//            String constraint = err.getConstraint();
//            if (constraint == null || !constraint.equals("hold_pkey")) {
//                String error = String.format("OnText: %s %s ex: %s", userId, messageId, e);
//                throw new RuntimeException(error);
//            }
//        } catch (Exception e) {
//            String error = String.format("OnText: %s %s ex: %s", userId, messageId, e);
//            throw new RuntimeException(error);
//        }
    }

    public void onImage(WireClient client, ImageMessage msg) {
        UUID messageId = UUID.fromString(msg.getMessageId());
        UUID conversationId = UUID.fromString(msg.getConversationId());
        UUID senderId = UUID.fromString(msg.getUserId());
        String userId = client.getId();
        String time = msg.getTime();
        Logger.info("onImage: %s, %s -> %s, %s %s msg: %s, time: %s",
                conversationId,
                senderId,
                userId,
                msg.getName(),
                msg.getMimeType(),
                messageId,
                time);
//        try {
//            db.insertAssetRecord(conversationId,
//                    messageId,
//                    senderId,
//                    msg.getMimeType(),
//                    uri);
//        } catch (Exception e) {
//            Logger.error("onImage: %s %s %s", conversationId, messageId, e);
//        }
    }

    @Override
    public void onAttachment(WireClient client, AttachmentMessage msg) {
        UUID messageId = UUID.fromString(msg.getMessageId());
        UUID conversationId = UUID.fromString(msg.getConversationId());
        UUID senderId = UUID.fromString(msg.getUserId());
        String userId = client.getId();
        String time = msg.getTime();
        Logger.info("onAttachment: %s %s -> %s, %s %s msg: %s, time: %s",
                conversationId,
                senderId,
                userId,
                msg.getName(),
                msg.getMimeType(),
                messageId,
                time);
//        try {
//            db.insertAssetRecord(conversationId,
//                    messageId,
//                    senderId,
//                    msg.getMimeType(),
//                    uri);
//        } catch (Exception e) {
//            Logger.error("onAttachment: %s %s %s", conversationId, messageId, e);
//        }
    }

    @Override
    public void onCalling(WireClient client, String userId, String clientId, String content) {
        Logger.info("onCalling: %s, %s -> %s",
                client.getConversationId(),
                userId,
                client.getId());
    }

    @Override
    public boolean onConnectRequest(WireClient client, UUID from, UUID to, String status) {
        return false;
    }

    @Override
    public void validatePreKeys(WireClient client, int size) {

    }
}
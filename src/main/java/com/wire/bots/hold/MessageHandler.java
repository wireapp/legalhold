package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.AttachmentMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.tools.Logger;

import java.util.ArrayList;
import java.util.UUID;

public class MessageHandler extends MessageHandlerBase {
    private final EventsDAO eventsDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    MessageHandler(EventsDAO eventsDAO) {
        this.eventsDAO = eventsDAO;
    }

    @Override
    public void onNewConversation(WireClient client) {
        UUID conversationId = client.getConversationId();
        String userId = client.getId();
        try {
            Logger.info("onNewConversation: user: %s, conv: %s", userId, conversationId);

            Conversation conversation = client.getConversation();
            String payload = mapper.writeValueAsString(conversation);
            eventsDAO.insert(UUID.randomUUID(), conversationId, "conversation.create", payload); //todo UUID.randomUUID()
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

            Conversation conversation = client.getConversation();
            String payload = mapper.writeValueAsString(conversation);
            eventsDAO.insert(UUID.randomUUID(), conversationId, "conversation.member-join", payload); //todo UUID.randomUUID()
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

            Conversation conversation = client.getConversation();
            String payload = mapper.writeValueAsString(conversation);
            eventsDAO.insert(UUID.randomUUID(), conversationId, "conversation.member-leave", payload); //todo UUID.randomUUID()
        } catch (Exception e) {
            String error = String.format("onMemberLeave: %s ex: %s", userId, e);
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID conversationId = client.getConversationId();
        String userId = client.getId();

        UUID messageId = UUID.fromString(msg.getMessageId());
        UUID senderId = UUID.fromString(msg.getUserId());
        String time = msg.getTime();

        Logger.info("onText %s, %s -> %s msg:%s at %s",
                conversationId,
                senderId,
                userId,
                messageId,
                time);

        try {
            String payload = mapper.writeValueAsString(msg);
            int insert = eventsDAO.insert(messageId, conversationId, "conversation.otr-message-add.new-text", payload);
            if (0 == insert) {
                String error = String.format("Failed to persist txt msg: %s, userId: %s, senderId: %s",
                        messageId,
                        userId,
                        senderId);
                throw new RuntimeException(error);
            }
        } catch (Exception e) {
            String error = String.format("OnText: %s %s ex: %s", userId, messageId, e);
            throw new RuntimeException(error);
        }
    }

    public void onImage(WireClient client, ImageMessage msg) {
        UUID conversationId = client.getConversationId();
        String userId = client.getId();

        UUID messageId = UUID.fromString(msg.getMessageId());
        UUID senderId = UUID.fromString(msg.getUserId());
        String time = msg.getTime();
        Logger.info("onImage: %s, %s -> %s, %s %s msg: %s, time: %s",
                conversationId,
                senderId,
                userId,
                msg.getName(),
                msg.getMimeType(),
                messageId,
                time);
        try {
            String payload = mapper.writeValueAsString(msg);
            int insert = eventsDAO.insert(messageId, conversationId, "conversation.otr-message-add.new-image", payload);
            if (0 == insert) {
                String error = String.format("Failed to persist image msg: %s, userId: %s, senderId: %s",
                        messageId,
                        userId,
                        senderId);
                throw new RuntimeException(error);
            }
        } catch (Exception e) {
            String error = String.format("OnText: %s %s ex: %s", userId, messageId, e);
            throw new RuntimeException(error);
        }
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
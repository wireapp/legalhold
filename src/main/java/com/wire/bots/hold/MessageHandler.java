package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.AttachmentMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.SystemMessage;
import com.wire.bots.sdk.tools.Logger;

import java.util.UUID;

public class MessageHandler extends MessageHandlerBase {
    private final EventsDAO eventsDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    MessageHandler(EventsDAO eventsDAO) {
        this.eventsDAO = eventsDAO;
    }

    @Override
    public void onNewConversation(WireClient client, SystemMessage message) {
        String userId = client.getId();
        try {
            Logger.info("onNewConversation: user: %s, conv: %s", userId, message.conversation.id);

            String payload = mapper.writeValueAsString(message);
            eventsDAO.insert(message.id, message.conversation.id, message.type, payload);
        } catch (Exception e) {
            String error = String.format("onNewConversation: %s ex: %s", userId, e);
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage message) {
        UUID conversationId = client.getConversationId();
        String userId = client.getId();
        try {
            for (UUID memberId : message.users) {
                Logger.info("onMemberJoin: %s, user: %s, member: %s",
                        conversationId,
                        userId,
                        memberId);
            }

            String payload = mapper.writeValueAsString(message);

            eventsDAO.insert(message.id, conversationId, message.type, payload);
        } catch (Exception e) {
            String error = String.format("onMemberJoin: %s ex: %s", userId, e);
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onMemberLeave(WireClient client, SystemMessage message) {
        UUID conversationId = client.getConversationId();
        try {
            for (UUID memberId : message.users) {
                Logger.info("onMemberLeave: %s, user: %s, member: %s",
                        conversationId,
                        client.getId(),
                        memberId);
            }

            String payload = mapper.writeValueAsString(message);

            eventsDAO.insert(message.id, conversationId, message.type, payload);
        } catch (Exception e) {
            String error = String.format("onMemberLeave: %s ex: %s", client.getId(), e);
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID conversationId = client.getConversationId();
        String userId = client.getId();

        UUID senderId = msg.getUserId();
        String time = msg.getTime();

        Logger.info("onText %s, %s -> %s msg:%s at %s",
                conversationId,
                senderId,
                userId,
                msg.getMessageId(),
                time);

        String type = "conversation.otr-message-add.new-text";

        persist(msg, conversationId, userId, msg.getMessageId(), type);
    }

    private void persist(TextMessage msg, UUID conversationId, String userId, UUID messageId, String type) {
        try {
            String payload = mapper.writeValueAsString(msg);
            int insert = eventsDAO.insert(messageId, conversationId, type, payload);
        } catch (Exception e) {
            String error = String.format("%s: %s %s ex: %s", type, userId, messageId, e);
            throw new RuntimeException(error);
        }
    }

    public void onImage(WireClient client, ImageMessage msg) {
        UUID conversationId = client.getConversationId();
        String userId = client.getId();

        UUID messageId = msg.getMessageId();
        UUID senderId = msg.getUserId();
        String time = msg.getTime();
        Logger.info("onImage: %s, %s -> %s, %s %s msg: %s, time: %s",
                conversationId,
                senderId,
                userId,
                msg.getName(),
                msg.getMimeType(),
                messageId,
                time);
        String type = "conversation.otr-message-add.new-image";

        try {
            String payload = mapper.writeValueAsString(msg);
            int insert = eventsDAO.insert(messageId, conversationId, type, payload);
        } catch (Exception e) {
            String error = String.format("%s: %s %s ex: %s", type, userId, messageId, e);
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onAttachment(WireClient client, AttachmentMessage msg) {
        UUID messageId = msg.getMessageId();
        UUID conversationId = msg.getConversationId();
        UUID senderId = msg.getUserId();
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
        String type = "conversation.otr-message-add.new-attachment";

        try {
            String payload = mapper.writeValueAsString(msg);
            int insert = eventsDAO.insert(messageId, conversationId, type, payload);
        } catch (Exception e) {
            String error = String.format("%s: %s %s ex: %s", type, userId, messageId, e);
            throw new RuntimeException(error);
        }
    }

    @Override
    public void onCalling(WireClient client, UUID userId, String clientId, String content) {
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
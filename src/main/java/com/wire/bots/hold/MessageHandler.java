package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.AttachmentMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.Member;
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

            Conversation conversation = new Conversation();
            conversation.id = conversationId;
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
        Conversation conversation = new Conversation();
        conversation.id = conversationId;
        conversation.members = new ArrayList<>();

        try {
            for (String memberId : userIds) {
                Logger.info("onMemberJoin: %s, user: %s, member: %s",
                        conversationId,
                        userId,
                        memberId);
                Member member = new Member();
                member.id = memberId;
                conversation.members.add(member);
            }

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
        Conversation conversation = new Conversation();
        conversation.id = conversationId;
        conversation.members = new ArrayList<>();

        try {
            for (String memberId : userIds) {
                Logger.info("onMemberLeave: %s, user: %s, member: %s",
                        conversationId,
                        userId,
                        memberId);
                Member member = new Member();
                member.id = memberId;
                conversation.members.add(member);
            }

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

        String type = "conversation.otr-message-add.new-text";

        persist(msg, conversationId, userId, messageId, type);
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
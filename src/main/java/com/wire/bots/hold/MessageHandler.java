package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.*;
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
    public void onNewConversation(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, userId, messageId, type, msg);
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, userId, messageId, type, msg);
    }

    @Override
    public void onMemberLeave(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, userId, messageId, type, msg);
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID senderId = msg.getUserId();
        UUID messageId = msg.getMessageId();
        String type = "conversation.otr-message-add.new-text";

        persist(convId, senderId, userId, messageId, type, msg);
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID messageId = msg.getMessageId();
        UUID senderId = msg.getUserId();
        String type = "conversation.otr-message-add.new-image";

        persist(convId, senderId, userId, messageId, type, msg);
    }

    @Override
    public void onAudio(WireClient client, AudioMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID messageId = msg.getMessageId();
        UUID senderId = msg.getUserId();
        String type = "conversation.otr-message-add.new-audio";

        persist(convId, senderId, userId, messageId, type, msg);
    }

    @Override
    public void onVideo(WireClient client, VideoMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID messageId = msg.getMessageId();
        UUID senderId = msg.getUserId();
        String type = "conversation.otr-message-add.new-video";

        persist(convId, senderId, userId, messageId, type, msg);
    }

    @Override
    public void onAttachment(WireClient client, AttachmentMessage msg) {
        UUID messageId = msg.getMessageId();
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = UUID.fromString(client.getId());
        String type = "conversation.otr-message-add.new-attachment";

        persist(convId, senderId, userId, messageId, type, msg);
    }

    @Override
    public void onEditText(WireClient client, EditedTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID senderId = msg.getUserId();
        UUID messageId = msg.getMessageId();
        String type = "conversation.otr-message-add.edit-text";

        persist(convId, senderId, userId, messageId, type, msg);
    }

    @Override
    public void onConversationRename(WireClient client) {

    }

    @Override
    public void onDelete(WireClient client, DeletedTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID senderId = msg.getUserId();
        UUID messageId = msg.getMessageId();
        String type = "conversation.otr-message-add.delete-text";

        persist(convId, senderId, userId, messageId, type, msg);
    }

    @Override
    public void onCalling(WireClient client, CallingMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = UUID.fromString(client.getId());
        UUID senderId = msg.getUserId();
        UUID messageId = msg.getMessageId();
        String type = "conversation.otr-message-add.call";

        persist(convId, senderId, userId, messageId, type, msg);
    }

    @Override
    public boolean onConnectRequest(WireClient client, UUID from, UUID to, String status) {
        return false;
    }

    @Override
    public void validatePreKeys(WireClient client, int size) {

    }

    private void persist(UUID convId, UUID senderId, UUID userId, UUID msgId, String type, Object msg)
            throws RuntimeException {
        try {
            String payload = mapper.writeValueAsString(msg);
            int insert = eventsDAO.insert(msgId, convId, type, payload);

            Logger.info("%s: conv: %s, %s -> %s, msg: %s, insert: %d",
                    type,
                    convId,
                    senderId,
                    userId,
                    msgId,
                    insert);
        } catch (Exception e) {
            String error = String.format("%s: conv: %s, user: %s, msg: %s, e: %s",
                    type,
                    convId,
                    userId,
                    msgId,
                    e);

            Logger.error(error);
            throw new RuntimeException(error);
        }
    }
}
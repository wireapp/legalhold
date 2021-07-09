package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.models.SystemMessage;
import com.wire.xenon.models.*;
import com.wire.xenon.tools.Logger;

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
        UUID userId = client.getId();
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, userId, messageId, type, msg);
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, userId, messageId, type, msg);
    }

    @Override
    public void onMemberLeave(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID messageId = msg.id;
        String type = msg.type;

        persist(convId, null, userId, messageId, type, msg);
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        UUID eventId = msg.getEventId();
        String type = "conversation.otr-message-add.new-text";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public void onText(WireClient client, EphemeralTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        UUID eventId = msg.getEventId();
        String type = "conversation.otr-message-add.new-text";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public void onPhotoPreview(WireClient client, PhotoPreviewMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.image-preview";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public void onFilePreview(WireClient client, FilePreviewMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.file-preview";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public void onAudioPreview(WireClient client, AudioPreviewMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.audio-preview";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public void onVideoPreview(WireClient client, VideoPreviewMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.video-preview";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public void onAssetData(WireClient client, RemoteMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.new-attachment";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public void onEditText(WireClient client, EditedTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        UUID eventId = msg.getEventId();
        String type = "conversation.otr-message-add.edit-text";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public void onConversationRename(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID messageId = msg.id;
        String type = "conversation.rename";

        persist(convId, null, userId, messageId, type, msg);
    }

    @Override
    public void onDelete(WireClient client, DeletedTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        UUID eventId = msg.getEventId();
        String type = "conversation.otr-message-add.delete-text";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public void onCalling(WireClient client, CallingMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        UUID eventId = msg.getEventId();
        String type = "conversation.otr-message-add.call";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    public void onReaction(WireClient client, ReactionMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        UUID eventId = msg.getEventId();
        String type = "conversation.otr-message-add.reaction";

        persist(convId, senderId, userId, eventId, type, msg);
    }

    @Override
    public boolean onConnectRequest(WireClient client, UUID from, UUID to, String status) {
        return false;
    }

    @Override
    public void validatePreKeys(WireClient client, int size) {

    }

    private void persist(UUID convId, UUID senderId, UUID userId, UUID eventId, String type, Object msg)
            throws RuntimeException {
        try {
            String payload = mapper.writeValueAsString(msg);
            int insert = eventsDAO.insert(eventId, convId, type, payload);

            Logger.info("%s: conv: %s, %s -> %s, event: %s, insert: %d",
                    type,
                    convId,
                    senderId,
                    userId,
                    eventId,
                    insert);
        } catch (Exception e) {
            String error = String.format("%s: conv: %s, user: %s, event: %s, e: %s",
                    type,
                    convId,
                    userId,
                    eventId,
                    e.getMessage());

            Logger.exception(error, e);
            throw new RuntimeException(error);
        }
    }
}

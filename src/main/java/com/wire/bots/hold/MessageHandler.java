package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.DAO.AssetsDAO;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.model.LHAccess;
import com.wire.helium.API;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.SystemMessage;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.models.*;
import com.wire.xenon.tools.Logger;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.client.Client;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MessageHandler extends MessageHandlerBase {
    private final EventsDAO eventsDAO;
    private final AssetsDAO assetsDAO;
    private final AccessDAO accessDAO;
    private final Client httpClient;

    private final ObjectMapper mapper = new ObjectMapper();

    MessageHandler(Jdbi jdbi, Client httpClient) {
        eventsDAO = jdbi.onDemand(EventsDAO.class);
        assetsDAO = jdbi.onDemand(AssetsDAO.class);
        accessDAO = jdbi.onDemand(AccessDAO.class);
        this.httpClient = httpClient;
    }

    @Override
    public void onNewConversation(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = msg.type;

        persist(convId, null, userId, type, msg);
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = msg.type;

        persist(convId, null, userId, type, msg);
    }

    @Override
    public void onMemberLeave(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = msg.type;

        persist(convId, null, userId, type, msg);
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        String type = "conversation.otr-message-add.new-text";

        persist(convId, senderId, userId, type, msg);

        trace(msg, convId, senderId, userId);
    }

    @Override
    public void onText(WireClient client, EphemeralTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        String type = "conversation.otr-message-add.new-text";

        persist(convId, senderId, userId, type, msg);
    }

    @Override
    public void onPhotoPreview(WireClient client, PhotoPreviewMessage msg) {
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.image-preview";

        persist(convId, senderId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onFilePreview(WireClient client, FilePreviewMessage msg) {
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.file-preview";

        persist(convId, senderId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onAudioPreview(WireClient client, AudioPreviewMessage msg) {
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.audio-preview";

        persist(convId, senderId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onVideoPreview(WireClient client, VideoPreviewMessage msg) {
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.video-preview";

        persist(convId, senderId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onAssetData(WireClient client, RemoteMessage msg) {
        UUID convId = msg.getConversationId();
        UUID senderId = msg.getUserId();
        UUID userId = client.getId();
        String type = "conversation.otr-message-add.asset-data";

        persist(convId, senderId, userId, type, msg);

        try {
            final byte[] assetData = client.downloadAsset(msg.getAssetId(), msg.getAssetToken(), msg.getSha256(), msg.getOtrKey());
            assetsDAO.insert(msg.getMessageId(), assetData);
        } catch (Exception e) {
            Logger.error("onAssetData, msg: %s, %s", msg.getMessageId(), e);
        }
    }

    @Override
    public void onEditText(WireClient client, EditedTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        String type = "conversation.otr-message-add.edit-text";

        persist(convId, senderId, userId, type, msg);
    }

    @Override
    public void onConversationRename(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = "conversation.rename";

        persist(convId, null, userId, type, msg);
    }

    @Override
    public void onDelete(WireClient client, DeletedTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        String type = "conversation.otr-message-add.delete-text";

        persist(convId, senderId, userId, type, msg);
    }

    @Override
    public void onCalling(WireClient client, CallingMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        String type = "conversation.otr-message-add.call";

        persist(convId, senderId, userId, type, msg);
    }

    public void onReaction(WireClient client, ReactionMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        UUID senderId = msg.getUserId();
        String type = "conversation.otr-message-add.reaction";

        persist(convId, senderId, userId, type, msg);
    }

    @Override
    public boolean onConnectRequest(WireClient client, UUID from, UUID to, String status) {
        return false;
    }

    @Override
    public void validatePreKeys(WireClient client, int size) {

    }

    private void persist(UUID convId, UUID senderId, UUID userId, String type, Object msg)
            throws RuntimeException {
        final UUID id = UUID.randomUUID();

        try {
            String payload = mapper.writeValueAsString(msg);
            int insert = eventsDAO.insert(id, convId, type, payload);

            Logger.info("%s: conv: %s, %s -> %s, id: %s, insert: %d",
                    type,
                    convId,
                    senderId,
                    userId,
                    id,
                    insert);
        } catch (Exception e) {
            String error = String.format("%s: conv: %s, user: %s, id: %s, e: %s",
                    type,
                    convId,
                    userId,
                    id,
                    e.getMessage());

            Logger.exception(e, error);
            throw new RuntimeException(error);
        }
    }

    private void trace(TextMessage msg, UUID convId, UUID senderId, UUID userId) {
        try {
            LHAccess single = accessDAO.get(userId);
            API api = new API(httpClient, convId, single.token);

            Conversation conversation = api.getConversation();
            List<UUID> members = conversation.members.stream().map(x -> x.id).collect(Collectors.toList());
            Collection<User> users = api.getUsers(members);
            List<String> participants = users.stream().map(x -> x.handle).collect(Collectors.toList());
            String sender = users.stream().filter(x -> x.id.equals(senderId)).map(x -> x.handle).findFirst().orElse(null);

            _Event event = new _Event();
            event.conversationId = convId;
            event.conversationName = conversation.name;
            event.participants = participants;
            event.messageId = msg.getMessageId();
            event.sender = sender;
            event.type = "text";
            event.text = msg.getText();
            event.time = new Date();

            System.out.println(mapper.writeValueAsString(event));

        } catch (Exception e) {
            Logger.exception(e, "Error tracing");
        }
    }

    static class _Event {
        public String type;
        public UUID conversationId;
        public String conversationName;
        public UUID messageId;
        public String sender;
        public String text;
        public Date time;
        public List<String> participants;
    }
}

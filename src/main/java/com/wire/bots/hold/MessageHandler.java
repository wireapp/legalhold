package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AssetsDAO;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.model.Log;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.Member;
import com.wire.xenon.backend.models.SystemMessage;
import com.wire.xenon.models.*;
import com.wire.xenon.tools.Logger;
import org.jdbi.v3.core.Jdbi;

import java.util.UUID;

import static com.wire.bots.hold.KibanaExporter.date;

public class MessageHandler extends MessageHandlerBase {
    private final EventsDAO eventsDAO;
    private final AssetsDAO assetsDAO;

    private final ObjectMapper mapper = new ObjectMapper();

    MessageHandler(Jdbi jdbi) {
        eventsDAO = jdbi.onDemand(EventsDAO.class);
        assetsDAO = jdbi.onDemand(AssetsDAO.class);
    }

    @Override
    public void onNewConversation(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = msg.type;

        persist(convId, userId, type, msg);
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = msg.type;

        persist(convId, userId, type, msg);
    }

    @Override
    public void onMemberLeave(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = msg.type;

        persist(convId, userId, type, msg);
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_NEW_TEXT;

        UUID eventId = persist(convId, userId, type, msg);

        kibana(eventId, type, msg, client);
    }

    @Override
    public void onText(WireClient client, EphemeralTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_NEW_TEXT;

        persist(convId, userId, type, msg);
    }

    @Override
    public void onPhotoPreview(WireClient client, PhotoPreviewMessage msg) {
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_IMAGE_PREVIEW;

        persist(convId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onFilePreview(WireClient client, FilePreviewMessage msg) {
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_FILE_PREVIEW;

        persist(convId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onAudioPreview(WireClient client, AudioPreviewMessage msg) {
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_AUDIO_PREVIEW;

        persist(convId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onVideoPreview(WireClient client, VideoPreviewMessage msg) {
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_VIDEO_PREVIEW;

        persist(convId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onAssetData(WireClient client, RemoteMessage msg) {
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_ASSET_DATA;

        persist(convId, userId, type, msg);

        try {
            final byte[] assetData = client.downloadAsset(msg.getAssetId(), msg.getAssetToken(), msg.getSha256(), msg.getOtrKey());
            assetsDAO.insert(msg.getMessageId(), assetData);
        } catch (Exception e) {
            Logger.exception(e, "onAssetData");
        }
    }

    @Override
    public void onEditText(WireClient client, EditedTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_EDIT_TEXT;

        persist(convId, userId, type, msg);
    }

    @Override
    public void onConversationRename(WireClient client, SystemMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_RENAME;

        persist(convId, userId, type, msg);
    }

    @Override
    public void onDelete(WireClient client, DeletedTextMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_DELETE_TEXT;

        persist(convId, userId, type, msg);
    }

    @Override
    public void onCalling(WireClient client, CallingMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_CALL;

        persist(convId, userId, type, msg);
    }

    public void onReaction(WireClient client, ReactionMessage msg) {
        UUID convId = client.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_REACTION;

        persist(convId, userId, type, msg);
    }

    @Override
    public boolean onConnectRequest(WireClient client, UUID from, UUID to, String status) {
        return false;
    }

    @Override
    public void validatePreKeys(WireClient client, int size) {

    }

    private UUID persist(UUID convId, UUID userId, String type, Object msg)
            throws RuntimeException {
        final UUID id = UUID.randomUUID();

        try {
            String payload = mapper.writeValueAsString(msg);
            eventsDAO.insert(id, convId, userId, type, payload);
        } catch (Exception e) {
            Logger.exception(e, "%s: conv: %s, user: %s, id: %s, e: %s",
                    type,
                    convId,
                    userId,
                    id);
            throw new RuntimeException(e);
        }
        return id;
    }

    void kibana(UUID id, String type, TextMessage msg, WireClient client) {
        try {
            Log.Kibana kibana = new Log.Kibana();
            kibana.id = id;
            kibana.type = type;
            kibana.messageID = msg.getMessageId();
            kibana.conversationID = msg.getConversationId();
            kibana.from = msg.getUserId();
            kibana.sent = date(msg.getTime());
            kibana.text = msg.getText();

            kibana.sender = client.getUser(msg.getUserId()).handle;

            Conversation conversation = client.getConversation();
            kibana.conversationName = conversation.name;

            for (Member m : conversation.members) {
                kibana.participants.add(client.getUser(m.id).handle);
            }

            Log log = new Log();
            log.securehold = kibana;
            System.out.println(mapper.writeValueAsString(log));
        } catch (Exception e) {
            Logger.exception(e, "MessageHandler:kibana: evt: %s", id);
        }
    }
}

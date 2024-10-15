package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AssetsDAO;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.backend.models.SystemMessage;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.models.*;
import com.wire.xenon.tools.Logger;
import org.jdbi.v3.core.Jdbi;

import java.util.UUID;

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
        UUID eventId = msg.id;
        QualifiedId conversationId = msg.conversation.id;
        String type = msg.type;

        persist(eventId, conversationId, client, type, msg);
    }

    @Override
    public void onConversationRename(WireClient client, SystemMessage msg) {
        UUID eventId = msg.id;
        QualifiedId conversationId = msg.conversation.id;
        String type = Const.CONVERSATION_RENAME;

        persist(eventId, conversationId, client, type, msg);
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage msg) {
        UUID eventId = msg.id;
        QualifiedId conversationId = msg.conversation.id;
        String type = msg.type;

        persist(eventId, conversationId, client, type, msg);
    }

    @Override
    public void onMemberLeave(WireClient client, SystemMessage msg) {
        UUID eventId = msg.id;
        QualifiedId conversationId = msg.conversation.id;
        String type = msg.type;

        persist(eventId, conversationId, client, type, msg);
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_NEW_TEXT;

        persist(eventId, conversationId, client, type, msg);
    }

    @Override
    public void onText(WireClient client, EphemeralTextMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_NEW_TEXT;

        persist(eventId, conversationId, client, type, msg);
    }

    @Override
    public void onEditText(WireClient client, EditedTextMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_EDIT_TEXT;

        persist(eventId, conversationId, client, type, msg);
    }

    @Override
    public void onPhotoPreview(WireClient client, PhotoPreviewMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_IMAGE_PREVIEW;

        persist(eventId, conversationId, client, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onFilePreview(WireClient client, FilePreviewMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_FILE_PREVIEW;

        persist(eventId, conversationId, client, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onAudioPreview(WireClient client, AudioPreviewMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_AUDIO_PREVIEW;

        persist(eventId, conversationId, client, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onVideoPreview(WireClient client, VideoPreviewMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_VIDEO_PREVIEW;

        persist(eventId, conversationId, client, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onAssetData(WireClient client, RemoteMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_ASSET_DATA;

        persist(eventId, conversationId, client, type, msg);

        try {
            final byte[] assetData = client.downloadAsset(
                msg.getAssetId(),
                msg.getUserId().domain,
                msg.getAssetToken(),
                msg.getSha256(),
                msg.getOtrKey()
            );
            assetsDAO.insert(msg.getMessageId(), assetData);
        } catch (Exception e) {
            Logger.exception(e, "onAssetData");
        }
    }

    @Override
    public void onDelete(WireClient client, DeletedTextMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_DELETE_TEXT;

        persist(eventId, conversationId, client, type, msg);
    }

    @Override
    public void onCalling(WireClient client, CallingMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_CALL;

        persist(eventId, conversationId, client, type, msg);
    }

    public void onReaction(WireClient client, ReactionMessage msg) {
        UUID eventId = msg.getEventId();
        QualifiedId conversationId = msg.getConversationId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_REACTION;

        persist(eventId, conversationId, client, type, msg);
    }

    @Override
    public boolean onConnectRequest(WireClient client, QualifiedId from, QualifiedId to, String status) {
        return false;
    }

    @Override
    public void validatePreKeys(WireClient client, int size) {

    }

    private void persist(UUID eventId, QualifiedId conversationId, WireClient client, String type, Object msg) {
        try {
            User user = client.getSelf();
            String payload = mapper.writeValueAsString(msg);

            eventsDAO.insert(eventId, conversationId.id, conversationId.domain, user.id.id, user.id.domain, type, payload);
        } catch (Exception exception) {
            Logger.exception(
                exception,
                "%s: conversation: %s, event: %s, e: %s",
                type,
                conversationId,
                eventId
            );
        }
    }
}

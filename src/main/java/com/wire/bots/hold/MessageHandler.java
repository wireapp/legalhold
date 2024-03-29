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
        UUID eventId = msg.id;
        UUID convId = msg.convId;
        UUID userId = client.getId();
        String type = msg.type;

        persist(eventId, convId, userId, type, msg);
        kibana(type, msg, client);
    }

    @Override
    public void onConversationRename(WireClient client, SystemMessage msg) {
        UUID eventId = msg.id;
        UUID convId = msg.convId;
        UUID userId = client.getId();
        String type = Const.CONVERSATION_RENAME;

        persist(eventId, convId, userId, type, msg);
        kibana(type, msg, client);
    }

    @Override
    public void onMemberJoin(WireClient client, SystemMessage msg) {
        UUID eventId = msg.id;
        UUID convId = msg.convId;
        UUID userId = client.getId();
        String type = msg.type;

        persist(eventId, convId, userId, type, msg);
        kibana(type, msg, client);
    }

    @Override
    public void onMemberLeave(WireClient client, SystemMessage msg) {
        UUID eventId = msg.id;
        UUID convId = msg.convId;
        UUID userId = client.getId();
        String type = msg.type;

        persist(eventId, convId, userId, type, msg);
        kibana(type, msg, client);
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_NEW_TEXT;

        persist(eventId, convId, userId, type, msg);
        kibana(type, msg, client);
    }

    @Override
    public void onText(WireClient client, EphemeralTextMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_NEW_TEXT;

        persist(eventId, convId, userId, type, msg);
        kibana(type, msg, client);
    }

    @Override
    public void onEditText(WireClient client, EditedTextMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_EDIT_TEXT;

        persist(eventId, convId, userId, type, msg);
        kibana(type, msg, client);
    }

    @Override
    public void onPhotoPreview(WireClient client, PhotoPreviewMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_IMAGE_PREVIEW;

        persist(eventId, convId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onFilePreview(WireClient client, FilePreviewMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_FILE_PREVIEW;

        persist(eventId, convId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onAudioPreview(WireClient client, AudioPreviewMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_AUDIO_PREVIEW;

        persist(eventId, convId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onVideoPreview(WireClient client, VideoPreviewMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_VIDEO_PREVIEW;

        persist(eventId, convId, userId, type, msg);

        assetsDAO.insert(msg.getMessageId(), msg.getMimeType());
    }

    @Override
    public void onAssetData(WireClient client, RemoteMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_ASSET_DATA;

        persist(eventId, convId, userId, type, msg);

        try {
            final byte[] assetData = client.downloadAsset(msg.getAssetId(), msg.getAssetToken(), msg.getSha256(), msg.getOtrKey());
            assetsDAO.insert(msg.getMessageId(), assetData);
        } catch (Exception e) {
            Logger.exception(e, "onAssetData");
        }
    }

    @Override
    public void onDelete(WireClient client, DeletedTextMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_DELETE_TEXT;

        persist(eventId, convId, userId, type, msg);
    }

    @Override
    public void onCalling(WireClient client, CallingMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_CALL;

        persist(eventId, convId, userId, type, msg);
    }

    public void onReaction(WireClient client, ReactionMessage msg) {
        UUID eventId = msg.getEventId();
        UUID convId = msg.getConversationId();
        UUID userId = client.getId();
        String type = Const.CONVERSATION_OTR_MESSAGE_ADD_REACTION;

        persist(eventId, convId, userId, type, msg);
    }

    @Override
    public boolean onConnectRequest(WireClient client, UUID from, UUID to, String status) {
        return false;
    }

    @Override
    public void validatePreKeys(WireClient client, int size) {

    }

    private void persist(UUID eventId, UUID convId, UUID userId, String type, Object msg) {
        try {
            String payload = mapper.writeValueAsString(msg);
            eventsDAO.insert(eventId, convId, userId, type, payload);
        } catch (Exception e) {
            Logger.exception(e, "%s: conv: %s, user: %s, id: %s, e: %s",
                    type,
                    convId,
                    userId,
                    eventId);
        }
    }

    void kibana(String type, TextMessage msg, WireClient client) {
        try {
            Log.Kibana kibana = new Log.Kibana();
            kibana.id = msg.getEventId();
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
            Logger.exception(e, "MessageHandler:kibana: evt: %s", msg.getEventId());
        }
    }

    void kibana(String type, SystemMessage msg, WireClient client) {
        try {
            Log.Kibana kibana = new Log.Kibana();
            kibana.id = msg.id;
            kibana.type = type;
            kibana.messageID = msg.id; //todo fix xenon to extract the messageId from payload.data.id
            kibana.conversationID = msg.convId;
            kibana.from = msg.from;
            kibana.sent = date(msg.time);

            kibana.sender = client.getUser(msg.from).handle;

            Conversation conversation = client.getConversation();
            kibana.conversationName = conversation.name;

            for (Member m : conversation.members) {
                kibana.participants.add(client.getUser(m.id).handle);
            }

            Log log = new Log();
            log.securehold = kibana;
            System.out.println(mapper.writeValueAsString(log));
        } catch (Exception e) {
            Logger.exception(e, "MessageHandler:kibana: evt: %s", msg.id);
        }
    }
}

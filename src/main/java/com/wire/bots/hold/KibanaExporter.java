package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.model.Event;
import com.wire.bots.hold.model.LHAccess;
import com.wire.bots.hold.utils.Cache;
import com.wire.bots.hold.utils.LegalHoldAPI;
import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.Member;
import com.wire.xenon.backend.models.SystemMessage;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.models.RemoteMessage;
import com.wire.xenon.models.TextMessage;
import com.wire.xenon.tools.Logger;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class KibanaExporter implements Runnable {
    private final Client httpClient;
    private final EventsDAO eventsDAO;
    private final AccessDAO accessDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    public KibanaExporter(Jdbi jdbi, Client httpClient) {
        this.httpClient = httpClient;

        eventsDAO = jdbi.onDemand(EventsDAO.class);
        accessDAO = jdbi.onDemand(AccessDAO.class);
    }

    public void run() {
        int count = 0;

        Set<UUID> uniques = new HashSet<>();

        List<Event> events = eventsDAO.listAllUnxported();
        Logger.info("KibanaExporter: exporting %d events", events.size());
        for (Event event : events) {
            try {
                final LHAccess access = accessDAO.get(event.userId);

                if (!access.enabled) {
                    Logger.warning("KibanaExporter: skipping event: %s, userId: %s", event.eventId, event.userId);

                    //todo only for testing
                    eventsDAO.markExported(event.eventId);
                    continue;
                }

                Kibana kibana = processEvent(event);

                if (kibana == null) {
                    Logger.info("KibanaExporter: skipping %s evt: %s", event.type, event.eventId);
                    eventsDAO.markExported(event.eventId);
                    continue;
                }

                assert event.userId.equals(kibana.from);

                if (!uniques.add(kibana.messageID)) {
                    Logger.info("KibanaExporter: de-dup. msg: %s", kibana.messageID);
                    eventsDAO.markExported(event.eventId);
                    continue;
                }

                _Conversation conversation = fetchConversation(event, access);
                kibana.conversationName = conversation.name;
                kibana.participants = conversation.participants;
                kibana.sender = conversation.user;

                _Log log = new _Log();
                log.securehold = kibana;
                System.out.println(mapper.writeValueAsString(log));

                if (eventsDAO.markExported(event.eventId) > 0)
                    count++;

            } catch (Exception ex) {
                Logger.exception(ex, "Export exception. cvn: %s, evt: %s", event.conversationId, event.eventId);
            }
        }
        Logger.info("Finished exporting %d messages to Kibana", count);
    }

    @Nullable
    private Kibana processEvent(Event event) throws JsonProcessingException, ParseException {
        UUID userId;
        UUID messageId;
        String time;
        String text = null;

        switch (event.type) {
            case "conversation.create":
            case "conversation.rename":
            case "conversation.member-leave":
            case "conversation.member-join": {
                SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
                userId = msg.from;
                messageId = msg.id;
                time = msg.time;
            }
            break;
            case "conversation.otr-message-add.new-text":
            case "conversation.otr-message-add.edit-text": {
                TextMessage msg = mapper.readValue(event.payload, TextMessage.class);
                userId = msg.getUserId();
                messageId = msg.getMessageId();
                time = msg.getTime();
                text = msg.getText();
            }
            break;
            case "conversation.otr-message-add.asset-data": {
                RemoteMessage msg = mapper.readValue(event.payload, RemoteMessage.class);
                userId = msg.getUserId();
                messageId = msg.getMessageId();
                time = msg.getTime();
                text = msg.getAssetId();
            }
            break;
            case "conversation.otr-message-add.image-preview":
            case "conversation.otr-message-add.video-preview":
            case "conversation.otr-message-add.file-preview":
            case "conversation.otr-message-add.audio-preview":
            case "conversation.otr-message-add.call":
            case "conversation.otr-message-add.delete-text":
            case "conversation.otr-message-add.reaction":
                return null;
            default:
                Logger.warning("Kibana exporter: Unknown type: %s", event.type);
                return null;
        }

        Kibana ret = new Kibana();
        ret.id = event.eventId;
        ret.type = event.type;
        ret.messageID = messageId;
        ret.conversationID = event.conversationId;
        ret.from = userId;
        ret.sent = date(time);
        ret.text = text;

        return ret;
    }

    private _Conversation fetchConversation(Event event, LHAccess access) {
        _Conversation ret = new _Conversation();

        final String token = access.token != null ? access.token : access.cookie;
        final LegalHoldAPI api = new LegalHoldAPI(httpClient, event.conversationId, token);
        Cache cache = new Cache(api, null);

        Conversation conversation = api.getConversation();
        ret.name = conversation.name;
        ret.user = name(cache.getUser(event.userId));
        for (Member m : conversation.members) {
            User user = cache.getUser(m.id);
            ret.participants.add(user.handle != null ? user.handle : user.id.toString());
        }
        return ret;
    }

    private String name(User user) {
        return user.handle != null ? user.handle : user.id.toString();
    }

    public static Long date(@Nullable String date) throws ParseException {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date ret = parser.parse(date);
        return ret.getTime();
    }

    static class Kibana {
        public UUID id;
        public String type;
        public UUID conversationID;
        public String conversationName;
        public List<String> participants;
        public Long sent;
        public String sender;
        public UUID messageID;
        public String text;
        public UUID from;
    }

    static class _Log {
        public Kibana securehold;
    }

    static class _Conversation {
        ArrayList<String> participants = new ArrayList<>();
        String user;
        String name;
    }
}

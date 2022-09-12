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
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.servlets.tasks.Task;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class KibanaExporter extends Task implements Runnable {
    private final Client httpClient;
    private final LifecycleEnvironment lifecycleEnvironment;
    private final EventsDAO eventsDAO;
    private final AccessDAO accessDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    public KibanaExporter(Jdbi jdbi, Client httpClient, LifecycleEnvironment lifecycle) {
        super("kibana");
        this.httpClient = httpClient;

        lifecycleEnvironment = lifecycle;
        eventsDAO = jdbi.onDemand(EventsDAO.class);
        accessDAO = jdbi.onDemand(AccessDAO.class);
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {
        lifecycleEnvironment.scheduledExecutorService("Kibana")
                .build()
                .schedule(this, 1, TimeUnit.SECONDS);

        output.println("Kibana task has been queued");
    }

    public void run() {
        int count = 0;

        Set<UUID> uniques = new HashSet<>();

        for (Event event : eventsDAO.listAllUnxported()) {
            try {
                Kibana kibana = processEvent(event);
                if (!uniques.add(kibana.messageID))
                    continue;

                _Log log = new _Log();
                log.securehold = kibana;
                System.out.println(mapper.writeValueAsString(log));

                if (eventsDAO.markExported(event.eventId) > 0)
                    count++;

            } catch (Exception ex) {
                Logger.exception(ex, "Export exception %s %s", event.conversationId, event.eventId);
            }
        }
        Logger.info("Finished exporting %d messages to Kibana", count);
    }

    private Kibana processEvent(Event event) throws JsonProcessingException, ParseException {
        UUID userId = null;
        UUID messageId = null;
        String time = null;
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
                break;
            case "conversation.otr-message-add.new-video":
            case "conversation.otr-message-add.new-audio":
            case "conversation.otr-message-add.new-attachment":
            case "conversation.otr-message-add.new-image": {
                Logger.warning("Kibana exporter: Deleting old type: %s", event.type);
                eventsDAO.delete(event.eventId);
            }
            break;
            default:
                Logger.warning("Kibana exporter: Unknown type: %s", event.type);
                break;
        }

        _Conversation conversation = fetchConversation(event.conversationId, userId);

        Kibana ret = new Kibana();
        ret.id = event.eventId;
        ret.type = event.type;
        ret.messageID = messageId;
        ret.conversationID = event.conversationId;
        ret.conversationName = conversation.name;
        ret.participants = conversation.participants;
        ret.sender = conversation.user;
        ret.sent = date(time);
        ret.text = text;

        return ret;
    }

    private _Conversation fetchConversation(UUID conversationId, @Nullable UUID userId) {
        _Conversation ret = new _Conversation();

        if (userId == null)
            return ret;

        final LHAccess access = accessDAO.get(userId);
        final LegalHoldAPI api = new LegalHoldAPI(httpClient, conversationId, access.token);
        Cache cache = new Cache(api, null);

        Conversation conversation = api.getConversation();
        ret.name = conversation.name;
        ret.user = name(cache.getUser(userId));
        for (Member m : conversation.members) {
            User user = cache.getUser(m.id);
            ret.participants.add(user.handle != null ? user.handle : user.id.toString());
        }
        return ret;
    }

    private String name(User user) {
        return user.handle != null ? user.handle : user.id.toString();
    }

    public static long date(String date) throws ParseException {
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
        public long sent;
        public String sender;
        public UUID messageID;
        public String text;
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

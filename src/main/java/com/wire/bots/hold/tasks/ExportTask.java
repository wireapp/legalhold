package com.wire.bots.hold.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.model.Event;
import com.wire.bots.hold.model.LHAccess;
import com.wire.bots.hold.utils.Cache;
import com.wire.helium.API;
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

import javax.ws.rs.client.Client;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ExportTask extends Task implements Runnable {
    private final Client httpClient;
    private final LifecycleEnvironment lifecycleEnvironment;
    private final EventsDAO eventsDAO;
    private final AccessDAO accessDAO;
    private final ObjectMapper mapper = new ObjectMapper();
    private Cache cache;

    public ExportTask(Jdbi jdbi, Client httpClient, LifecycleEnvironment lifecycle) {
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

        final LHAccess access = accessDAO.getSingle();
        final API api = new API(httpClient, null, access.token);

        cache = new Cache(api, null);

        List<Event> events = eventsDAO.getUnexportedConvs();
        Logger.info("Exporting %d conversations to Kibana", events.size());

        for (Event e : events) {
            String name = null;
            List<User> participants = new ArrayList<>();
            Set<UUID> uniques = new HashSet<>();

            List<Event> messages = eventsDAO.listAllUnxported(e.conversationId);

            for (Event event : messages) {
                try {
                    switch (event.type) {
                        case "conversation.create": {
                            SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
                            if (!uniques.add(msg.id)) continue;

                            name = msg.conversation.name;

                            for (Member m : msg.conversation.members) {
                                User user = cache.getUser(m.id);
                                participants.add(user);
                            }

                            String text = format(msg.conversation);

                            log(event, name, participants, msg, text);

                            if (eventsDAO.markExported(event.eventId) > 0) count++;
                        }
                        break;
                        case "conversation.member-join": {
                            SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
                            if (!uniques.add(msg.id)) continue;

                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("%s added these participants: ", name(msg.from)));
                            for (UUID userId : msg.users) {
                                User user = cache.getUser(userId);
                                participants.add(user);

                                sb.append(String.format("%s,", name(userId)));
                            }

                            log(event, name, participants, msg, sb.toString());

                            if (eventsDAO.markExported(event.eventId) > 0) count++;
                        }
                        case "conversation.member-leave": {
                            SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
                            if (!uniques.add(msg.id)) continue;

                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("%s removed these participants: ", name(msg.from)));
                            for (UUID userId : msg.users) {
                                participants.removeIf(x -> x.id.equals(userId));

                                sb.append(String.format("%s,", name(userId)));
                            }

                            log(event, name, participants, msg, sb.toString());

                            if (eventsDAO.markExported(event.eventId) > 0) count++;
                        }
                        break;
                        case "conversation.otr-message-add.new-text": {
                            TextMessage msg = mapper.readValue(event.payload, TextMessage.class);
                            if (!uniques.add(msg.getMessageId())) continue;

                            log(event, name, participants, msg);

                            if (eventsDAO.markExported(event.eventId) > 0)
                                count++;
                        }
                        break;
                        case "conversation.otr-message-add.image-preview":
                        case "conversation.otr-message-add.video-preview":
                        case "conversation.otr-message-add.file-preview":
                        case "conversation.otr-message-add.audio-preview": {
                            eventsDAO.markExported(event.eventId);
                        }
                        break;
                        case "conversation.otr-message-add.asset-data": {
                            RemoteMessage msg = mapper.readValue(event.payload, RemoteMessage.class);
                            if (!uniques.add(msg.getMessageId())) continue;

                            log(event, name, participants, msg);

                            if (eventsDAO.markExported(event.eventId) > 0)
                                count++;
                        }
                        break;
                    }
                } catch (Exception ex) {
                    Logger.exception(ex, "Export exception %s %s", event.conversationId, event.eventId);
                }
            }
        }
        Logger.info("Finished exporting %d messages to Kibana", count);
    }

    private void log(Event event, String conversation, List<User> participants, TextMessage msg) throws Exception {
        Kibana kibana = new Kibana();
        kibana.id = event.eventId;
        kibana.type = "text";
        kibana.conversationID = msg.getConversationId();
        kibana.conversationName = conversation;
        kibana.participants = participants.stream().map(x -> x.handle != null ? x.handle : x.id.toString()).collect(Collectors.toList());
        kibana.messageID = msg.getMessageId();
        kibana.sender = name(msg.getUserId());
        kibana.text = msg.getText();
        kibana.sent = date(msg.getTime());

        _Log log = new _Log();
        log.securehold = kibana;
        System.out.println(mapper.writeValueAsString(log));
    }

    private void log(Event event, String conversation, List<User> participants, RemoteMessage msg) throws Exception {
        Kibana kibana = new Kibana();
        kibana.id = event.eventId;
        kibana.type = "file";
        kibana.conversationID = msg.getConversationId();
        kibana.conversationName = conversation;
        kibana.participants = participants.stream().map(x -> x.handle != null ? x.handle : x.id.toString()).collect(Collectors.toList());
        kibana.messageID = msg.getMessageId();
        kibana.sender = name(msg.getUserId());
        kibana.text = msg.getAssetId();
        kibana.sent = date(msg.getTime());

        _Log log = new _Log();
        log.securehold = kibana;
        System.out.println(mapper.writeValueAsString(log));
    }

    private void log(Event event, String conversation, List<User> participants, SystemMessage msg, String text) throws Exception {
        Kibana kibana = new Kibana();
        kibana.id = event.eventId;
        kibana.type = "system";
        kibana.conversationID = msg.convId;
        kibana.conversationName = conversation;
        kibana.participants = participants.stream().map(x -> x.handle != null ? x.handle : x.id.toString()).collect(Collectors.toList());
        kibana.messageID = msg.id;
        kibana.sender = name(msg.from);
        kibana.text = text;
        kibana.sent = date(msg.time);

        _Log log = new _Log();
        log.securehold = kibana;
        System.out.println(mapper.writeValueAsString(log));
    }

    private String format(Conversation conversation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s created conversation '%s' with: ", name(conversation.creator), conversation.name));
        for (Member member : conversation.members) {
            sb.append(String.format("%s,", name(member.id)));
        }
        return sb.toString();
    }

    private String name(UUID userId) {
        User user = cache.getUser(userId);
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
}

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
import com.wire.xenon.models.TextMessage;
import com.wire.xenon.tools.Logger;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.servlets.tasks.Task;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.client.Client;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ExportTask extends Task {
    private final Client httpClient;
    private final LifecycleEnvironment lifecycleEnvironment;
    private final AccessDAO accessDAO;
    private final EventsDAO eventsDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    public ExportTask(Jdbi jdbi, Client httpClient, LifecycleEnvironment lifecycleEnvironment) {
        super("kibana");
        this.httpClient = httpClient;
        this.lifecycleEnvironment = lifecycleEnvironment;
        accessDAO = jdbi.onDemand(AccessDAO.class);
        eventsDAO = jdbi.onDemand(EventsDAO.class);
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) {
        lifecycleEnvironment.scheduledExecutorService("ExportTask")
                .threads(1)
                .build()
                .schedule(this::export, 1, TimeUnit.SECONDS);

        output.println("ExportTask task has been queued");
    }

    void export() {
        LHAccess access = accessDAO.getSingle();
        API api = new API(httpClient, null, access.token);
        Cache cache = new Cache(api, null);

        for (Event e : eventsDAO.listConversations()) {
            Conversation conversation = null;
            List<User> participants = new ArrayList<>();

            for (Event event : eventsDAO.listAllAsc(e.conversationId)) {
                try {
                    if (event.type.equals("conversation.create")) {
                        SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
                        for (Member m : msg.conversation.members) {
                            User user = cache.getUser(m.id);
                            participants.add(user);
                            conversation = msg.conversation;
                        }
                    }
                    if (event.type.equals("conversation.member-join")) {
                        SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
                        for (UUID userId : msg.users) {
                            User user = cache.getUser(userId);
                            participants.add(user);
                        }
                    }
                    if (event.type.equals("conversation.otr-message-add.new-text")) {
                        TextMessage msg = mapper.readValue(event.payload, TextMessage.class);

                        Kibana kibana = new Kibana();
                        kibana.conversationId = event.conversationId;
                        kibana.conversationName = conversation == null ? null : conversation.name;
                        kibana.participants = participants.stream().map(x -> x.handle).collect(Collectors.toList());
                        kibana.messageId = msg.getMessageId();
                        kibana.sender = cache.getUser(msg.getUserId()).handle;
                        kibana.type = "text";
                        kibana.text = msg.getText();
                        kibana.time = event.time;

                        System.out.println(mapper.writeValueAsString(kibana));
                    }
                } catch (Exception ex) {
                    Logger.exception(ex, "Export %s %s", event.conversationId, event.eventId);
                }
            }
        }
    }

    static class Kibana {
        public String type;
        public UUID conversationId;
        public String conversationName;
        public UUID messageId;
        public String sender;
        public String text;
        public String time;
        public List<String> participants;
    }
}

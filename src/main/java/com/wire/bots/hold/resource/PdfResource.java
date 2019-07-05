package com.wire.bots.hold.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.Service;
import com.wire.bots.hold.model.Event;
import com.wire.bots.hold.utils.Cache;
import com.wire.bots.hold.utils.Collector;
import com.wire.bots.hold.utils.PdfGenerator;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.SystemMessage;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.API;
import com.wire.bots.sdk.user.LoginClient;
import com.wire.bots.sdk.user.model.User;
import io.swagger.annotations.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

@Api
@Path("/pdf/{conversationId}")
@Produces("application/pdf")
public class PdfResource {
    private final static MustacheFactory mf = new DefaultMustacheFactory();
    private final EventsDAO eventsDAO;
    private final ObjectMapper mapper = new ObjectMapper();

    public PdfResource(EventsDAO eventsDAO) {
        this.eventsDAO = eventsDAO;
    }

    @GET
    @ApiOperation(value = "List all Wire events for this conversation")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Wire events")})
    public Response list(@ApiParam @PathParam("conversationId") UUID conversationId) {
        try {
            List<Event> events = eventsDAO.listAllAsc(conversationId);

            Client client = Service.instance.getClient();
            String email = Service.instance.getConfig().email;
            String password = Service.instance.getConfig().password;

            LoginClient loginClient = new LoginClient(client);
            User admin = loginClient.login(email, password);
            String token = admin.getToken();
            API api = new API(client, null, token);

            Collector collector = new Collector(api);
            for (Event event : events) {
                switch (event.type) {
                    case "conversation.create": {
                        onConversationCreate(api, collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.new-text": {
                        onText(collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.new-image": {
                        onImage(collector, event);
                    }
                    break;
                    case "conversation.member-join": {
                        onMember(api, collector, event, "**%s** joined the conversation");
                    }
                    break;
                    case "conversation.member-leave": {
                        onMember(api, collector, event, "**%s** left the conversation");
                    }
                    break;
                }
            }

            Collector.Conversation conversation = collector.getConversation();
            String html = execute(conversation);
            ByteArrayOutputStream outputStream = PdfGenerator.convert(html);

            return Response.
                    ok(outputStream.toByteArray(), "application/pdf").
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("PdfResource.list: %s", e);
            return Response
                    .serverError()
                    .status(500)
                    .build();
        }
    }

    private void onImage(Collector collector, Event event) {
        try {
            ImageMessage message = mapper.readValue(event.payload, ImageMessage.class);
            collector.add(message);
        } catch (Exception e) {
            Logger.error("onText: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onText(Collector collector, Event event) {
        try {
            TextMessage message = mapper.readValue(event.payload, TextMessage.class);
            collector.add(message);
        } catch (Exception e) {
            Logger.error("onText: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onMember(API api, Collector collector, Event event, String s) {
        try {
            SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
            for (UUID userId : msg.users) {
                com.wire.bots.sdk.server.model.User user = Cache.getUser(api, userId);
                if (user != null) {
                    String format = String.format(s, user.name);
                    collector.add(format, msg.time);
                }
            }
        } catch (Exception e) {
            Logger.error("onMember: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onConversationCreate(API api, Collector collector, Event event) {
        try {
            SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
            Conversation conversation = msg.conversation;
            collector.setConvName(conversation.name);
            com.wire.bots.sdk.server.model.User user = Cache.getUser(api, conversation.creator);
            if (user != null) {
                String format = String.format("New conversation created by **%s**", user.name);
                collector.add(format, msg.time);
            }

            for (Member member : conversation.members) {
                user = Cache.getUser(api, member.id);
                if (user != null) {
                    String format = String.format("with **%s**", user.name);
                    collector.add(format, msg.time);
                }
            }
        } catch (Exception e) {
            Logger.error("onConversationCreate: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private Mustache compileTemplate() {
        String path = "templates/conversation.html";
        return mf.compile(path);
    }

    private String execute(Object model) throws IOException {
        Mustache mustache = compileTemplate();
        try (StringWriter sw = new StringWriter()) {
            mustache.execute(new PrintWriter(sw), model).flush();
            return sw.toString();
        }
    }
}
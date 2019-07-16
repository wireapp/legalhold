package com.wire.bots.hold.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.Service;
import com.wire.bots.hold.model.Event;
import com.wire.bots.hold.model.LHAccess;
import com.wire.bots.hold.utils.Cache;
import com.wire.bots.hold.utils.Collector;
import com.wire.bots.hold.utils.PdfGenerator;
import com.wire.bots.sdk.models.*;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.SystemMessage;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.API;
import io.swagger.annotations.*;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

@Api
@Path("/conv/{conversationId}")
@Produces("application/pdf")
public class ConversationResource {
    private final static MustacheFactory mf = new DefaultMustacheFactory();
    private final EventsDAO eventsDAO;
    private final AccessDAO accessDAO;
    private final ObjectMapper mapper = new ObjectMapper();
    private API api;

    public ConversationResource(EventsDAO eventsDAO, AccessDAO accessDAO) {
        this.eventsDAO = eventsDAO;
        this.accessDAO = accessDAO;
        api = getLHApi();
    }

    @GET
    @ApiOperation(value = "Render Wire events for this conversation")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Wire events")})
    public Response list(@ApiParam @PathParam("conversationId") UUID conversationId,
                         @ApiParam @QueryParam("html") boolean isHtml) {
        try {
            List<Event> events = eventsDAO.listAllAsc(conversationId);

            testAPI();

            Cache cache = new Cache(api);
            Collector collector = new Collector(cache);
            for (Event event : events) {
                switch (event.type) {
                    case "conversation.create": {
                        onConversationCreate(collector, event);
                    }
                    break;
                    case "conversation.rename": {
                        onConversationRename(collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.new-text": {
                        onText(collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.edit-text": {
                        onTextEdit(collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.delete-text": {
                        onTextDelete(collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.new-image": {
                        onImage(collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.new-attachment": {
                        onAttachment(collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.new-audio": {
                        onAudio(collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.new-video": {
                        onVideo(collector, event);
                    }
                    break;
                    case "conversation.otr-message-add.call": {
                        onCall(collector, event);
                    }
                    break;
                    case "conversation.member-join": {
                        onMember(collector, event, "added");
                    }
                    break;
                    case "conversation.member-leave": {
                        onMember(collector, event, "removed");
                    }
                    break;
                }
            }

            Collector.Conversation conversation = collector.getConversation();
            String html = execute(conversation);

            if (isHtml)
                return Response.
                        ok(html, MediaType.TEXT_HTML).
                        build();

            byte[] out = PdfGenerator.convert(html, "file:/opt/hold");
            return Response.
                    ok(out, "application/pdf").
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("ConversationResource.list: %s", e);
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
            Logger.error("onImage: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
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

    private void onTextEdit(Collector collector, Event event) {
        try {
            EditedTextMessage message = mapper.readValue(event.payload, EditedTextMessage.class);
            message.setText("_edit:_ " + message.getText());
            collector.add(message);
        } catch (Exception e) {
            Logger.error("onTextEdit: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onTextDelete(Collector collector, Event event) {
        try {
            DeletedTextMessage message = mapper.readValue(event.payload, DeletedTextMessage.class);
            UUID deletedMessageId = message.getDeletedMessageId();
            String orgText = getText(deletedMessageId);
            String text = String.format("**%s** deleted text: '%s'",
                    getUserName(message.getUserId()),
                    orgText);
            collector.add(text, message.getTime());
        } catch (Exception e) {
            Logger.error("onTextDelete: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onCall(Collector collector, Event event) {
        try {
            CallingMessage message = mapper.readValue(event.payload, CallingMessage.class);
            String json = message.getContent().replace("\\", "");
            _CallingContent content = mapper.readValue(json, _CallingContent.class);
            String text = String.format("**%s** called: %s", getUserName(message.getUserId()), content.type);
            collector.add(text, message.getTime());
        } catch (Exception e) {
            Logger.error("onCall: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onAttachment(Collector collector, Event event) {
        try {
            AttachmentMessage message = mapper.readValue(event.payload, AttachmentMessage.class);
            collector.add(message);
        } catch (Exception e) {
            Logger.error("onAttachment: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onAudio(Collector collector, Event event) {
        try {
            AudioMessage message = mapper.readValue(event.payload, AudioMessage.class);
            collector.add(message);
        } catch (Exception e) {
            Logger.error("onAudio: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onVideo(Collector collector, Event event) {
        try {
            VideoMessage message = mapper.readValue(event.payload, VideoMessage.class);
            collector.add(message);
        } catch (Exception e) {
            Logger.error("onVideo: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onMember(Collector collector, Event event, String label) {
        try {
            SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
            for (UUID userId : msg.users) {
                String format = String.format("**%s** %s **%s**",
                        getUserName(msg.from),
                        label,
                        getUserName(userId));
                collector.add(format, msg.time);
            }
        } catch (Exception e) {
            Logger.error("onMember: %s conv: %s, msg: %s error: %s", event.type, event.conversationId, event.messageId, e);
        }
    }

    private void onConversationCreate(Collector collector, Event event) {
        try {
            SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
            collector.setConvName(msg.conversation.name);

            String text = formatConversation(msg.conversation);
            collector.add(text, msg.time);
        } catch (Exception e) {
            Logger.error("onConversationCreate: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void onConversationRename(Collector collector, Event event) {
        try {
            SystemMessage msg = mapper.readValue(event.payload, SystemMessage.class);
            collector.setConvName(msg.conversation.name);

            String userName = getUserName(msg.from);
            String text = String.format("**%s** renamed the conversation to **%s**", userName, msg.conversation.name);
            collector.add(text, msg.time);
        } catch (Exception e) {
            Logger.error("onConversationRename: conv: %s, msg: %s error: %s", event.conversationId, event.messageId, e);
        }
    }

    private void testAPI() {
        try {
            api.getSelf();
        } catch (Exception e) {
            Logger.info("reconnecting... %s", e);
            api = getLHApi();
        }
    }

    private API getLHApi() {
        Client client = Service.instance.getClient();
        try {
            LHAccess single = accessDAO.getSingle();
            return new API(client, null, single.token);
        } catch (Exception e) {
            Logger.warning("getLHApi: %s", e);
            return new API(client, null, null);
        }
    }

    private String formatConversation(Conversation conversation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("**%s** created conversation **%s** with: \n",
                getUserName(conversation.creator),
                conversation.name));
        for (Member member : conversation.members) {
            sb.append(String.format("- **%s** \n", getUserName(member.id)));
        }
        return sb.toString();
    }

    @Nullable
    private String getText(UUID msgId) throws IOException {
        Event event = eventsDAO.get(msgId);
        if (event == null)
            return null;
        TextMessage orgMessage = mapper.readValue(event.payload, TextMessage.class);
        return orgMessage.getText();
    }

    @Nullable
    private String getUserName(UUID userId) {
        Cache cache = new Cache(api);
        return cache.getUser(userId).name;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _CallingContent {
        @JsonProperty
        boolean resp;
        @JsonProperty
        String sessid;
        @JsonProperty
        String type;
        @JsonProperty
        String version;
    }
}
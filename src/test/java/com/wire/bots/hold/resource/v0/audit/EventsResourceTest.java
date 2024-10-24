package com.wire.bots.hold.resource.v0.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.Config;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.Service;
import com.wire.bots.hold.model.EventModel;
import com.wire.bots.hold.model.database.Event;
import com.wire.bots.hold.utils.Cache;
import com.wire.bots.hold.utils.HtmlGenerator;
import com.wire.bots.hold.utils.HttpTestUtils;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.models.TextMessage;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EventsResourceTest {
    private static final String TOKEN = "dummy_token";
    private static final String API_HOST = "dummy_api_host";
    private static final String FALLBACK_DOMAIN = "dummy_domain";

    private static final QualifiedId newConversationId = new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString());
    private static final QualifiedId oldConversationId = new QualifiedId(UUID.randomUUID(), null);
    private static final QualifiedId oldConversationIdWithDummyDomain = new QualifiedId(oldConversationId.id, FALLBACK_DOMAIN);

    private static final DropwizardTestSupport<Config> SUPPORT = new DropwizardTestSupport<>(
        Service.class,
        "hold.yaml",
        ConfigOverride.config("token", TOKEN),
        ConfigOverride.config("apiHost", API_HOST)
    );

    private static final ObjectMapper mapper = new ObjectMapper();
    private static Client client;
    private static EventsDAO eventsDAO;

    @BeforeClass
    public static void before() throws Exception {
        SUPPORT.before();
        client = HttpTestUtils.createHttpClient(
            SUPPORT.getConfiguration(),
            SUPPORT.getEnvironment()
        );

        Service app = SUPPORT.getApplication();
        eventsDAO = app.getJdbi().onDemand(EventsDAO.class);

        insertDummyEvents();
    }

    @AfterClass
    public static void after() {
        client.close();
        SUPPORT.after();

    }

    @Before
    public void beforeEach() {
        // Clears cached domain
        Cache.setFallbackDomain(null);
    }

    @Before
    public void afterEach() {
        // Clears cached domain
        Cache.setFallbackDomain(null);
    }

    @Test
    public void givenRequestWithIdAndDomain_whenGettingEvents_thenGetEventsAndGenerateHTMLCorrectly() {
        final Response response = getEvents(
            newConversationId.id,
            newConversationId.domain
        );

        assert response.getStatus() == HttpStatus.SC_OK;
        String eventsHTML = response.readEntity(String.class);

        List<Event> events = eventsDAO.listAll(newConversationId.id, newConversationId.domain);
        EventModel model = new EventModel();
        model.events = events;

        String expectedResult = HtmlGenerator.execute(model, HtmlGenerator.TemplateType.EVENTS);
        assert expectedResult.equals(eventsHTML);

        for (Event event : events) {
            assert eventsHTML.contains(event.conversationId.toString());
            assert eventsHTML.contains(event.conversationDomain);
        }
    }

    @Test
    public void givenRequestOnlyWithId_whenGettingEvents_thenGetEventsAndGenerateHTMLCorrectly() {
        Cache.setFallbackDomain(FALLBACK_DOMAIN);

        final Response response = getEvents(
            oldConversationId.id,
            oldConversationId.domain
        );

        assert response.getStatus() == HttpStatus.SC_OK;
        String eventsHTML = response.readEntity(String.class);

        List<Event> events = eventsDAO.listAllDefaultDomain(oldConversationId.id, FALLBACK_DOMAIN);
        EventModel model = new EventModel();
        model.events = events;

        String expectedResult = HtmlGenerator.execute(model, HtmlGenerator.TemplateType.EVENTS);
        assert expectedResult.equals(eventsHTML);

        for (Event event : events) {
            assert eventsHTML.contains(event.conversationId.toString());
        }
    }

    @Test
    public void givenRequestWithIdAndFallbackDomain_whenGettingEvents_thenGetEventsAndGenerateHTMLCorrectly() {
        Cache.setFallbackDomain(FALLBACK_DOMAIN);

        final Response response = getEvents(
            oldConversationIdWithDummyDomain.id,
            oldConversationIdWithDummyDomain.domain
        );

        assert response.getStatus() == HttpStatus.SC_OK;
        String eventsHTML = response.readEntity(String.class);

        List<Event> events = eventsDAO.listAllDefaultDomain(oldConversationIdWithDummyDomain.id, oldConversationIdWithDummyDomain.domain);
        EventModel model = new EventModel();
        model.events = events;

        String expectedResult = HtmlGenerator.execute(model, HtmlGenerator.TemplateType.EVENTS);
        assert expectedResult.equals(eventsHTML);

        for (Event event : events) {
            assert eventsHTML.contains(event.conversationId.toString());
        }
    }

    private Response getEvents(UUID conversationId, String conversationDomain) {
        String conversationIdAndDomain = conversationId.toString();
        if (conversationDomain != null && !conversationDomain.isEmpty()) {
            conversationIdAndDomain = conversationIdAndDomain + "/" + conversationDomain;
        }

        return client
            .target("http://localhost:" + SUPPORT.getLocalPort())
            .path("events")
            .path(conversationIdAndDomain)
            .request(MediaType.TEXT_HTML)
            .header(HttpHeaders.AUTHORIZATION, TOKEN)
            .accept(MediaType.APPLICATION_JSON)
            .get();
    }

    private static void insertDummyEvents() throws JsonProcessingException {
        // New conversation with ID and Domain
        insertTextMessage(newConversationId);
        insertTextMessage(newConversationId);
        insertTextMessage(newConversationId);

        // Old conversation with ID but without Domain
        insertTextMessage(oldConversationIdWithDummyDomain);
        insertTextMessage(oldConversationId);
        insertTextMessage(oldConversationId);
    }

    private static void insertTextMessage(QualifiedId conversationId) throws JsonProcessingException {
        final String type = "conversation.otr-message-add.new-text";
        final UUID eventId = UUID.randomUUID();
        final UUID messageId = UUID.randomUUID();
        final String clientId = UUID.randomUUID().toString();
        final QualifiedId userId  = new QualifiedId(UUID.randomUUID(), FALLBACK_DOMAIN);
        final String time = new Date().toString();

        final TextMessage textMessage = new TextMessage(eventId, messageId, conversationId, clientId, userId, time);
        textMessage.addMention(new QualifiedId(UUID.randomUUID(), FALLBACK_DOMAIN), 0, 5);
        textMessage.setText("Text for: " + conversationId.id + " - " + conversationId.domain);

        String payload = mapper.writeValueAsString(textMessage);

        eventsDAO.insert(eventId, conversationId.id, conversationId.domain, userId.id, userId.domain, type, payload);
    }
}

package com.wire.bots.hold.resource.v0.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.Config;
import com.wire.bots.hold.DAO.AccessDAO;
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
import org.junit.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ConversationResourceTest {
    private static final String TOKEN = "dummy_token";
    private static final String API_HOST = "dummy_api_host";
    private static final String FALLBACK_DOMAIN = "dummy_domain";
    private static final String USER_CLIENT_ID = "deviceId1";
    private static final String USER_COOKIE = "userCookie1";

    private static final QualifiedId userId  = new QualifiedId(UUID.randomUUID(), FALLBACK_DOMAIN);
    private static final QualifiedId newConversationId = new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString());
    private static final QualifiedId oldConversationId = new QualifiedId(UUID.randomUUID(), null);
    private static final QualifiedId oldConversationIdWithDummyDomain = new QualifiedId(oldConversationId.id, FALLBACK_DOMAIN);

    private static final DropwizardTestSupport<Config> SUPPORT = new DropwizardTestSupport<>(
        Service.class,
        "hold.yaml",
        ConfigOverride.config("token", TOKEN),
        ConfigOverride.config("apiHost", API_HOST),
        ConfigOverride.config("server.applicationConnectors[0].type", "http")
    );

    private static EventsDAO eventsDAO;
    private static AccessDAO accessDAO;
    private static Client client;
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void before() throws Exception {
        SUPPORT.before();
        client = HttpTestUtils.createHttpClient(
            SUPPORT.getConfiguration(),
            SUPPORT.getEnvironment()
        );

        Service app = SUPPORT.getApplication();
        eventsDAO = app.getJdbi().onDemand(EventsDAO.class);
        accessDAO = app.getJdbi().onDemand(AccessDAO.class);

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
    public void givenRequestWithIdAndDomain_whenGettingConversationListInHtml_thenHTMLIsGeneratedCorrectly() {
        final Response response = getConversationList(
            newConversationId.id,
            newConversationId.domain,
            true
        );

        assert response.getStatus() == HttpStatus.SC_OK;
        String result = response.readEntity(String.class);

        List<Event> events = eventsDAO.listAllAsc(newConversationId.id, newConversationId.domain);
        EventModel model = new EventModel();
        model.events = events;

        String expectedResult = HtmlGenerator.execute(model, HtmlGenerator.TemplateType.CONVERSATION);
        assert expectedResult.equals(result);
    }

    @Test
    public void givenRequestOnlyWithId_whenGettingConversationListInHtml_thenHTMLIsGeneratedCorrectly() {
        Cache.setFallbackDomain(FALLBACK_DOMAIN);

        final Response response = getConversationList(
            oldConversationId.id,
            oldConversationId.domain,
            true
        );

        assert response.getStatus() == HttpStatus.SC_OK;
        String result = response.readEntity(String.class);

        List<Event> events = eventsDAO.listAllDefaultDomainAsc(oldConversationId.id, FALLBACK_DOMAIN);
        EventModel model = new EventModel();
        model.events = events;

        String expectedResult = HtmlGenerator.execute(model, HtmlGenerator.TemplateType.CONVERSATION);
        assert expectedResult.equals(result);
    }

    @Test
    public void givenRequestWithIdAndFallbackDomain_whenGettingConversationListInHtml_thenHTMLIsGeneratedCorrectly() {
        Cache.setFallbackDomain(FALLBACK_DOMAIN);

        final Response response = getConversationList(
            oldConversationIdWithDummyDomain.id,
            oldConversationIdWithDummyDomain.domain,
            true
        );

        assert response.getStatus() == HttpStatus.SC_OK;
        String result = response.readEntity(String.class);

        List<Event> events = eventsDAO.listAllDefaultDomainAsc(oldConversationIdWithDummyDomain.id, oldConversationIdWithDummyDomain.domain);
        EventModel model = new EventModel();
        model.events = events;

        String expectedResult = HtmlGenerator.execute(model, HtmlGenerator.TemplateType.CONVERSATION);
        assert expectedResult.equals(result);
    }

    @Test
    public void givenRequestWithIdAndDomain_whenGettingConversationListInPDF_thenPDFIsGeneratedCorrectly() throws Exception {
        final Response response = getConversationList(
            newConversationId.id,
            newConversationId.domain,
            false
        );

        assert response.getStatus() == HttpStatus.SC_OK;
        assert response.getMediaType().toString().equals("application/pdf");
    }

    private Response getConversationList(UUID conversationId, String conversationDomain, boolean isHtml) {
        String extraParameters = conversationId.toString();
        if (conversationDomain != null && !conversationDomain.isEmpty()) {
            extraParameters = extraParameters + "/" + conversationDomain;
        }

        return client
            .target("http://localhost:" + SUPPORT.getLocalPort())
            .path("conv")
            .path(extraParameters)
            .queryParam("html", isHtml)
            .request("application/pdf")
            .header(HttpHeaders.AUTHORIZATION, TOKEN)
            .accept(MediaType.APPLICATION_JSON)
            .get();
    }

    private static void insertDummyEvents() throws JsonProcessingException {
        // Insert User
        accessDAO.insert(
            userId.id,
            userId.domain,
            USER_CLIENT_ID,
            USER_COOKIE
        );
        accessDAO.update(
            userId.id,
            userId.domain,
            TOKEN,
            USER_COOKIE
        );

        // Date format
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        // New conversation with ID and Domain
        insertTextMessage(newConversationId, dateFormat);
        insertTextMessage(newConversationId, dateFormat);
        insertTextMessage(newConversationId, dateFormat);

        // Old conversation with ID but without Domain
        insertTextMessage(oldConversationIdWithDummyDomain, dateFormat);
        insertTextMessage(oldConversationId, dateFormat);
        insertTextMessage(oldConversationId, dateFormat);
    }

    private static void insertTextMessage(QualifiedId conversationId, DateFormat dateFormat) throws JsonProcessingException {
        final String type = "conversation.otr-message-add.new-text";
        final UUID eventId = UUID.randomUUID();
        final UUID messageId = UUID.randomUUID();
        final String time = dateFormat.format(new Date());

        final TextMessage textMessage = new TextMessage(eventId, messageId, conversationId, USER_CLIENT_ID, userId, time);
        textMessage.addMention(new QualifiedId(UUID.randomUUID(), FALLBACK_DOMAIN), 0, 5);
        textMessage.setText("Text for: " + conversationId.id + " - " + conversationId.domain);

        String payload = mapper.writeValueAsString(textMessage);

        eventsDAO.insert(eventId, conversationId.id, conversationId.domain, userId.id, userId.domain, type, payload);
    }
}

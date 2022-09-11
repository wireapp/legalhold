package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.DAO.AssetsDAO;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.model.Config;
import com.wire.bots.hold.model.Event;
import com.wire.bots.hold.model.LHAccess;
import com.wire.xenon.models.TextMessage;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

//@Ignore("integration test, needs DB")
public class DatabaseTest {
    private static final DropwizardTestSupport<Config> SUPPORT = new DropwizardTestSupport<>(
            Service.class, "hold.yaml",
            ConfigOverride.config("token", "dummy"));

    private static final ObjectMapper mapper = new ObjectMapper();
    private static AssetsDAO assetsDAO;
    private static EventsDAO eventsDAO;
    private static AccessDAO accessDAO;

    @BeforeClass
    public static void init() throws Exception {
        SUPPORT.before();
        Service app = SUPPORT.getApplication();

        eventsDAO = app.getJdbi().onDemand(EventsDAO.class);
        assetsDAO = app.getJdbi().onDemand(AssetsDAO.class);
        accessDAO = app.getJdbi().onDemand(AccessDAO.class);
    }

    @AfterClass
    public static void afterClass() {
        SUPPORT.after();
    }

    @Test
    public void eventsTextMessageTest() throws JsonProcessingException {
        final String type = "conversation.otr-message-add.new-text";
        final UUID eventId = UUID.randomUUID();
        final UUID messageId = UUID.randomUUID();
        final UUID convId = UUID.randomUUID();
        final String clientId = UUID.randomUUID().toString();
        final UUID userId = UUID.randomUUID();
        final String time = new Date().toString();
        final TextMessage textMessage = new TextMessage(eventId, messageId, convId, clientId, userId, time);
        textMessage.addMention(UUID.randomUUID().toString(), 0, 5);
        textMessage.setText("Some text");

         int insert = eventsDAO.insert(eventId, convId, type, mapper.writeValueAsString(textMessage));
        assert insert == 1;

        insert = eventsDAO.insert(UUID.randomUUID(), convId, type, mapper.writeValueAsString(textMessage));
        assert insert == 1;

        final Event event = eventsDAO.get(eventId);

        TextMessage message = mapper.readValue(event.payload, TextMessage.class);

        assert textMessage.getMessageId().equals(message.getMessageId());

        List<Event> events = eventsDAO.listAllUnxported(convId);
        assert events.size() == 2;
    }

    @Test
    public void assetsTest() {
        final String mimetype = "image/jpeg";
        final UUID messageId = UUID.randomUUID();
        final byte[] image = new byte[1024];

        new Random().nextBytes(image);

        assetsDAO.insert(messageId, mimetype);
        assetsDAO.insert(messageId, image);

        final AssetsDAO.Asset asset = assetsDAO.get(messageId);
        final boolean deepEquals = Objects.deepEquals(image, asset.data);
        final boolean equals = Objects.equals(messageId, asset.messageId);
        final boolean equals1 = Objects.equals(mimetype, asset.mimeType);
    }

    @Test
    public void accessTests() {
        final UUID userId = UUID.randomUUID();
        final String clientId = UUID.randomUUID().toString();
        final String cookie = "cookie";
        final UUID last = UUID.randomUUID();
        final String cookie2 = "cookie2";
        final String token = "token";

        final int insert = accessDAO.insert(userId, clientId, cookie);
        accessDAO.updateLast(userId, last);
        accessDAO.update(userId, token, cookie2);

        final LHAccess lhAccess = accessDAO.get(userId);

        accessDAO.disable(userId);

        final int insert2 = accessDAO.insert(userId, clientId, cookie);
        final LHAccess lhAccess2 = accessDAO.get(userId);

    }
}

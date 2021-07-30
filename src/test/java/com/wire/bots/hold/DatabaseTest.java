package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AssetsDAO;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.model.Config;
import com.wire.bots.hold.model.Event;
import com.wire.xenon.models.TextMessage;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

//@Ignore("integration test, needs DB")
public class DatabaseTest {
    private static EventsDAO eventsDAO;
    private static final DropwizardTestSupport<Config> SUPPORT = new DropwizardTestSupport<>(
            Service.class, "hold.yaml",
            ConfigOverride.config("token", "dummy"));

    private static final ObjectMapper mapper = new ObjectMapper();
    private static AssetsDAO assetsDAO;

    @BeforeClass
    public static void init() throws Exception {
        SUPPORT.before();
        Service app = SUPPORT.getApplication();

        eventsDAO = app.getJdbi().onDemand(EventsDAO.class);
        assetsDAO = app.getJdbi().onDemand(AssetsDAO.class);

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

        final int insert = eventsDAO.insert(eventId, convId, type, mapper.writeValueAsString(textMessage));
        assert insert == 1;

        final Event event = eventsDAO.get(eventId);

        TextMessage message = mapper.readValue(event.payload, TextMessage.class);

        assert textMessage.getMessageId().equals(message.getMessageId());
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
}

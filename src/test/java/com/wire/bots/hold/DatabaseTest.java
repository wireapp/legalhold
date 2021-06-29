package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.model.Config;
import com.wire.bots.hold.model.Event;
import com.wire.xenon.models.TextMessage;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

//@Ignore("integration test, needs DB")
public class DatabaseTest {
    private static EventsDAO eventsDAO;
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void init() {
        Config.Database db = new Config.Database();
        db.setUrl("jdbc:postgresql://localhost/legalhold");
        db.setDriverClass("org.postgresql.Driver");
        Environment env = new Environment("DatabaseTest");

        final ManagedDataSource dataSource = db.build(env.metrics(), "DatabaseTest");
        final Jdbi jdbi = Jdbi.create(dataSource)
                .installPlugin(new SqlObjectPlugin());

        eventsDAO = jdbi.onDemand(EventsDAO.class);
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

        final int insert = eventsDAO.insert(messageId, convId, type, mapper.writeValueAsString(textMessage));
        assert insert == 1;

        final Event event = eventsDAO.get(messageId);

        TextMessage message = mapper.readValue(event.payload, TextMessage.class);

        assert textMessage.getMessageId().equals(message.getMessageId());
    }

}

package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.Event;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


public class EventsResultSetMapper implements ResultSetMapper<Event> {
    @Override
    public Event map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
        Event event = new Event();
        event.messageId = (UUID) rs.getObject("messageId");
        event.conversationId = (UUID) rs.getObject("conversationId");
        event.type = rs.getString("type");
        event.time = rs.getString("time");
        event.payload = rs.getString("payload");
        return event;
    }
}

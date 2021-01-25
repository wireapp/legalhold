package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.Event;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EventsResultSetMapper implements ColumnMapper<Event> {
    @Override
    public Event map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
        Event event = new Event();
        Object conversationId = rs.getObject("conversationId");
        if (conversationId != null)
            event.conversationId = (UUID) conversationId;
        event.time = rs.getString("time");
        event.type = rs.getString("type");
        event.payload = rs.getString("payload");
        Object messageId = rs.getObject("messageId");
        if (messageId != null)
            event.messageId = (UUID) messageId;

        return event;
    }
}

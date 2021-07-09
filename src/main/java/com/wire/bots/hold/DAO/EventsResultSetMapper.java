package com.wire.bots.hold.DAO;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.model.Event;
import com.wire.xenon.tools.Logger;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class EventsResultSetMapper implements ColumnMapper<Event> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Event map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
        Event event = new Event();
        Object conversationId = rs.getObject("conversationId");
        if (conversationId != null)
            event.conversationId = (UUID) conversationId;
        event.time = rs.getString("time");
        event.type = rs.getString("type");
        event.payload = getPayload(rs);
        Object messageId = rs.getObject("messageId");
        if (messageId != null)
            event.eventId = (UUID) messageId;

        return event;
    }

    private String getPayload(ResultSet rs) throws SQLException {
        try {
            JsonParser jsonParser = mapper.getFactory().createParser(rs.getString("payload"));
            final TreeNode treeNode = jsonParser.readValueAsTree();
            JsonNode node = (JsonNode) treeNode;
            return node.asText();
        } catch (IOException e) {
            Logger.exception("getPayload", e);
            return null;
        }
    }
}

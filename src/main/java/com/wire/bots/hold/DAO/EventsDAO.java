package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.database.Event;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface EventsDAO {
    @SqlUpdate("INSERT INTO Events (eventId, conversationId, userId, type, payload, time) " +
            "VALUES (:eventId, :conversationId, :userId, :type, to_jsonb(:payload)::json, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (eventId) DO NOTHING")
    int insert(@Bind("eventId") UUID eventId,
               @Bind("conversationId") UUID conversationId,
               @Bind("userId") UUID userId,
               @Bind("type") String type,
               @Bind("payload") String payload);

    @SqlQuery("SELECT * FROM Events WHERE eventId = :eventId")
    @RegisterColumnMapper(EventsResultSetMapper.class)
    Event get(@Bind("eventId") UUID eventId);

    @SqlQuery("SELECT * FROM Events WHERE conversationId = :conversationId ORDER BY time DESC")
    @RegisterColumnMapper(EventsResultSetMapper.class)
    List<Event> listAll(@Bind("conversationId") UUID conversationId);

    @SqlQuery("SELECT * FROM Events WHERE conversationId = :conversationId ORDER BY time ASC")
    @RegisterColumnMapper(EventsResultSetMapper.class)
    List<Event> listAllAsc(@Bind("conversationId") UUID conversationId);

    @SqlQuery("SELECT DISTINCT conversationId, MAX(time) AS time " +
            "FROM Events " +
            "GROUP BY conversationId " +
            "ORDER BY MAX(time) DESC, conversationId " +
            "LIMIT 400")
    @RegisterColumnMapper(_EventsResultSetMapper.class)
    List<Event> listConversations();

    @SqlUpdate("DELETE FROM Events WHERE eventId = :eventId")
    int delete(@Bind("eventId") UUID eventId);

    class _EventsResultSetMapper implements ColumnMapper<Event> {
        @Override
        public Event map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            Event event = new Event();
            Object conversationId = rs.getObject("conversationId");
            if (conversationId != null)
                event.conversationId = (UUID) conversationId;
            event.time = rs.getString("time");

            return event;
        }
    }
}

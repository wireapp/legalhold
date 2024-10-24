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
    @SqlUpdate("INSERT INTO Events (eventId, conversationId, conversationDomain, userId, userDomain, type, payload, time) " +
            "VALUES (:eventId, :conversationId, :conversationDomain, :userId, :userDomain, :type, to_jsonb(:payload)::json, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (eventId) DO NOTHING")
    int insert(@Bind("eventId") UUID eventId,
               @Bind("conversationId") UUID conversationId,
               @Bind("conversationDomain") String conversationDomain,
               @Bind("userId") UUID userId,
               @Bind("userDomain") String userDomain,
               @Bind("type") String type,
               @Bind("payload") String payload);

    @SqlQuery("SELECT * FROM Events WHERE eventId = :eventId")
    @RegisterColumnMapper(EventsResultSetMapper.class)
    Event get(@Bind("eventId") UUID eventId);

    @SqlQuery("SELECT * FROM Events WHERE conversationId = :conversationId AND (conversationDomain IS NULL OR conversationDomain = :conversationDomain) ORDER BY time DESC")
    @RegisterColumnMapper(EventsResultSetMapper.class)
    List<Event> listAllDefaultDomain(@Bind("conversationId") UUID conversationId,
        @Bind("conversationDomain") String conversationDomain);

    @SqlQuery("SELECT * FROM Events WHERE conversationId = :conversationId AND conversationDomain = :conversationDomain ORDER BY time DESC")
    @RegisterColumnMapper(EventsResultSetMapper.class)
    List<Event> listAll(@Bind("conversationId") UUID conversationId,
        @Bind("conversationDomain") String conversationDomain);

    @SqlQuery("SELECT * FROM Events WHERE conversationId = :conversationId AND (conversationDomain IS NULL OR conversationDomain = :conversationDomain) ORDER BY time ASC")
    @RegisterColumnMapper(EventsResultSetMapper.class)
    List<Event> listAllDefaultDomainAsc(@Bind("conversationId") UUID conversationId,
        @Bind("conversationDomain") String conversationDomain);

    @SqlQuery("SELECT * FROM Events WHERE conversationId = :conversationId AND conversationDomain = :conversationDomain ORDER BY time ASC")
    @RegisterColumnMapper(EventsResultSetMapper.class)
    List<Event> listAllAsc(@Bind("conversationId") UUID conversationId,
        @Bind("conversationDomain") String conversationDomain);

    @SqlQuery("SELECT DISTINCT conversationId, conversationDomain, MAX(time) AS time " +
            "FROM Events " +
            "GROUP BY conversationId, conversationDomain " +
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
            event.conversationDomain = rs.getString("conversationDomain");
            event.time = rs.getString("time");

            return event;
        }
    }
}

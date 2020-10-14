package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.Event;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface EventsDAO {
    @SqlUpdate("INSERT INTO Events (messageId, conversationId, type, payload, time) " +
            "VALUES (:messageId, :conversationId, :type, :payload, CURRENT_TIMESTAMP) ON CONFLICT (messageId) DO NOTHING")
    int insert(@Bind("messageId") UUID messageId,
               @Bind("conversationId") UUID conversationId,
               @Bind("type") String type,
               @Bind("payload") String payload);

    @SqlQuery("SELECT * FROM Events WHERE messageId = :messageId")
    @RegisterMapper(EventsResultSetMapper.class)
    Event get(@Bind("messageId") UUID messageId);

    @SqlQuery("SELECT * FROM Events WHERE conversationId = :conversationId ORDER BY time DESC")
    @RegisterMapper(EventsResultSetMapper.class)
    List<Event> listAll(@Bind("conversationId") UUID conversationId);

    @SqlQuery("SELECT * FROM Events WHERE conversationId = :conversationId ORDER BY time ASC")
    @RegisterMapper(EventsResultSetMapper.class)
    List<Event> listAllAsc(@Bind("conversationId") UUID conversationId);

    @SqlQuery("SELECT DISTINCT conversationId, MAX(time) AS time " +
            "FROM Events " +
            "GROUP BY conversationId " +
            "ORDER BY MAX(time) DESC, conversationId " +
            "LIMIT 400")
    @RegisterMapper(_EventsResultSetMapper.class)
    List<Event> listConversations();

    class _EventsResultSetMapper implements ResultSetMapper<Event> {
        @Override
        public Event map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
            Event event = new Event();
            Object conversationId = rs.getObject("conversationId");
            if (conversationId != null)
                event.conversationId = (UUID) conversationId;
            event.time = rs.getString("time");

            return event;
        }
    }
}

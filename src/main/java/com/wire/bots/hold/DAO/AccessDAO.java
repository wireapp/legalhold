package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.LHAccess;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.UUID;

public interface AccessDAO {
    @SqlUpdate("INSERT INTO Access (userId, clientId, cookie, updated, created, enabled) " +
            "VALUES (:userId, :clientId, :cookie, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1) " +
            "ON CONFLICT (userId) DO UPDATE SET cookie = EXCLUDED.cookie, clientId = EXCLUDED.clientId, " +
            "updated = EXCLUDED.updated, enabled = EXCLUDED.enabled")
    int insert(@Bind("userId") UUID userId,
               @Bind("clientId") String clientId,
               @Bind("cookie") String cookie);

    @SqlUpdate("UPDATE Access SET enabled = 0, updated = CURRENT_TIMESTAMP WHERE userId = :userId")
    int disable(@Bind("userId") UUID userId);

    @SqlUpdate("UPDATE Access SET token = :token, cookie = :cookie, updated = CURRENT_TIMESTAMP WHERE userId = :userId")
    int update(@Bind("userId") UUID userId,
               @Bind("token") String token,
               @Bind("cookie") String cookie);

    @SqlUpdate("UPDATE Access SET last = :last, updated = CURRENT_TIMESTAMP WHERE userId = :userId")
    int updateLast(@Bind("userId") UUID userId,
                   @Bind("last") UUID last);

    @SqlQuery("SELECT * FROM Access WHERE token IS NOT NULL AND enabled = 1 ORDER BY created DESC LIMIT 1")
    @RegisterColumnMapper(AccessResultSetMapper.class)
    LHAccess getSingle();

    @SqlQuery("SELECT * FROM Access WHERE userId = :userId")
    @RegisterColumnMapper(AccessResultSetMapper.class)
    LHAccess get(@Bind("userId") UUID userId);

    @SqlQuery("SELECT * FROM Access WHERE enabled = 1 ORDER BY created DESC")
    @RegisterColumnMapper(AccessResultSetMapper.class)
    List<LHAccess> listEnabled();

    @SqlQuery("SELECT * FROM Access ORDER BY created DESC")
    @RegisterColumnMapper(AccessResultSetMapper.class)
    List<LHAccess> listAll();

    @SqlQuery("SELECT * FROM Access ORDER BY created DESC LIMIT :count")
    @RegisterColumnMapper(AccessResultSetMapper.class)
    List<LHAccess> list(@Bind("count") int count);

    @SqlQuery("SELECT * FROM Access WHERE created < :created :: TIMESTAMP ORDER BY created DESC LIMIT :count")
    @RegisterColumnMapper(AccessResultSetMapper.class)
    List<LHAccess> list(@Bind("count") int count,
                        @Bind("created") String created);
}

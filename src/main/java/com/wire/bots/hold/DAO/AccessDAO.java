package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.LHAccess;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;
import java.util.UUID;

public interface AccessDAO {
    @SqlUpdate("INSERT INTO Access (userId, clientId, cookie, updated, created) " +
            "VALUES (:userId, :clientId, :cookie, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (userId) DO UPDATE SET cookie = EXCLUDED.cookie, clientId = EXCLUDED.clientId, " +
            "updated = EXCLUDED.updated")
    int insert(@Bind("userId") UUID userId,
               @Bind("clientId") String clientId,
               @Bind("cookie") String cookie);

    @SqlUpdate("DELETE FROM Access WHERE userId = :userId")
    int remove(@Bind("userId") UUID userId);

    @SqlUpdate("UPDATE Access SET token = :token, cookie = :cookie, updated = CURRENT_TIMESTAMP WHERE userId = :userId")
    int update(@Bind("userId") UUID userId,
               @Bind("token") String token,
               @Bind("cookie") String cookie);

    @SqlUpdate("UPDATE Access SET last = :last, updated = CURRENT_TIMESTAMP WHERE userId = :userId")
    int updateLast(@Bind("userId") UUID userId,
                   @Bind("last") UUID last);

    @SqlQuery("SELECT * FROM Access WHERE token IS NOT NULL ORDER BY created DESC LIMIT 1")
    @RegisterMapper(AccessResultSetMapper.class)
    LHAccess getSingle();

    @SqlQuery("SELECT * FROM Access ORDER BY created DESC")
    @RegisterMapper(AccessResultSetMapper.class)
    List<LHAccess> listAll();

    @SqlQuery("SELECT * FROM Access ORDER BY created DESC LIMIT :count")
    @RegisterMapper(AccessResultSetMapper.class)
    List<LHAccess> list(@Bind("count") int count);
}

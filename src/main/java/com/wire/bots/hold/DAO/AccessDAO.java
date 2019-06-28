package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.Access;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;
import java.util.UUID;

public interface AccessDAO {
    @SqlUpdate("INSERT INTO Hold_Tokens (userId, clientId, cookie, updated, created) " +
            "VALUES (:userId, :clientId, :cookie, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON CONFLICT (userId) DO UPDATE SET cookie = EXCLUDED.cookie, clientId = EXCLUDED.clientId, " +
            "updated = EXCLUDED.updated")
    int insert(@Bind("userId") UUID userId,
               @Bind("clientId") String clientId,
               @Bind("cookie") String cookie);

    @SqlUpdate("DELETE FROM Hold_Tokens WHERE userId = :userId")
    int remove(@Bind("userId") UUID userId);

    @SqlUpdate("UPDATE Hold_Tokens SET token = :token, cookie = :cookie, updated = CURRENT_TIMESTAMP WHERE userId = :userId")
    int update(@Bind("userId") UUID userId,
               @Bind("token") String token,
               @Bind("cookie") String cookie);

    @SqlUpdate("UPDATE Hold_Tokens SET last = :last, updated = CURRENT_TIMESTAMP WHERE userId = :userId")
    int updateLast(@Bind("userId") UUID userId,
                   @Bind("last") UUID last);

    @SqlQuery("SELECT * FROM Hold_Tokens ORDER BY created DESC")
    @RegisterMapper(AccessResultSetMapper.class)
    List<Access> listAll();

    @SqlQuery("SELECT * FROM Hold_Tokens ORDER BY created DESC LIMIT :count")
    @RegisterMapper(AccessResultSetMapper.class)
    List<Access> list(@Bind("count") int count);
}

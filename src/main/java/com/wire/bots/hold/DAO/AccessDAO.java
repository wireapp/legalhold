package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.Access;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;
import java.util.UUID;

public interface AccessDAO {
    @SqlUpdate("INSERT INTO Hold_Tokens (userId, clientId, token, cookie, timestamp, created) " +
            "VALUES (:userId, :clientId, :token, :cookie, :timestamp, :timestamp)")
    int insert(@Bind("userId") UUID userId,
               @Bind("clientId") String clientId,
               @Bind("token") String token,
               @Bind("cookie") String cookie,
               @Bind("timestamp") int timestamp);

    @SqlUpdate("DELETE FROM Hold_Tokens WHERE userId = :userId")
    int remove(@Bind("userId") UUID userId);

    @SqlUpdate("UPDATE Hold_Tokens SET token = :token, cookie = :cookie, timestamp = :timestamp WHERE userId = :userId")
    int update(@Bind("userId") UUID userId,
               @Bind("token") String token,
               @Bind("cookie") String cookie,
               @Bind("timestamp") int timestamp);

    @SqlUpdate("UPDATE Hold_Tokens SET last = :last, timestamp = :timestamp WHERE userId = :userId")
    int updateLast(@Bind("userId") UUID userId,
                   @Bind("last") String last,
                   @Bind("timestamp") int timestamp);

    @SqlQuery("SELECT * FROM Hold_Tokens")
    @RegisterMapper(AccessResultSetMapper.class)
    List<Access> listAll();

    @SqlQuery("SELECT * FROM Hold_Tokens ORDER BY timestamp DESC limit :count")
    @RegisterMapper(AccessResultSetMapper.class)
    List<Access> list(@Bind int count);
}

package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.Metadata;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface MetadataDAO {
    String FALLBACK_DOMAIN_KEY = "FALLBACK_DOMAIN_KEY";

    @SqlUpdate("INSERT INTO Metadata (key, value)" +
        "VALUES (:key, :value)" +
        "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value")
    int insert(@Bind("key") String key, @Bind("value") String value);

    @SqlQuery("SELECT key, value FROM Metadata WHERE key = :key LIMIT 1")
    @RegisterColumnMapper(MetadataResultSetMapper.class)
    Metadata get(@Bind("key") String key);
}

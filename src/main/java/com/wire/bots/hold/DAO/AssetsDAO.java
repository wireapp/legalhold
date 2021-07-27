package com.wire.bots.hold.DAO;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public interface AssetsDAO {
    @SqlUpdate("INSERT INTO Assets (messageId, mimetype) " +
            "VALUES (:messageId, :mimetype) " +
            "ON CONFLICT (messageId) DO UPDATE SET mimetype = EXCLUDED.mimetype")
    int insert(@Bind("messageId") UUID messageId,
               @Bind("mimetype") String mimetype);

    @SqlUpdate("INSERT INTO Assets (messageId, data) " +
            "VALUES (:messageId, :data) " +
            "ON CONFLICT (messageId) DO UPDATE SET data = EXCLUDED.data")
    int insert(@Bind("messageId") UUID messageId,
               @Bind("data") byte[] data);

    @SqlQuery("SELECT messageId, mimetype, data FROM Assets WHERE messageId = :messageId")
    @RegisterColumnMapper(_Mapper.class)
    Asset get(@Bind("messageId") UUID messageId);

    class _Mapper implements ColumnMapper<Asset> {
        @Override
        public Asset map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
            Asset asset = new Asset();
            asset.messageId = (UUID) rs.getObject("messageId");
            asset.mimeType = rs.getString("mimetype");
            asset.data = rs.getBytes("data");

            return asset;
        }
    }

    public class Asset {
        public UUID messageId;
        public String mimeType;
        public byte[] data;
    }
}

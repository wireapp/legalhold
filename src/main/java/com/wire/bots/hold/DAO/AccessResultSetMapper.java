package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.database.LHAccess;
import com.wire.xenon.backend.models.QualifiedId;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class AccessResultSetMapper implements ColumnMapper<LHAccess> {
    @Override
    public LHAccess map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
        LHAccess LHAccess = new LHAccess();
        LHAccess.last = (UUID) rs.getObject("last");
        UUID userId = (UUID) rs.getObject("userId");
        String userDomain = rs.getString("userDomain");
        LHAccess.userId = new QualifiedId(userId, userDomain);
        LHAccess.clientId = rs.getString("clientId");
        LHAccess.token = rs.getString("token");
        LHAccess.cookie = rs.getString("cookie");
        LHAccess.updated = rs.getString("updated");
        LHAccess.created = rs.getString("created");
        LHAccess.enabled = rs.getInt("enabled") == 1;
        return LHAccess;
    }
}

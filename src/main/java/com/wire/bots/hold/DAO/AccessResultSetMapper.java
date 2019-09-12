package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.LHAccess;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


public class AccessResultSetMapper implements ResultSetMapper<LHAccess> {
    @Override
    public LHAccess map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
        LHAccess LHAccess = new LHAccess();
        LHAccess.last = (UUID) rs.getObject("last");
        LHAccess.userId = (UUID) rs.getObject("userId");
        LHAccess.clientId = rs.getString("clientId");
        LHAccess.token = rs.getString("token");
        LHAccess.cookie = rs.getString("cookie");
        LHAccess.updated = rs.getString("updated");
        LHAccess.created = rs.getString("created");
        LHAccess.enabled = rs.getInt("enabled") == 1;
        return LHAccess;
    }
}

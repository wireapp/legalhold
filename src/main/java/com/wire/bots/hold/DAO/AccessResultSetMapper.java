package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.Access;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


public class AccessResultSetMapper implements ResultSetMapper<Access> {
    @Override
    public Access map(int i, ResultSet rs, StatementContext statementContext) throws SQLException {
        Access access = new Access();
        access.last = (UUID) rs.getObject("last");
        access.userId = (UUID) rs.getObject("userId");
        access.clientId = rs.getString("clientId");
        access.token = rs.getString("token");
        access.cookie = rs.getString("cookie");
        access.updated = rs.getString("updated");
        access.created = rs.getString("created");
        return access;
    }
}

package com.wire.bots.hold.DAO;

import com.wire.bots.hold.model.Metadata;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MetadataResultSetMapper implements ColumnMapper<Metadata> {

    @Override
    public Metadata map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
        return new Metadata(
            rs.getString("key"),
            rs.getString("value")
        );
    }
}

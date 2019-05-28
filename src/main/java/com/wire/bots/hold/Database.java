package com.wire.bots.hold;

import com.wire.bots.sdk.Configuration;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class Database {
    private final Configuration.DB conf;

    Database(Configuration.DB conf) {
        this.conf = conf;
    }

    boolean insertTextRecord(UUID conversationId, UUID messageId, UUID senderId, String time, String text)
            throws SQLException {
        try (Connection c = newConnection()) {
            String sql = "INSERT INTO Hold (conversationId, messageId, senderId, mimeType, text, timestamp)" +
                    " VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setObject(1, conversationId);
            stmt.setObject(2, messageId);
            stmt.setObject(3, senderId);
            stmt.setString(4, "txt");
            stmt.setString(5, text);
            stmt.setInt(6, (int) (new Date().getTime() / 1000));
            return stmt.executeUpdate() == 1;
        }
    }

    boolean insertAssetRecord(UUID conversationId, UUID messageId, UUID senderId, String mimeType, String uri)
            throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("conversationId, messageId, senderId, mimeType, uri, timestamp" +
                    " VALUES (?, ?, ?, ?, ?, ?)");
            stmt.setObject(1, conversationId);
            stmt.setObject(2, messageId);
            stmt.setObject(3, senderId);
            stmt.setString(4, mimeType);
            stmt.setString(5, uri);
            stmt.setInt(6, (int) (new Date().getTime() / 1000));
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean insertAccess(UUID userId, String clientId, String token, String cookie)
            throws SQLException {
        try (Connection c = newConnection()) {
            String sql = "INSERT INTO Hold_Tokens (userId, clientId, token, cookie, timestamp)" +
                    " VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = c.prepareStatement(sql);
            stmt.setObject(1, userId);
            stmt.setString(2, clientId);
            stmt.setString(3, token);
            stmt.setString(4, cookie);
            stmt.setInt(5, (int) (new Date().getTime() / 1000));
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean removeAccess(UUID userId) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("DELETE FROM Hold_Tokens WHERE userId = ?");
            stmt.setObject(1, userId);
            return stmt.executeUpdate() == 1;
        }
    }

    boolean updateAccess(UUID userId, String token, String cookie) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("UPDATE Hold_Tokens SET token = ?, cookie = ? WHERE userId = ?");
            stmt.setString(1, token);
            stmt.setString(2, cookie);
            stmt.setObject(3, userId);
            return stmt.executeUpdate() == 1;
        }
    }

    public ArrayList<_Access> getAccess() throws SQLException {
        ArrayList<_Access> ret = new ArrayList<>();
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT * FROM Hold_Tokens");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                _Access access = new _Access();
                access.userId = (UUID) rs.getObject("userId");
                access.clientId = rs.getString("clientId");
                access.token = rs.getString("token");
                access.cookie = rs.getString("cookie");
                access.last = rs.getString("last");
                access.timestamp = rs.getLong("timestamp");
                ret.add(access);
            }
        }
        return ret;
    }

    private Connection newConnection() throws SQLException {
        String url = String.format("jdbc:%s://%s:%d/%s", conf.driver, conf.host, conf.port, conf.database);
        return DriverManager.getConnection(url, conf.user, conf.password);
    }

    boolean updateLast(UUID userId, String last) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("UPDATE Hold_Tokens SET last = ? WHERE userId = ?");
            stmt.setString(1, last);
            stmt.setObject(2, userId);
            return stmt.executeUpdate() == 1;
        }
    }

    public static class _Access {
        public String last;
        public UUID userId;
        public String clientId;
        public String token;
        public String cookie;
        public Long timestamp;
    }
}

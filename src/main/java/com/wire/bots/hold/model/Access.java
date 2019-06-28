package com.wire.bots.hold.model;

import java.util.UUID;

public class Access {
    public UUID last;
    public UUID userId;
    public String clientId;
    public String token;
    public String cookie;
    public String updated;
    public String created;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}

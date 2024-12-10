package com.wire.bots.hold.model.database;

import com.wire.xenon.backend.models.QualifiedId;

import java.util.UUID;

public class LHAccess {
    public UUID last;
    public QualifiedId userId;
    public String clientId;
    public String token;
    public String cookie;
    public boolean mlsClientCreated;
    public Integer mlsCiphersuite;
    public String updated;
    public String created;
    public boolean enabled;
}

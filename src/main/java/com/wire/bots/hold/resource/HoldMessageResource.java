package com.wire.bots.hold.resource;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.hold.utils.HoldClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.server.resources.MessageResource;
import com.wire.bots.sdk.tools.AuthValidator;

import java.util.UUID;

public class HoldMessageResource extends MessageResource {
    private final HoldClientRepo repo;

    public HoldMessageResource(MessageHandlerBase handler, AuthValidator validator, HoldClientRepo repo) {
        super(handler, validator, repo);
        this.repo = repo;
    }

    protected WireClient getWireClient(String userId, Payload payload) throws CryptoException {
        UUID uuid = UUID.fromString(userId);
        return repo.getClient(uuid, payload.data.recipient, payload.convId);
    }
}

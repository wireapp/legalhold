package com.wire.bots.hold.utils;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.LHAccess;
import com.wire.helium.API;
import com.wire.xenon.WireClient;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.client.Client;
import java.util.UUID;

public class HoldClientRepo {

    private final Jdbi jdbi;
    private final CryptoFactory cf;
    private final Client httpClient;

    public HoldClientRepo(Jdbi jdbi, CryptoFactory cf, Client httpClient) {
        this.jdbi = jdbi;
        this.cf = cf;
        this.httpClient = httpClient;
    }

    public WireClient getClient(UUID userId, String deviceId, UUID convId) throws CryptoException {
        Crypto crypto = cf.create(userId);
        final LHAccess single = jdbi.onDemand(AccessDAO.class).get(userId);
        final API api = new LegalHoldAPI(httpClient, convId, single.token);
        return new HoldWireClient(userId, deviceId, convId, crypto, api);
    }
}

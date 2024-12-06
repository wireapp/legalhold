package com.wire.bots.hold.utils;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.database.LHAccess;
import com.wire.helium.API;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.crypto.mls.CryptoMlsClient;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.client.Client;

public class HoldClientRepo {

    private final Jdbi jdbi;
    private final CryptoDatabaseFactory cf;
    private final Client httpClient;
    private final String coreCryptoPassword;

    public HoldClientRepo(Jdbi jdbi, CryptoDatabaseFactory cf, Client httpClient, String coreCryptoPassword) {
        this.jdbi = jdbi;
        this.cf = cf;
        this.httpClient = httpClient;
        this.coreCryptoPassword = coreCryptoPassword;
    }

    public WireClient getClient(QualifiedId userId, String deviceId, QualifiedId conversationId) throws CryptoException {
        Crypto crypto = cf.create(userId);
        final LHAccess single = jdbi.onDemand(AccessDAO.class).get(userId.id, userId.domain);
        final API api = new API(httpClient, conversationId, single.token);

        // for receiving notifications
        final CryptoMlsClient cryptoMlsClient = new CryptoMlsClient(deviceId, userId, coreCryptoPassword);
        return new HoldWireClient(userId, deviceId, conversationId, cryptoMlsClient, crypto, api);
    }
}

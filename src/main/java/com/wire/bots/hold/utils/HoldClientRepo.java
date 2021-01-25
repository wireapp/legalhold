package com.wire.bots.hold.utils;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.lithium.ClientRepo;
import com.wire.xenon.WireClient;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;

import java.util.UUID;

public class HoldClientRepo extends ClientRepo {

    public HoldClientRepo(CryptoFactory cf) {
        super(null, cf, null);
    }

    public WireClient getClient(UUID userId, String deviceId, UUID convId) throws CryptoException {
        Crypto crypto = cf.create(userId);
        return new HoldWireClient(userId, deviceId, convId, crypto);
    }

    @Override
    public void purgeBot(UUID botId) {
    }
}

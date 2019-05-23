package com.wire.bots.hold.utils;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;

import java.util.UUID;

public class HoldClientRepo extends ClientRepo {

    public HoldClientRepo(CryptoFactory cf) {
        super(null, cf, null);
    }

    public WireClient getClient(UUID userId, String deviceId, UUID convId) throws CryptoException {
        Crypto crypto = cf.create(userId.toString());
        return new HoldWireClient(userId, deviceId, convId, crypto);
    }
}

package com.wire.bots.hold.utils;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.xenon.WireClient;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;

import java.util.UUID;

public class HoldClientRepo {

    private final CryptoFactory cf;

    public HoldClientRepo(CryptoFactory cf) {
        this.cf = cf;
    }

    public WireClient getClient(UUID userId, String deviceId, UUID convId) throws CryptoException {
        Crypto crypto = cf.create(userId);
        return new HoldWireClient(userId, deviceId, convId, crypto);
    }
}

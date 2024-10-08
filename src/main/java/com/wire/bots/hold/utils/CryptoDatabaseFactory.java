package com.wire.bots.hold.utils;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;

public interface CryptoDatabaseFactory extends CryptoFactory {
    Crypto create(QualifiedId userId) throws CryptoException;
}

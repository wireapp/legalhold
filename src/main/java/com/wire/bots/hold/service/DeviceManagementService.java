package com.wire.bots.hold.service;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.dto.InitializedDeviceDTO;
import com.wire.bots.hold.utils.CryptoDatabaseFactory;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.models.otr.PreKey;
import com.wire.xenon.tools.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static com.wire.bots.hold.utils.Tools.hexify;

public class DeviceManagementService {
    private final CryptoDatabaseFactory cf;
    private final AccessDAO accessDAO;

    public DeviceManagementService(AccessDAO accessDAO, CryptoDatabaseFactory cf) {
        this.accessDAO = accessDAO;
        this.cf = cf;
    }

    /**
     * Initializes cryptobox for a new user on LegalHold machine.
     *
     * <p>
     *     Generates Prekeys and sends them back to the backend in order to be distributed by other clients
     *     interacting with this user
     * </p>
     * @param userId user setup to be put under legal hold
     * @param teamId user's own team
     * @return generated prekeys
     * @throws IOException
     * @throws CryptoException
     */
    public InitializedDeviceDTO initiateLegalHoldDevice(QualifiedId userId, UUID teamId) throws IOException, CryptoException {
        try (Crypto crypto = cf.create(userId)) {
            ArrayList<PreKey> preKeys = crypto.newPreKeys(0, 50);
            PreKey lastKey = crypto.newLastPreKey();
            byte[] fingerprint = crypto.getLocalFingerprint();

            InitializedDeviceDTO device = new InitializedDeviceDTO(preKeys, lastKey, hexify(fingerprint));

            Logger.info("InitiateLegalHoldDevice: team: %s, user: %s", teamId, userId);

            return device;
        } catch (Exception e) {
            Logger.exception(e, "InitiateLegalHoldDevice: %s", e.getMessage());
            throw e;
        }
    }

    /**
     * Confirm a user's device under legal hold.
     *
     * <p>
     *     Stores the refreshToken in order to fetch user notifications while under legal hold
     * </p>
     * @param userId user setup to be put under legal hold
     * @param teamId user's own team
     * @param clientId user's device
     * @param refreshToken token used to get expiring api tokens
     */
    public void confirmDevice(QualifiedId userId, UUID teamId, String clientId, String refreshToken) {
        int insert = accessDAO.insert(userId.id,
            clientId,
            refreshToken);

        if (0 == insert) {
            Logger.error("ConfirmResource: Failed to insert Access %s:%s",
                userId,
                clientId);

            throw new RuntimeException("Cannot insert new device");
        }

        Logger.info("ConfirmResource: team: %s, user:%s, client: %s",
            teamId,
            userId,
            clientId);
    }

    /**
     * Remove a user from legal hold.
     * @param userId user setup to be put under legal hold
     * @param teamId user's own team
     * @throws IOException
     * @throws CryptoException
     */
    public void removeDevice(QualifiedId userId, UUID teamId) throws IOException, CryptoException {
        try (Crypto crypto = cf.create(userId)) {
            crypto.purge();

            int removeAccess = accessDAO.disable(userId.id);

            Logger.info(
                "RemoveResource: team: %s, user: %s, removed: %s",
                teamId,
                userId,
                removeAccess
            );
        } catch (Exception e) {
            Logger.exception(e, "RemoveLegalHoldDevice: %s", e.getMessage());
            throw e;
        }
    }
}

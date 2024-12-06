package com.wire.bots.hold.service;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.database.LHAccess;
import com.wire.bots.hold.model.dto.InitializedDeviceDTO;
import com.wire.bots.hold.utils.CryptoDatabaseFactory;
import com.wire.bots.hold.utils.LoginClientExtension;
import com.wire.helium.API;
import com.wire.helium.models.Access;
import com.wire.xenon.WireClientBase;
import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.crypto.mls.CryptoMlsClient;
import com.wire.xenon.models.otr.PreKey;
import com.wire.xenon.tools.Logger;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.wire.bots.hold.utils.Tools.hexify;

public class DeviceManagementService {
    private static final int KEY_PACKAGE_AMOUNT = 100;

    private final CryptoDatabaseFactory cf;
    private final AccessDAO accessDAO;
    private final Client client;
    private final String coreCryptoPassword;

    public DeviceManagementService(AccessDAO accessDAO, CryptoDatabaseFactory cf, Client client, String coreCryptoPassword) {
        this.accessDAO = accessDAO;
        this.cf = cf;
        this.client = client;
        this.coreCryptoPassword = coreCryptoPassword;
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
     * <p>Confirm a user's device under legal hold.</p>
     * <p>
     *     If MLS is enabled, then we initialize CryptoMlsClient and WireClientBase, then fetch and upload in parallel:
     *      - PublicKeys
     *      - KeyPackages
     *      - MLS Conversations
     *     Then join those filtered MLS conversations.
     *     In case any of those parallel work fails, then an exception will be thrown all the way up.
     * </p>
     * <p>
     *     Stores the refreshToken in order to fetch user notifications while under legal hold
     * </p>
     * @param userId user setup to be put under legal hold
     * @param teamId user's own team
     * @param clientId user's device
     * @param refreshToken token used to get expiring api tokens
     * @throws RuntimeException when any of the (parallel or not) MLS tasks fails. Or if inserting refreshToken to Database fails.
     */
    public void confirmDevice(QualifiedId userId, UUID teamId, String clientId, String refreshToken) throws RuntimeException {
        final Access access = LoginClientExtension.refreshToken(client, clientId, refreshToken);
        API api = new API(client, null, access.accessToken);

        if (api.isMlsEnabled()) {
            try (CryptoMlsClient cryptoMlsClient = new CryptoMlsClient(clientId, userId, coreCryptoPassword)) {
                // CryptoMlsClient will be closed from `try` with resource so there is no issue passing
                // Crypto as null, as we will not be calling wireClientBase.close()
                WireClientBase wireClientBase = new WireClientBase(api, null, cryptoMlsClient, null);

                wireClientBase.updateClientWithMlsPublicKey();

                CompletableFuture<Void> mlsKeyPackagesFuture = CompletableFuture.supplyAsync(() -> {
                    wireClientBase.uploadMlsKeyPackages(KEY_PACKAGE_AMOUNT);
                    return null;
                });
                CompletableFuture<List<Conversation>> conversationsFuture = CompletableFuture.supplyAsync(() ->
                    api.getUserConversations()
                   .stream()
                   .filter(c -> c.protocol == Conversation.Protocol.MLS)
                   .collect(Collectors.toList()));

                CompletableFuture<Void> combinedFutures = CompletableFuture.allOf(
                    mlsKeyPackagesFuture,
                    conversationsFuture
                ).exceptionally(throwable -> {
                    if (throwable instanceof CompletionException) {
                        throw new RuntimeException(throwable.getCause().getMessage());
                    } else {
                        throw new RuntimeException(throwable.getMessage());
                    }
                });

                combinedFutures.get();

                final List<Conversation> mlsConversations = conversationsFuture.get();
                Logger.info("Joining %d MLS conversations", mlsConversations.size());
                for (Conversation conversation : mlsConversations) {
                    Logger.info("Conversation ID: %s, Name: %s, GroupId: %s", conversation.id, conversation.name, conversation.mlsGroupId);
                    wireClientBase.joinMlsConversation(conversation.id, conversation.mlsGroupId);
                }
            } catch (ExecutionException exception) {
                throw new RuntimeException("ExecutionException: " + exception.getCause().getMessage());
            } catch (InterruptedException exception) {
                throw new RuntimeException("InterruptedException: " + exception.getMessage());
            }
        }

        // Proteus
        int insert = accessDAO.insert(userId.id,
            userId.domain,
            clientId,
            access.getCookie().value);

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
        // MLS
        LHAccess userAccess = accessDAO.get(userId.id, userId.domain);
        if (userAccess != null) {
            API api = new API(client, null, userAccess.token);
            if (api.isMlsEnabled()) {
                try (CryptoMlsClient cryptoMlsClient = new CryptoMlsClient(userAccess.clientId, userId, coreCryptoPassword)) {
                    cryptoMlsClient.wipe();
                }
            }
        }

        // Proteus
        try (Crypto crypto = cf.create(userId)) {
            crypto.purge();

            int removeAccess = accessDAO.disable(userId.id, userId.domain);

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

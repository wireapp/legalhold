package com.wire.bots.hold.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.hold.Config;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.DAO.MetadataDAO;
import com.wire.bots.hold.Service;
import com.wire.bots.hold.model.database.LHAccess;
import com.wire.bots.hold.utils.Cache;
import com.wire.bots.hold.utils.CryptoDatabaseFactory;
import com.wire.bots.hold.utils.HttpTestUtils;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.crypto.mls.CryptoMlsClient;
import com.wire.xenon.models.otr.Missing;
import com.wire.xenon.models.otr.PreKey;
import com.wire.xenon.models.otr.PreKeys;
import com.wire.xenon.models.otr.Recipients;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.*;

import javax.ws.rs.client.Client;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.wire.bots.hold.utils.Constant.CUSTOM_CIPHERSUITE_IDENTIFIER;
import static com.wire.bots.hold.utils.Constant.DEFAULT_CIPHERSUITE_IDENTIFIER;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

public class DeviceManagementServiceTest {
    private static final String TOKEN = "dummy";
    private static final String FALLBACK_DOMAIN = "fallback_domain";
    private static final String API_HOST = "http://localhost:8090";

    private static final DropwizardTestSupport<Config> SUPPORT = new DropwizardTestSupport<>(
        Service.class, "hold.yaml",
        ConfigOverride.config("token", TOKEN),
        ConfigOverride.config("apiHost", API_HOST));
    private static Client client;
    private static final WireMockServer wireMockServer = new WireMockServer(8090);
    private AccessDAO accessDAO;
    private DeviceManagementService deviceManagementService;

    // Consts
    private static final QualifiedId userId = new QualifiedId(UUID.randomUUID(), MetadataDAO.FALLBACK_DOMAIN_KEY);
    private static final String clientId = UUID.randomUUID().toString();
    private static final String refreshToken = UUID.randomUUID().toString();
    private static final String coreCryptoPassword = "secr3t";

    @BeforeClass
    public static void beforeClass() throws Exception {
        Cache.setFallbackDomain(FALLBACK_DOMAIN);
        SUPPORT.before();
        client = HttpTestUtils.createHttpClient(SUPPORT.getConfiguration(), SUPPORT.getEnvironment());
    }

    @AfterClass
    public static void afterClass() {
        client.close();
        Cache.setFallbackDomain(null);
        SUPPORT.after();
    }

    @Before
    public void before() throws CryptoException {
        wireMockServer.start();
        configureFor("localhost", 8090);

        stubFor(get(urlEqualTo("/api-version"))
            .willReturn(okJson(apiVersionV6)));

        CryptoDatabaseFactory cryptoFactory = mock(CryptoDatabaseFactory.class);
        when(cryptoFactory.create(userId)).thenReturn(mockedCrypto);
        accessDAO = mock(AccessDAO.class);

        deviceManagementService = new DeviceManagementService(
            accessDAO,
            cryptoFactory,
            client,
            coreCryptoPassword
        );
    }

    @After
    public void after() {
        wireMockServer.stop();
    }

    @Test
    public void givenDisabledMls_whenConfirmingProteusDevice_thenInsertToDatabase() {
        // given
        when(
            accessDAO.insert(
                userId.id,
                userId.domain,
                clientId,
                refreshToken,
                false,
                null
            )
        ).thenReturn(1);
        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(disabledMlsFeatureConfigJsonResponse)));
        stubFor(post(urlEqualTo("/v6/access?client_id=" + clientId))
            .willReturn(okJson(accessResponse)));

        // when
        deviceManagementService.confirmDevice(userId, clientId, refreshToken);

        // then
        verify(accessDAO, times(1)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            false,
            null
        );
    }

    @Test
    public void givenDisabledMls_whenConfirmingProteusDevice_thenThrowsError() {
        // given
        when(
            accessDAO.insert(
                userId.id,
                userId.domain,
                clientId,
                refreshToken,
                false,
                null
            )
        ).thenReturn(0);
        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(disabledMlsFeatureConfigJsonResponse)));

        // when
        try {
            deviceManagementService.confirmDevice(userId, clientId, refreshToken);
        } catch (Exception exception) {
            // then
            assert exception.getMessage().equals("Cannot insert new device");
        }

        // then
        verify(accessDAO, times(1)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            false,
            null
        );
    }

    @Test
    public void givenEnabledMlsAndFailingPublicKeys_whenConfirmingDevice_thenProteusDeviceIsRegistered() {
        // given
        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledMlsFeatureConfigJsonResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(jsonResponse("{\"error\":\"error from mls/public-keys\"}", 400)));
        when(
            accessDAO.insert(
                userId.id,
                userId.domain,
                clientId,
                refreshToken,
                false,
                null
            )
        ).thenReturn(1);

        // when
        deviceManagementService.confirmDevice(userId, clientId, refreshToken);

        // then
        verify(accessDAO, times(1)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            false,
            null
        );
    }

    @Test
    public void givenEnabledMls_whenConfirmingDeviceAndUpdateClientPublicKeyFails_thenThrowsErrorFromMls() {
        // given
        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledMlsFeatureConfigJsonResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(okJson(mlsPublicKeysSuccessResponse)));
        stubFor(post(urlEqualTo("/v6/access?client_id=" + clientId))
            .willReturn(okJson(accessResponse)));
        stubFor(put(urlEqualTo("/v6/clients/" + clientId))
            .willReturn(jsonResponse("{\"error\":\"error from clients/" + clientId + "\"}", 400)));

        // when
        try {
            deviceManagementService.confirmDevice(userId, clientId, refreshToken);
        } catch (Exception exception) {
            // then
            assert exception.getMessage().equals("{\"error\":\"error from clients/" + clientId + "\"}");
        }

        // then
        verify(accessDAO, times(0)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            true,
            DEFAULT_CIPHERSUITE_IDENTIFIER
        );
    }

    @Test
    public void givenEnabledMls_whenConfirmingDeviceAndKeyPackagesFails_thenThrowsErrorFromMls() {
        // given
        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledMlsFeatureConfigJsonResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(okJson(mlsPublicKeysSuccessResponse)));
        stubFor(put(urlEqualTo("/v6/clients/" + clientId))
            .willReturn(ok()));
        stubFor(post(urlEqualTo("/v6/mls/key-packages/self/" + clientId))
            .willReturn(jsonResponse("{\"error\":\"error from mls/key-packages/self/" + clientId + "\"}", 400)));

        // when
        try {
            deviceManagementService.confirmDevice(userId, clientId, refreshToken);
        } catch (Exception exception) {
            // then
            assert exception.getMessage().equals("ExecutionException: {\"error\":\"error from mls/key-packages/self/" + clientId + "\"}");
        }

        // then
        verify(accessDAO, times(0)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            true,
            DEFAULT_CIPHERSUITE_IDENTIFIER
        );
    }

    @Test
    public void givenEnabledMls_whenConfirmingDeviceAndConversationsIdsFails_thenEmptyConversationListIsReturned() {
        // given
        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledMlsFeatureConfigJsonResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(okJson(mlsPublicKeysSuccessResponse)));
        stubFor(put(urlEqualTo("/v6/clients/" + clientId))
            .willReturn(ok()));
        stubFor(post(urlEqualTo("/v6/mls/key-packages/self/" + clientId))
            .willReturn(created()));
        stubFor(post(urlEqualTo("/v6/conversations/list-ids"))
            .willReturn(jsonResponse("{\"error\":\"error from conversations/list-ids\"}", 400)));
        when(
            accessDAO.insert(
                userId.id,
                userId.domain,
                clientId,
                refreshToken,
                true,
                DEFAULT_CIPHERSUITE_IDENTIFIER
            )
        ).thenReturn(1);

        // when
        deviceManagementService.confirmDevice(userId, clientId, refreshToken);

        // then
        verify(accessDAO, times(1)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            true,
            DEFAULT_CIPHERSUITE_IDENTIFIER
        );
    }

    @Test
    public void givenEnabledMls_whenConfirmingDeviceAndConversationsListFails_thenEmptyConversationListIsReturned() {
        // given
        QualifiedId conversationId = new QualifiedId(UUID.randomUUID(), MetadataDAO.FALLBACK_DOMAIN_KEY);

        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledMlsFeatureConfigJsonResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(okJson(mlsPublicKeysSuccessResponse)));
        stubFor(put(urlEqualTo("/v6/clients/" + clientId))
            .willReturn(ok()));
        stubFor(post(urlEqualTo("/v6/mls/key-packages/self/" + clientId))
            .willReturn(created()));
        stubFor(post(urlEqualTo("/v6/conversations/list-ids"))
            .willReturn(okJson(getConversationsListIdsSuccessResponse(conversationId))));
        stubFor(post(urlEqualTo("/v6/conversations/list"))
            .willReturn(jsonResponse("{\"error\":\"error from conversations/list\"}", 400)));

        when(
            accessDAO.insert(
                userId.id,
                userId.domain,
                clientId,
                refreshToken,
                true,
                DEFAULT_CIPHERSUITE_IDENTIFIER
            )
        ).thenReturn(1);

        // when
        deviceManagementService.confirmDevice(userId, clientId, refreshToken);

        // then
        verify(accessDAO, times(1)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            true,
            DEFAULT_CIPHERSUITE_IDENTIFIER
        );
    }

    @Test
    public void givenEnabledMls_whenConfirmingDevice_thenEmptyConversationListIsReturned() {
        // given
        QualifiedId conversationId = new QualifiedId(UUID.randomUUID(), MetadataDAO.FALLBACK_DOMAIN_KEY);

        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledMlsFeatureConfigJsonResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(okJson(mlsPublicKeysSuccessResponse)));
        stubFor(put(urlEqualTo("/v6/clients/" + clientId))
            .willReturn(ok()));
        stubFor(post(urlEqualTo("/v6/mls/key-packages/self/" + clientId))
            .willReturn(created()));
        stubFor(post(urlEqualTo("/v6/conversations/list-ids"))
            .willReturn(okJson(getConversationsListIdsSuccessResponse(conversationId))));
        stubFor(post(urlEqualTo("/v6/conversations/list"))
            .willReturn(jsonResponse("{\"error\":\"error from conversations/list\"}", 400)));

        when(
            accessDAO.insert(
                userId.id,
                userId.domain,
                clientId,
                refreshToken,
                true,
                DEFAULT_CIPHERSUITE_IDENTIFIER
            )
        ).thenReturn(1);

        // when
        deviceManagementService.confirmDevice(userId, clientId, refreshToken);

        // then
        verify(accessDAO, times(1)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            true,
            DEFAULT_CIPHERSUITE_IDENTIFIER
        );
    }

    @Test
    public void givenEnabledMls_whenConfirmingDeviceAndGroupInfoFails_thenExceptionIsThrown() {
        // given
        QualifiedId conversationId = new QualifiedId(UUID.randomUUID(), MetadataDAO.FALLBACK_DOMAIN_KEY);
        String clientId = UUID.randomUUID().toString();

        stubFor(post(urlEqualTo("/v6/access?client_id=" + clientId))
            .willReturn(okJson(accessResponse)));
        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledMlsFeatureConfigJsonResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(okJson(mlsPublicKeysSuccessResponse)));
        stubFor(put(urlEqualTo("/v6/clients/" + clientId))
            .willReturn(ok()));
        stubFor(post(urlEqualTo("/v6/mls/key-packages/self/" + clientId))
            .willReturn(created()));
        stubFor(post(urlEqualTo("/v6/conversations/list-ids"))
            .willReturn(okJson(getConversationsListIdsSuccessResponse(conversationId))));
        stubFor(post(urlEqualTo("/v6/conversations/list"))
            .willReturn(okJson(getConversationsListSuccessResponse(conversationId))));
        stubFor(get(urlEqualTo("/v6/conversations/" + conversationId.domain + "/" + conversationId.id.toString() + "/groupinfo"))
            .willReturn(jsonResponse("{\"error\":\"error from conversations/" + conversationId.domain + "/" + conversationId.id.toString() + "/groupinfo\"}", 400)));

        // when
        try {
            deviceManagementService.confirmDevice(userId, clientId, refreshToken);
        } catch (Exception exception) {
            // then
            assert exception.getMessage().equals("{\"error\":\"error from conversations/" + conversationId.domain + "/" + conversationId.id + "/groupinfo\"}");
        }
    }

    @Test
    public void givenEnabledMls_whenConfirmingDeviceAndCommitMlsBundleFails_thenExceptionIsThrown() throws IOException {
        // given
        QualifiedId conversationId = new QualifiedId(UUID.randomUUID(), MetadataDAO.FALLBACK_DOMAIN_KEY);
        String clientId = UUID.randomUUID().toString();

        stubFor(post(urlEqualTo("/v6/access?client_id=" + clientId))
            .willReturn(okJson(accessResponse)));
        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledMlsFeatureConfigJsonResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(okJson(mlsPublicKeysSuccessResponse)));
        stubFor(put(urlEqualTo("/v6/clients/" + clientId))
            .willReturn(ok()));
        stubFor(post(urlEqualTo("/v6/mls/key-packages/self/" + clientId))
            .willReturn(created()));
        stubFor(post(urlEqualTo("/v6/conversations/list-ids"))
            .willReturn(okJson(getConversationsListIdsSuccessResponse(conversationId))));
        stubFor(post(urlEqualTo("/v6/conversations/list"))
            .willReturn(okJson(getConversationsListSuccessResponse(conversationId))));
        // GroupInfo of a real conversation, stored in a binary test file
        try (InputStream inputStream = new FileInputStream("src/test/resources/dummy_mls_conversation_groupinfo.bin")) {
            byte[] groupInfo = inputStream.readAllBytes();
            stubFor(get(urlEqualTo("/v6/conversations/" + conversationId.domain + "/" + conversationId.id.toString() + "/groupinfo"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(groupInfo)));
        }
        stubFor(post(urlEqualTo("/v6/mls/commit-bundles"))
            .willReturn(jsonResponse("{\"error\":\"error from mls/commit-bundles\"}", 400)));

        // when
        try {
            deviceManagementService.confirmDevice(userId, clientId, refreshToken);
        } catch (Exception exception) {
            // then
            assert exception.getMessage().equals("{\"error\":\"error from mls/commit-bundles\"}");
        }
    }

    @Test
    public void givenEnabledMls_whenConfirmingDevice_thenDeviceIsSavedToDatabase() throws IOException {
        // given
        QualifiedId conversationId = new QualifiedId(UUID.randomUUID(), MetadataDAO.FALLBACK_DOMAIN_KEY);

        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledMlsFeatureConfigJsonResponse)));
        stubFor(post(urlEqualTo("/v6/access?client_id=" + clientId))
            .willReturn(okJson(accessResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(okJson(mlsPublicKeysSuccessResponse)));
        stubFor(put(urlEqualTo("/v6/clients/" + clientId))
            .willReturn(ok()));
        stubFor(post(urlEqualTo("/v6/mls/key-packages/self/" + clientId))
            .willReturn(created()));
        stubFor(post(urlEqualTo("/v6/conversations/list-ids"))
            .willReturn(okJson(getConversationsListIdsSuccessResponse(conversationId))));
        stubFor(post(urlEqualTo("/v6/conversations/list"))
            .willReturn(okJson(getConversationsListSuccessResponse(conversationId))));
        // GroupInfo of a real conversation, stored in a binary test file
        try (InputStream inputStream = new FileInputStream("src/test/resources/dummy_mls_conversation_groupinfo.bin")) {
            byte[] groupInfo = inputStream.readAllBytes();
            stubFor(get(urlEqualTo("/v6/conversations/" + conversationId.domain + "/" + conversationId.id.toString() + "/groupinfo"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(groupInfo)));
        }
        stubFor(post(urlEqualTo("/v6/mls/commit-bundles"))
            .willReturn(ok()));

        when(
            accessDAO.insert(
                userId.id,
                userId.domain,
                clientId,
                refreshToken,
                true,
                DEFAULT_CIPHERSUITE_IDENTIFIER
            )
        ).thenReturn(1);

        // when
        deviceManagementService.confirmDevice(userId, clientId, refreshToken);

        // then
        verify(accessDAO, times(1)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            true,
            DEFAULT_CIPHERSUITE_IDENTIFIER
        );
    }

    @Test
    public void givenEnabledMls_whenConfirmingDevice_thenCorrectCipherSuiteIsSet() throws IOException {
        // given
        QualifiedId conversationId = new QualifiedId(UUID.randomUUID(), MetadataDAO.FALLBACK_DOMAIN_KEY);

        stubFor(get(urlEqualTo("/v6/feature-configs"))
            .willReturn(okJson(enabledCustomMlsFeatureConfigJsonResponse)));
        stubFor(post(urlEqualTo("/v6/access?client_id=" + clientId))
            .willReturn(okJson(accessResponse)));
        stubFor(get(urlEqualTo("/v6/mls/public-keys"))
            .willReturn(okJson(mlsPublicKeysSuccessResponse)));
        stubFor(put(urlEqualTo("/v6/clients/" + clientId))
            .willReturn(ok()));
        stubFor(post(urlEqualTo("/v6/mls/key-packages/self/" + clientId))
            .willReturn(created()));
        stubFor(post(urlEqualTo("/v6/conversations/list-ids"))
            .willReturn(okJson(getConversationsListIdsSuccessResponse(conversationId))));
        stubFor(post(urlEqualTo("/v6/conversations/list"))
            .willReturn(okJson(getConversationsListSuccessResponse(conversationId))));
        // GroupInfo of a real conversation, stored in a binary test file
        try (InputStream inputStream = new FileInputStream("src/test/resources/dummy_mls_conversation_groupinfo.bin")) {
            byte[] groupInfo = inputStream.readAllBytes();
            stubFor(get(urlEqualTo("/v6/conversations/" + conversationId.domain + "/" + conversationId.id.toString() + "/groupinfo"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(groupInfo)));
        }
        stubFor(post(urlEqualTo("/v6/mls/commit-bundles"))
            .willReturn(ok()));

        when(
            accessDAO.insert(
                userId.id,
                userId.domain,
                clientId,
                refreshToken,
                true,
                CUSTOM_CIPHERSUITE_IDENTIFIER
            )
        ).thenReturn(1);

        // when
        deviceManagementService.confirmDevice(userId, clientId, refreshToken);

        // then
        verify(accessDAO, times(1)).insert(
            userId.id,
            userId.domain,
            clientId,
            refreshToken,
            true,
            CUSTOM_CIPHERSUITE_IDENTIFIER
        );
    }

    @Test
    public void givenUnknownUser_whenRemovingDevice_thenNoMlsCallsAreMade() throws IOException, CryptoException {
        // given
        when(accessDAO.get(userId.id, userId.domain)).thenReturn(null);

        // when
        deviceManagementService.removeDevice(userId);

        // then
        verify(accessDAO, times(1)).get(userId.id, userId.domain);
    }

    @Test
    public void givenKnownUser_whenRemovingDeviceAndMlsIsDisabled_thenWipeIsCalled() throws IOException, CryptoException {
        // given
        Path path = Paths.get("mls/" + clientId);
        try (CryptoMlsClient cryptoMlsClient = new CryptoMlsClient(clientId, userId, DEFAULT_CIPHERSUITE_IDENTIFIER, coreCryptoPassword)) {
            assert cryptoMlsClient != null;
            assert Files.exists(path);
        }

        LHAccess lhAccess = new LHAccess();
        lhAccess.last = UUID.randomUUID();
        lhAccess.userId = new QualifiedId(userId.id, userId.domain);
        lhAccess.clientId = clientId;
        lhAccess.token = refreshToken;
        lhAccess.cookie = "cookie";
        lhAccess.mlsClientCreated = true;
        lhAccess.mlsCiphersuite = DEFAULT_CIPHERSUITE_IDENTIFIER;
        lhAccess.enabled = true;

        when(accessDAO.get(userId.id, userId.domain)).thenReturn(lhAccess);

        // when
        deviceManagementService.removeDevice(userId);

        // then
        verify(accessDAO, times(1)).get(userId.id, userId.domain);
        assert Files.notExists(path);
    }

    @Test
    public void givenKnownUser_whenRemovingDeviceAndMlsWasNotCreated_thenNoMlsFilesExist() throws IOException, CryptoException {
        String clientId = UUID.randomUUID().toString();
        Path path = Paths.get("mls/" + clientId);
        assert Files.notExists(path);

        LHAccess lhAccess = new LHAccess();
        lhAccess.last = UUID.randomUUID();
        lhAccess.userId = new QualifiedId(userId.id, userId.domain);
        lhAccess.clientId = clientId;
        lhAccess.token = refreshToken;
        lhAccess.cookie = "cookie";
        lhAccess.mlsClientCreated = false;
        lhAccess.mlsCiphersuite = null;
        lhAccess.enabled = true;

        when(accessDAO.get(userId.id, userId.domain)).thenReturn(lhAccess);

        // when
        deviceManagementService.removeDevice(userId);

        // then
        verify(accessDAO, times(1)).get(userId.id, userId.domain);
        assert Files.notExists(path);
    }

    Crypto mockedCrypto = new Crypto() {
        @Override
        public byte[] getIdentity() throws CryptoException {
            return new byte[0];
        }

        @Override
        public byte[] getLocalFingerprint() throws CryptoException {
            return new byte[0];
        }

        @Override
        public PreKey newLastPreKey() throws CryptoException {
            return null;
        }

        @Override
        public ArrayList<PreKey> newPreKeys(int from, int count) throws CryptoException {
            return null;
        }

        @Override
        public Recipients encrypt(PreKeys preKeys, byte[] content) throws CryptoException {
            return null;
        }

        @Override
        public Recipients encrypt(Missing missing, byte[] content) throws CryptoException {
            return null;
        }

        @Override
        public String decrypt(QualifiedId userId, String clientId, String cypher) throws CryptoException {
            return "";
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void purge() throws IOException {

        }

        @Override
        public void close() throws IOException {

        }
    };

    private static final String enabledMlsFeatureConfigJsonResponse = """
        {
            "mls": {
                "config": {
                  "allowedCipherSuites": [1],
                  "defaultCipherSuite": 1,
                  "defaultProtocol": "proteus",
                  "protocolToggleUsers": [],
                  "supportedProtocols": ["proteus"]
                },
                "lockStatus": "locked",
                "status": "enabled",
                "ttl": "unlimited"
            }
        }
    """;

    private static final String enabledCustomMlsFeatureConfigJsonResponse = """
        {
            "mls": {
                "config": {
                  "allowedCipherSuites": [7],
                  "defaultCipherSuite": 7,
                  "defaultProtocol": "mls",
                  "protocolToggleUsers": [],
                  "supportedProtocols": ["mls"]
                },
                "lockStatus": "locked",
                "status": "enabled",
                "ttl": "unlimited"
            }
        }
    """;

    private static final String disabledMlsFeatureConfigJsonResponse = """
        {
            "mls": {
                "config": {
                  "allowedCipherSuites": [65535],
                  "defaultCipherSuite": 65535,
                  "defaultProtocol": "proteus",
                  "protocolToggleUsers": [],
                  "supportedProtocols": ["proteus"]
                },
                "lockStatus": "locked",
                "status": "disabled",
                "ttl": "unlimited"
            }
        }
    """;

    private static final String apiVersionV6 = """
        {
          "development": [7],
          "domain": "example.com",
          "federation": false,
          "supported": [6]
        }
    """;

    private static final String accessResponse = """
        {
          "access_token": "string",
          "expires_in": 0,
          "token_type": "Bearer",
          "user": "99db9768-04e3-4b5d-9268-831b6a25c4ab"
        }
    """;

    private static final String mlsPublicKeysSuccessResponse = """
        {
          "removal": {
            "ecdsa_secp256r1_sha256": "sha256",
            "ecdsa_secp384r1_sha384": "sha384",
            "ecdsa_secp521r1_sha512": "sha512",
            "ed25519": "ed25519"
          }
        }
    """;

    private static String getConversationsListIdsSuccessResponse(QualifiedId convId) {
        return "{\n" +
            "  \"has_more\": false,\n" +
            "  \"paging_state\": \"string\",\n" +
            "  \"qualified_conversations\": [\n" +
            "    {\n" +
            "      \"domain\": \"" + convId.domain + "\",\n" +
            "      \"id\": \"" + convId.id + "\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }

    private static String getConversationsListSuccessResponse(QualifiedId convId) {
        return "{\n" +
            "          \"failed\": [\n" +
            "            {\n" +
            "              \"domain\": \"example.com\",\n" +
            "              \"id\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\"\n" +
            "            }\n" +
            "          ],\n" +
            "          \"found\": [\n" +
            "            {\n" +
            "              \"access\": [\n" +
            "                \"private\"\n" +
            "              ],\n" +
            "              \"access_role\": [\n" +
            "                \"team_member\"\n" +
            "              ],\n" +
            "              \"cipher_suite\": 1,\n" +
            "              \"creator\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\",\n" +
            "              \"epoch\": 18446744073709552000,\n" +
            "              \"epoch_timestamp\": \"2021-05-12T10:52:02Z\",\n" +
            "              \"group_id\": \"AAEAAAoESamc9ECFvXeFq1oj8HUAYW50YS53aXJlLmxpbms=\",\n" +
            "              \"id\": \"" + convId.id + "\",\n" +
            "              \"last_event\": \"string\",\n" +
            "              \"last_event_time\": \"string\",\n" +
            "              \"members\": {\n" +
            "                \"others\": [\n" +
            "                  {\n" +
            "                    \"conversation_role\": \"string\",\n" +
            "                    \"id\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\",\n" +
            "                    \"qualified_id\": {\n" +
            "                      \"domain\": \"example.com\",\n" +
            "                      \"id\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\"\n" +
            "                    },\n" +
            "                    \"service\": {\n" +
            "                      \"id\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\",\n" +
            "                      \"provider\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\"\n" +
            "                    }\n" +
            "                  }\n" +
            "                ],\n" +
            "                \"self\": {\n" +
            "                  \"conversation_role\": \"string\",\n" +
            "                  \"hidden\": true,\n" +
            "                  \"hidden_ref\": \"string\",\n" +
            "                  \"id\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\",\n" +
            "                  \"otr_archived\": true,\n" +
            "                  \"otr_archived_ref\": \"string\",\n" +
            "                  \"otr_muted_ref\": \"string\",\n" +
            "                  \"otr_muted_status\": 2147483647,\n" +
            "                  \"qualified_id\": {\n" +
            "                    \"domain\": \"example.com\",\n" +
            "                    \"id\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\"\n" +
            "                  },\n" +
            "                  \"service\": {\n" +
            "                    \"id\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\",\n" +
            "                    \"provider\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\"\n" +
            "                  },\n" +
            "                  \"status\": \"string\",\n" +
            "                  \"status_ref\": \"string\",\n" +
            "                  \"status_time\": \"string\"\n" +
            "                }\n" +
            "              },\n" +
            "              \"message_timer\": 9223372036854776000,\n" +
            "              \"name\": \"string\",\n" +
            "              \"protocol\": \"mls\",\n" +
            "              \"qualified_id\": {\n" +
            "                \"domain\": \"" + convId.domain + "\",\n" +
            "                \"id\": \"" + convId.id + "\"\n" +
            "              },\n" +
            "              \"receipt_mode\": 2147483647,\n" +
            "              \"team\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\",\n" +
            "              \"type\": 0\n" +
            "            }\n" +
            "          ],\n" +
            "          \"not_found\": [\n" +
            "            {\n" +
            "              \"domain\": \"example.com\",\n" +
            "              \"id\": \"99db9768-04e3-4b5d-9268-831b6a25c4ab\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }";
    }
}
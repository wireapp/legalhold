package com.wire.bots.hold;

import com.wire.bots.hold.model.api.shared.InitResponse;
import com.wire.bots.hold.model.api.v1.InitPayloadV1;
import com.wire.bots.hold.utils.HttpTestUtils;
import com.wire.xenon.backend.models.QualifiedId;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class InitiateResourceV1Test {
    private static final String TOKEN = "dummy";
    private static final DropwizardTestSupport<Config> SUPPORT = new DropwizardTestSupport<>(
        Service.class, "hold.yaml",
        ConfigOverride.config("token", TOKEN));
    private static Client client;

    @BeforeClass
    public static void init() throws Exception {
        SUPPORT.before();
        client = HttpTestUtils.createHttpClient(SUPPORT.getConfiguration(), SUPPORT.getEnvironment());
    }

    @AfterClass
    public static void afterClass() {
        client.close();
        SUPPORT.after();
    }

    @Test
    public void givenApiV1Body_ReturnKeys() {
        final QualifiedId userId = new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString());
        final UUID teamId = UUID.randomUUID();

        final InitResponse response = postInitiationV1(userId, teamId, TOKEN)
            .readEntity(InitResponse.class);

        assert response != null;
        assert response.lastPreKey != null;
        assert !response.preKeys.isEmpty();
        assert response.fingerprint != null;
    }

    @Test
    public void givenWrongApiV1Body_Return4xx() {
        final QualifiedId userId = null;
        final UUID teamId = UUID.randomUUID();

        final Response response = postInitiationV1(userId, teamId, TOKEN);

        assert response.getStatus() >= 400;
    }

    @Test
    public void givenWrongToken_ReturnUnauthorized() {
        final QualifiedId userId = new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString());
        final UUID teamId = UUID.randomUUID();

        final Response response = postInitiationV1(userId, teamId, "wrong_token");
        assert response.getStatus() == HttpStatus.SC_UNAUTHORIZED;
    }

    private Response postInitiationV1(QualifiedId userId, UUID teamId, String token){
        InitPayloadV1 initPayloadV1 = new InitPayloadV1();
        initPayloadV1.userId = userId;
        initPayloadV1.teamId = teamId;

        return client.target("http://localhost:" + SUPPORT.getLocalPort())
            .path("v1")
            .path("initiate")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(initPayloadV1, MediaType.APPLICATION_JSON));
    }
}

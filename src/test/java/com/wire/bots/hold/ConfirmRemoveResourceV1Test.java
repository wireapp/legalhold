package com.wire.bots.hold;

import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.api.v1.ConfirmPayloadV1;
import com.wire.bots.hold.model.api.v1.InitPayloadV1;
import com.wire.bots.hold.model.database.LHAccess;
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

public class ConfirmRemoveResourceV1Test {
    private static final String TOKEN = "dummy";
    private static final DropwizardTestSupport<Config> SUPPORT = new DropwizardTestSupport<>(
        Service.class, "hold.yaml",
        ConfigOverride.config("token", TOKEN));
    private static Client client;
    private static AccessDAO accessDAO;

    @BeforeClass
    public static void init() throws Exception {
        SUPPORT.before();
        client = HttpTestUtils.createHttpClient(SUPPORT.getConfiguration(), SUPPORT.getEnvironment());

        Service app = SUPPORT.getApplication();
        accessDAO = app.getJdbi().onDemand(AccessDAO.class);
    }

    @AfterClass
    public static void afterClass() {
        client.close();
        SUPPORT.after();
    }

    @Test
    public void givenConfirmAndRemove_ReturnOk() {
        final QualifiedId userId = new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString());
        final UUID teamId = UUID.randomUUID();
        final String refreshToken = UUID.randomUUID().toString();
        final String clientId = UUID.randomUUID().toString();

        ConfirmPayloadV1 confirmPayloadV1 = new ConfirmPayloadV1();
        confirmPayloadV1.userId = userId;
        confirmPayloadV1.teamId = teamId;
        confirmPayloadV1.refreshToken = refreshToken;
        confirmPayloadV1.clientId = clientId;

        final Response confirmResponse = postConfirmV1(confirmPayloadV1);
        assert confirmResponse.getStatus() == HttpStatus.SC_OK;

        InitPayloadV1 initPayloadV1 = new InitPayloadV1();
        initPayloadV1.userId = userId;
        initPayloadV1.teamId = teamId;

        final Response removeResponse = postRemoveV1(initPayloadV1);
        assert removeResponse.getStatus() == HttpStatus.SC_OK;
    }

    @Test
    public void givenRepeatedConfirm_ReturnOk() {
        final QualifiedId userId = new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString());
        final UUID teamId = UUID.randomUUID();
        final String refreshToken = UUID.randomUUID().toString();
        final String clientId = UUID.randomUUID().toString();

        ConfirmPayloadV1 confirmPayloadV1 = new ConfirmPayloadV1();
        confirmPayloadV1.userId = userId;
        confirmPayloadV1.teamId = teamId;
        confirmPayloadV1.refreshToken = refreshToken;
        confirmPayloadV1.clientId = clientId;

        final Response confirmResponse = postConfirmV1(confirmPayloadV1);
        assert confirmResponse.getStatus() == HttpStatus.SC_OK;

        final String updatedRefreshToken = UUID.randomUUID().toString();
        confirmPayloadV1.refreshToken = updatedRefreshToken;

        final Response confirmSecondResponse = postConfirmV1(confirmPayloadV1);
        assert confirmSecondResponse.getStatus() == HttpStatus.SC_OK;

        // Confirm the same device is possible and the latest refresh_token should be persisted
        final LHAccess lhAccess = accessDAO.get(userId.id);
        assert lhAccess != null;
        assert lhAccess.cookie.equals(updatedRefreshToken);
        assert !lhAccess.created.equals(lhAccess.updated);
    }

    @Test
    public void givenRemoveUnknownUser_ReturnOk() {
        // Remove device is idempotent on confirmed devices and also does not report errors trying to remove unknown users
        final QualifiedId userId = new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString());
        final UUID teamId = UUID.randomUUID();

        InitPayloadV1 initPayloadV1 = new InitPayloadV1();
        initPayloadV1.userId = userId;
        initPayloadV1.teamId = teamId;

        final Response removeResponse = postRemoveV1(initPayloadV1);
        assert removeResponse.getStatus() == HttpStatus.SC_OK;
    }

    private Response postConfirmV1(ConfirmPayloadV1 confirmPayloadV1){
        return client.target("http://localhost:" + SUPPORT.getLocalPort())
            .path("v1")
            .path("confirm")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, TOKEN)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(confirmPayloadV1, MediaType.APPLICATION_JSON));
    }

    private Response postRemoveV1(InitPayloadV1 initPayloadV1){
        return client.target("http://localhost:" + SUPPORT.getLocalPort())
            .path("v1")
            .path("remove")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, TOKEN)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(initPayloadV1, MediaType.APPLICATION_JSON));
    }
}

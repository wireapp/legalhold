package com.wire.bots.hold;

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

public class RemoveResourceV1Test {
    private static final String TOKEN = "dummy";
    private static final DropwizardTestSupport<Config> SUPPORT = new DropwizardTestSupport<>(
        Service.class, "hold.yaml",
        ConfigOverride.config("token", TOKEN),
        ConfigOverride.config("apiHost", "dummy"));
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

    private Response postRemoveV1(InitPayloadV1 initPayloadV1) {
        return client.target("http://localhost:" + SUPPORT.getLocalPort())
            .path("v1")
            .path("remove")
            .request(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, TOKEN)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(initPayloadV1, MediaType.APPLICATION_JSON));
    }
}

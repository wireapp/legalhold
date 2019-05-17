package com.wire.bots.hold;

import com.wire.bots.hold.model.Config;
import com.wire.bots.hold.model.InitPayload;
import com.wire.bots.hold.resource.InitiateResource;
import com.wire.bots.sdk.crypto.CryptoFile;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.tools.AuthValidator;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class InitiateResourceTest {
    @ClassRule
    public static final DropwizardAppRule<Config> app = new DropwizardAppRule<>(Service.class, "hold.yaml");

    private static AuthValidator authValidator = new AuthValidator("secret");

    @ClassRule
    public static final ResourceTestRule resources = ResourceTestRule.builder()
            .addResource(new InitiateResource(fileCryptoFactory(), authValidator))
            .build();

    private static CryptoFactory fileCryptoFactory() {
        return (botId) -> new CryptoFile("data", botId);
    }

    @Test
    public void test() {
        InitPayload init = new InitPayload();
        init.userId = UUID.randomUUID();
        init.teamId = UUID.randomUUID();
        Response response = resources.target("/initiate")
                .request()
                .header("Authorization", "secret")
                .post(Entity.entity(init, MediaType.APPLICATION_JSON));

        assert response.getStatus() == 200;
    }
}

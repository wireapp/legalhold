package com.wire.bots.hold;

import com.wire.bots.hold.model.Config;
import com.wire.bots.hold.model.ConfirmPayload;
import com.wire.bots.hold.resource.ConfirmResource;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.resources.StatusResource;
import com.wire.bots.sdk.state.FileState;
import com.wire.bots.sdk.tools.AuthValidator;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

public class ConfirmResourceTest {
    @ClassRule
    public static DropwizardAppRule<Config> app;
    @ClassRule
    public static ResourceTestRule resources;
    private static AuthValidator authValidator = new AuthValidator("secret");

    private static StorageFactory getStorageFactory() {
        return botId -> new FileState("data", botId);
    }

    @Before
    public void init() {
        app = new DropwizardAppRule<>(Service.class, "hold.yaml");
        resources = ResourceTestRule.builder()
                .addResource(new StatusResource())
                .build();
    }

    @Test
    public void test() {
        resources = ResourceTestRule.builder()
                .addResource(new ConfirmResource(new Database(app.getConfiguration().storage), getStorageFactory(), authValidator))
                .build();

        ConfirmPayload confirmPayload = new ConfirmPayload();
        confirmPayload.userId = UUID.randomUUID();
        confirmPayload.teamId = UUID.randomUUID();
        confirmPayload.clientId = UUID.randomUUID().toString();
        confirmPayload.accessToken = UUID.randomUUID().toString();
        confirmPayload.refreshToken = UUID.randomUUID().toString();
        Response response = resources.target("/confirm")
                .request()
                .header("Authorization", "secret")
                .post(Entity.entity(confirmPayload, MediaType.APPLICATION_JSON));

        assert response.getStatus() == 200;
    }
}

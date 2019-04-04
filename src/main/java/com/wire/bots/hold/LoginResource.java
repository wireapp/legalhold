package com.wire.bots.hold;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.bots.hold.model.Config;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.LoginClient;
import com.wire.bots.sdk.user.model.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.UUID;

@Api
@Path("/authorize")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoginResource {
    private final Client client;
    private final Database database;
    private final CryptoFactory cryptoFactory;
    private final Config config;

    LoginResource(Client client,
                  Database database,
                  CryptoFactory cryptoFactory,
                  Config config) {
        this.client = client;
        this.database = database;
        this.cryptoFactory = cryptoFactory;
        this.config = config;
    }

    @POST
    @ApiOperation(value = "Auth")
    public Response auth(@ApiParam("Email/Password") @Valid Payload payload) {
        try {
            LoginClient loginClient = new LoginClient(client);
            User login = loginClient.login(payload.email, payload.password, true);
            UUID botId = login.getUserId();
            String token = login.getToken();
            String cookie = login.getCookie();
            String clientId;

            // register new device
            try (Crypto crypto = cryptoFactory.create(botId.toString())) {
                ArrayList<PreKey> preKeys = crypto.newPreKeys(50, 100);
                PreKey lastKey = crypto.newLastPreKey();
                clientId = loginClient.registerClient(token, payload.password, preKeys, lastKey);
            }

            NewBot newBot = new NewBot();
            newBot.id = botId.toString();
            newBot.client = clientId;
            newBot.conversation = new Conversation();
            newBot.token = token;
            newBot.origin = new com.wire.bots.sdk.server.model.User();
            newBot.origin.handle = "legal";
            newBot.origin.id = botId.toString();

            Response response = client.target(config.baseUrl)
                    .path("bots")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, bearer(config.auth))
                    .post(Entity.entity(newBot, MediaType.APPLICATION_JSON_TYPE));

            if (response.getStatus() >= 400)
                return response;

            database.insertAccess(botId, clientId, token, cookie);

            Logger.info("Access: %s:%s token: %s cookie: %s",
                    botId,
                    clientId,
                    token,
                    cookie);

            return Response.
                    ok("Hooray", MediaType.TEXT_HTML_TYPE).
                    status(201).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("LoginResource.auth: %s", e);
            return Response
                    .ok(e)
                    .status(500)
                    .build();
        }
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        @NotNull
        @JsonProperty
        String email;

        @NotNull
        @JsonProperty
        String password;
    }
}
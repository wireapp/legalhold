package com.wire.bots.hold.resource;

import com.wire.bots.hold.Database;
import com.wire.bots.hold.model.Config;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.Conversation;
import com.wire.bots.sdk.server.model.ErrorMessage;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.LoginClient;
import com.wire.bots.sdk.user.model.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.FormParam;
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
@Path("/register")
@Produces(MediaType.APPLICATION_JSON)
public class RegisterDeviceResource {
    private static final String LEGALHOLD = "legalhold";
    private final Client client;
    private final Database database;
    private final CryptoFactory cryptoFactory;
    private final Config config;

    public RegisterDeviceResource(Client client,
                                  Database database,
                                  CryptoFactory cryptoFactory,
                                  Config config) {
        this.client = client;
        this.database = database;
        this.cryptoFactory = cryptoFactory;
        this.config = config;
    }

    private static String bearer(String token) {
        return String.format("Bearer %s", token);
    }

    private static String hexify(byte bytes[]) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 2) {
            buf.append((char) bytes[i]);
            buf.append((char) bytes[i + 1]);
            if (i == 30)
                buf.append("<br>");
            else
                buf.append(" ");
        }
        return buf.toString().trim();
    }

    private static String render(String clientId) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < clientId.length(); i += 2) {
            buf.append(clientId.charAt(i));
            buf.append(clientId.charAt(i + 1));
            buf.append(" ");
        }
        return buf.toString().trim();
    }

    @POST
    @ApiOperation(value = "(Obsolete) Register new Legal Device")
    public Response auth(@ApiParam @FormParam("email") String email,
                         @ApiParam @FormParam("password") String password) {
        try {
            LoginClient loginClient = new LoginClient(client);
            User login = loginClient.login(email, password, false);
            UUID botId = login.getUserId();
            String token = login.getToken();
            String cookie = login.getCookie();
            String clientId;
            byte[] fingerprint;

            // register new device
            try (Crypto crypto = cryptoFactory.create(botId.toString())) {
                ArrayList<PreKey> preKeys = crypto.newPreKeys(0, 50);
                PreKey lastKey = crypto.newLastPreKey();
                final String clazz = LEGALHOLD;
                final String type = LEGALHOLD;
                final String label = LEGALHOLD;
                clientId = loginClient.registerClient(token, password, preKeys, lastKey, clazz, type, label);
                fingerprint = crypto.getLocalFingerprint();
            }

            database.removeAccess(botId);
            database.insertAccess(botId, clientId, token, cookie);

            Logger.info("Access: %s:%s  email: %s",
                    botId,
                    clientId,
                    email);

            NewBot newBot = new NewBot();
            newBot.id = botId.toString();
            newBot.client = clientId;
            newBot.conversation = new Conversation();
            newBot.token = token;
            newBot.origin = new com.wire.bots.sdk.server.model.User();
            newBot.origin.handle = LEGALHOLD;
            newBot.origin.id = botId.toString();

            Response response = client.target(config.baseUrl)
                    .path("bots")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, bearer(config.auth))
                    .post(Entity.entity(newBot, MediaType.APPLICATION_JSON_TYPE));

            if (response.getStatus() >= 400)
                return response;

            String format = String.format("Legal Hold enabled for: %s <br><br>" +
                            "UserId: %s<br>" +
                            "Identity: %s<br><br>" +
                            "Key Fingerprint:<br>%s",
                    email,
                    botId,
                    render(clientId),
                    hexify(fingerprint));
            return Response.
                    ok(format, MediaType.TEXT_HTML_TYPE).
                    status(201).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("LoginResource.auth: %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
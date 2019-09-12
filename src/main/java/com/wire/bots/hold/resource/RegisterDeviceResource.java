package com.wire.bots.hold.resource;

import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.ErrorMessage;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.LoginClient;
import com.wire.bots.sdk.user.model.Access;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
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
    private final AccessDAO accessDAO;
    private final CryptoFactory cryptoFactory;

    public RegisterDeviceResource(Client client,
                                  AccessDAO accessDAO,
                                  CryptoFactory cryptoFactory) {
        this.client = client;
        this.accessDAO = accessDAO;
        this.cryptoFactory = cryptoFactory;
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

    @POST
    @ApiOperation(value = "(Obsolete) Register new Legal Device")
    public Response register(@ApiParam @FormParam("email") String email,
                             @ApiParam @FormParam("password") String password) {
        try {
            LoginClient loginClient = new LoginClient(client);
            Access access = loginClient.login(email, password, false);
            UUID botId = access.getUserId();
            String token = access.getToken();
            String cookie = access.getCookie().getValue();
            String clientId;
            byte[] fingerprint;

            // register new device
            try (Crypto crypto = cryptoFactory.create(botId)) {
                ArrayList<PreKey> preKeys = crypto.newPreKeys(0, 50);
                PreKey lastKey = crypto.newLastPreKey();
                final String clazz = LEGALHOLD;
                final String type = LEGALHOLD;
                final String label = LEGALHOLD;
                clientId = loginClient.registerClient(token, password, preKeys, lastKey, clazz, type, label);
                fingerprint = crypto.getLocalFingerprint();
            }

            accessDAO.disable(botId);
            accessDAO.insert(botId, clientId, cookie);

            Logger.info("RegisterDeviceResource.register: %s:%s  email: %s",
                    botId,
                    clientId,
                    email);

            String format = String.format("Legal Hold enabled for: %s <br><br>" +
                            "UserId:   %s<br><br>" +
                            "Identity: %s<br><br>" +
                            "Key Fingerprint:<br>%s",
                    email,
                    botId,
                    clientId,
                    hexify(fingerprint));
            return Response.
                    ok(format, MediaType.TEXT_HTML_TYPE).
                    status(201).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error("LoginResource.register: %s", e);
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}
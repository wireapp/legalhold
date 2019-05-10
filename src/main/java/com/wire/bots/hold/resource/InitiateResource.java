package com.wire.bots.hold.resource;

import com.wire.bots.hold.model.InitPayload;
import com.wire.bots.hold.model.InitResponse;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.ErrorMessage;
import com.wire.bots.sdk.tools.AuthValidator;
import com.wire.bots.sdk.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Api
@Path("/initiate")
@Produces(MediaType.APPLICATION_JSON)
public class InitiateResource {
    private final CryptoFactory cryptoFactory;
    private final AuthValidator validator;

    public InitiateResource(CryptoFactory cryptoFactory, AuthValidator validator) {

        this.cryptoFactory = cryptoFactory;
        this.validator = validator;
    }

    private static String hexify(byte bytes[]) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 2) {
            buf.append((char) bytes[i]);
            buf.append((char) bytes[i + 1]);
            buf.append(" ");
        }
        return buf.toString().trim();
    }

    @POST
    @ApiOperation(value = "Initiate")
    public Response initiate(@ApiParam @Valid InitPayload init,
                             @ApiParam @NotNull @HeaderParam("Authorization") String auth) {
        if (!validator.validate(auth)) {
            Logger.warning("Invalid auth '%s'", auth);
            return Response
                    .status(401)
                    .entity(new ErrorMessage("Invalid Authorization: " + auth))
                    .build();
        }

        try (Crypto crypto = cryptoFactory.create(init.userId.toString())) {
            ArrayList<PreKey> preKeys = crypto.newPreKeys(0, 50);
            PreKey lastKey = crypto.newLastPreKey();
            byte[] fingerprint = crypto.getLocalFingerprint();

            InitResponse response = new InitResponse();
            response.preKeys = preKeys;
            response.lastPreKey = lastKey;
            response.fingeprint = hexify(fingerprint);

            return Response.
                    ok(response).
                    build();
        } catch (Exception e) {
            Logger.error("InitiateResource.initiate: %s", e);
            return Response
                    .ok(e)
                    .status(500)
                    .build();
        }
    }
}
package com.wire.bots.hold.resource;

import com.wire.bots.hold.model.InitPayload;
import com.wire.bots.hold.model.InitResponse;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.model.ErrorMessage;
import com.wire.bots.sdk.tools.AuthValidator;
import com.wire.bots.sdk.tools.Logger;
import io.swagger.annotations.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

import static com.wire.bots.hold.utils.Tools.hexify;

@Api
@Path("/initiate")
@Produces(MediaType.APPLICATION_JSON)
public class InitiateResource {
    private final CryptoFactory cf;
    private final AuthValidator validator;

    public InitiateResource(CryptoFactory cf, AuthValidator validator) {

        this.cf = cf;
        this.validator = validator;
    }

    @POST
    @ApiOperation(value = "Initiate", response = InitResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Invalid Authorization"),
            @ApiResponse(code = 400, message = "Bad request. Invalid Payload or Authorization"),
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "CryptoBox initiated", response = InitResponse.class)})
    public Response initiate(@ApiParam @Valid InitPayload init,
                             @ApiParam @NotNull @HeaderParam("Authorization") String auth) {
        if (!validator.validate(auth)) {
            Logger.warning("Invalid auth '%s'", auth);
            return Response
                    .status(401)
                    .entity(new ErrorMessage("Invalid Authorization: " + auth))
                    .build();
        }

        try (Crypto crypto = cf.create(init.userId)) {
            ArrayList<PreKey> preKeys = crypto.newPreKeys(0, 50);
            PreKey lastKey = crypto.newLastPreKey();
            byte[] fingerprint = crypto.getLocalFingerprint();

            InitResponse response = new InitResponse();
            response.preKeys = preKeys;
            response.lastPreKey = lastKey;
            response.fingerprint = hexify(fingerprint);

            Logger.info("InitiateResource: team: %s, user: %s", init.teamId, init.userId);

            return Response.
                    ok(response).
                    build();
        } catch (Exception e) {
            Logger.error("InitiateResource: %s", e);
            return Response
                    .ok(e)
                    .status(500)
                    .build();
        }
    }
}
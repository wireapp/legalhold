package com.wire.bots.hold.resource;

import com.wire.bots.hold.model.InitPayload;
import com.wire.bots.hold.model.InitResponse;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;
import com.wire.xenon.models.otr.PreKey;
import com.wire.xenon.tools.Logger;
import io.swagger.annotations.*;

import javax.validation.Valid;
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

    public InitiateResource(CryptoFactory cf) {

        this.cf = cf;
    }

    @POST
    @Authorization("Bearer")
    @ApiOperation(value = "Initiate", response = InitResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad request. Invalid Payload"),
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "CryptoBox initiated")})
    public Response initiate(@ApiParam @Valid InitPayload init) {

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
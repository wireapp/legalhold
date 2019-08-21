package com.wire.bots.hold.resource;

import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.InitPayload;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
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

@Api
@Path("/remove")
@Produces(MediaType.APPLICATION_JSON)
public class RemoveResource {
    private final CryptoFactory cf;
    private final AuthValidator validator;
    private final AccessDAO accessDAO;

    public RemoveResource(AccessDAO accessDAO, CryptoFactory cf, AuthValidator validator) {
        this.accessDAO = accessDAO;
        this.cf = cf;
        this.validator = validator;
    }

    @POST
    @ApiOperation(value = "Remove legal hold device")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Invalid Authorization"),
            @ApiResponse(code = 400, message = "Bad request. Invalid Payload or Authorization"),
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Legal Hold Device was removed")})
    public Response remove(@ApiParam @Valid InitPayload payload,
                           @ApiParam @NotNull @HeaderParam("Authorization") String auth) {

        try {
            if (!validator.validate(auth)) {
                Logger.warning("Invalid auth '%s'", auth);
                return Response
                        .status(401)
                        .entity(new ErrorMessage("Invalid Authorization: " + auth))
                        .build();
            }

            try (Crypto crypto = cf.create(payload.userId)) {
                crypto.purge();
            }

            int removeAccess = accessDAO.remove(payload.userId);

            Logger.info("RemoveResource: team: %s, user: %s, removed: %s",
                    payload.teamId,
                    payload.userId,
                    removeAccess);

            return Response.
                    ok().
                    build();
        } catch (Exception e) {
            Logger.error("RemoveResource.remove: %s", e);
            return Response
                    .ok(e)
                    .status(500)
                    .build();
        }
    }
}
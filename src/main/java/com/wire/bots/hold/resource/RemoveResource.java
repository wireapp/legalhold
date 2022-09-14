package com.wire.bots.hold.resource;

import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.filters.ServiceAuthorization;
import com.wire.bots.hold.model.InitPayload;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.factories.CryptoFactory;
import com.wire.xenon.tools.Logger;
import io.swagger.annotations.*;

import javax.validation.Valid;
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
    private final AccessDAO accessDAO;

    public RemoveResource(AccessDAO accessDAO, CryptoFactory cf) {
        this.accessDAO = accessDAO;
        this.cf = cf;
    }

    @POST
    @ServiceAuthorization
    @ApiOperation(value = "Remove legal hold device")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad request. Invalid Payload"),
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Legal Hold Device was removed")})
    public Response remove(@ApiParam @Valid InitPayload payload) {
        try {
            try (Crypto crypto = cf.create(payload.userId)) {
                crypto.purge();
            }

            int removeAccess = accessDAO.disable(payload.userId);

            Logger.info("RemoveResource: team: %s, user: %s, removed: %s",
                    payload.teamId,
                    payload.userId,
                    removeAccess);

            return Response.
                    ok().
                    build();
        } catch (Exception e) {
            Logger.exception(e, "RemoveResource.remove: %s", e.getMessage());
            return Response
                    .ok(e)
                    .status(500)
                    .build();
        }
    }
}

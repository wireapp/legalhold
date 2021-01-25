package com.wire.bots.hold.resource;

import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.ConfirmPayload;
import com.wire.xenon.tools.Logger;
import io.swagger.annotations.*;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api
@Path("/confirm")
@Produces(MediaType.APPLICATION_JSON)
public class ConfirmResource {
    private final AccessDAO accessDAO;

    public ConfirmResource(AccessDAO accessDAO) {
        this.accessDAO = accessDAO;
    }

    @POST
    @Authorization("Bearer")
    @ApiOperation(value = "Confirm legal hold device")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad request. Invalid Payload"),
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Legal Hold Device enabled")})
    public Response confirm(@ApiParam @Valid ConfirmPayload payload) {
        try {
            int insert = accessDAO.insert(payload.userId,
                    payload.clientId,
                    payload.refreshToken);

            if (0 == insert) {
                Logger.error("ConfirmResource: Failed to insert Access %s:%s",
                        payload.userId,
                        payload.clientId);

                return Response.
                        serverError().
                        build();
            }

            Logger.info("ConfirmResource: team: %s, user:%s, client: %s",
                    payload.teamId,
                    payload.userId,
                    payload.clientId);

            return Response.
                    ok().
                    build();
        } catch (Exception e) {
            Logger.error("ConfirmResource.confirm: %s err: %s", payload.userId, e);
            e.printStackTrace();
            return Response
                    .ok(e)
                    .status(500)
                    .build();
        }
    }
}
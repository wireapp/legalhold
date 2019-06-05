package com.wire.bots.hold.resource;

import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.ConfirmPayload;
import com.wire.bots.sdk.tools.AuthValidator;
import com.wire.bots.sdk.tools.Logger;
import io.swagger.annotations.*;

import javax.validation.Valid;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;

@Api
@Path("/confirm")
@Produces(MediaType.APPLICATION_JSON)
public class ConfirmResource {
    private final AuthValidator validator;
    private final AccessDAO accessDAO;

    public ConfirmResource(AccessDAO accessDAO, AuthValidator validator) {
        this.accessDAO = accessDAO;
        this.validator = validator;
    }

    @POST
    @ApiOperation(value = "Confirm legal hold device")
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Invalid Authorization"),
            @ApiResponse(code = 400, message = "Bad request. Invalid Payload or Authorization"),
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Legal Hold Device enabled")})
    public Response confirm(@ApiParam @Valid ConfirmPayload confirmPayload,
                            @ApiParam @HeaderParam("Authorization") String auth) {

        try {
//            if (!validator.validate(auth)) {
//                Logger.warning("Invalid auth '%s'", auth);
//                return Response
//                        .status(401)
//                        .entity(new ErrorMessage("Invalid Authorization: " + auth))
//                        .build();
//            }

            int epochSecond = (int) Instant.now().getEpochSecond();

            int insert = accessDAO.insert(confirmPayload.userId,
                    confirmPayload.clientId,
                    confirmPayload.accessToken,
                    confirmPayload.refreshToken,
                    epochSecond);

            if (0 == insert) {
                Logger.error("ConfirmResource: Failed to insert Access %s:%s",
                        confirmPayload.userId,
                        confirmPayload.clientId);

                return Response.
                        serverError().
                        build();
            }

            Logger.info("ConfirmResource: %s:%s",
                    confirmPayload.userId,
                    confirmPayload.clientId);

            return Response.
                    ok().
                    build();
        } catch (Exception e) {
            Logger.error("ConfirmResource.confirm: %s", e);
            return Response
                    .ok(e)
                    .status(500)
                    .build();
        }
    }
}
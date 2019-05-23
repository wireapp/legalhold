package com.wire.bots.hold.resource;

import com.wire.bots.hold.Database;
import com.wire.bots.hold.model.ConfirmPayload;
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

@Api
@Path("/confirm")
@Produces(MediaType.APPLICATION_JSON)
public class ConfirmResource {
    private final Database database;
    private final AuthValidator validator;

    public ConfirmResource(Database database, AuthValidator validator) {
        this.database = database;
        this.validator = validator;
    }

    @POST
    @ApiOperation(value = "Confirm legal hold device")
    public Response confirm(@ApiParam @Valid ConfirmPayload confirmPayload,
                            @ApiParam @NotNull @HeaderParam("Authorization") String auth) {

        try {
            if (!validator.validate(auth)) {
                Logger.warning("Invalid auth '%s'", auth);
                return Response
                        .status(401)
                        .entity(new ErrorMessage("Invalid Authorization: " + auth))
                        .build();
            }

            database.removeAccess(confirmPayload.userId);
            database.insertAccess(confirmPayload.userId,
                    confirmPayload.clientId,
                    confirmPayload.accessToken,
                    confirmPayload.refreshToken);

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
package com.wire.bots.hold.resource.v0.backend;

import com.wire.bots.hold.filters.ServiceAuthorization;
import com.wire.bots.hold.model.api.v0.ConfirmPayloadV0;
import com.wire.bots.hold.service.DeviceManagementService;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.tools.Logger;
import io.swagger.annotations.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api
@Path("/confirm")
@Produces(MediaType.APPLICATION_JSON)
public class ConfirmResourceV0 {
    private final DeviceManagementService deviceManagementService;

    public ConfirmResourceV0(DeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }

    @POST
    @ServiceAuthorization
    @ApiOperation(value = "Confirm legal hold device")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Bad request. Invalid Payload"),
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Legal Hold Device enabled")})
    public Response confirm(@ApiParam @Valid @NotNull ConfirmPayloadV0 payload) {
        try {
            deviceManagementService.confirmDevice(
                new QualifiedId(payload.userId, null), //TODO Probably a good place to put the DEFAULT_DOMAIN
                payload.teamId,
                payload.clientId,
                payload.refreshToken
            );

            return Response
                .ok()
                .build();
        } catch (Exception e) {
            Logger.exception(e, "ConfirmResourceV0: %s err: %s", payload.userId, e.getMessage());
            return Response
                .ok(e)
                .status(500)
                .build();
        }
    }


}

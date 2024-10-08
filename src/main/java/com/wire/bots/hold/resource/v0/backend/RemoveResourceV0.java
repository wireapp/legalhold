package com.wire.bots.hold.resource.v0.backend;

import com.wire.bots.hold.filters.ServiceAuthorization;
import com.wire.bots.hold.model.api.v0.InitPayloadV0;
import com.wire.bots.hold.service.DeviceManagementService;
import com.wire.xenon.backend.models.QualifiedId;
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
public class RemoveResourceV0 {
    private final DeviceManagementService deviceManagementService;

    public RemoveResourceV0(DeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }

    @POST
    @ServiceAuthorization
    @ApiOperation(value = "Remove legal hold device")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Bad request. Invalid Payload"),
        @ApiResponse(code = 500, message = "Something went wrong"),
        @ApiResponse(code = 200, message = "Legal Hold Device was removed")})
    public Response remove(@ApiParam @Valid InitPayloadV0 payload) {
        try {
            deviceManagementService.removeDevice(
                new QualifiedId(payload.userId, null), //TODO Probably a good place to put the DEFAULT_DOMAIN
                payload.teamId
            );

            return Response.
                    ok().
                    build();
        } catch (Exception e) {
            Logger.exception(e, "RemoveResourceV0 error %s", e.getMessage());
            return Response
                    .ok(e)
                    .status(500)
                    .build();
        }
    }
}

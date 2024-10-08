package com.wire.bots.hold.resource.v0.backend;

import com.wire.bots.hold.filters.ServiceAuthorization;
import com.wire.bots.hold.model.api.shared.InitResponse;
import com.wire.bots.hold.model.api.v0.InitPayloadV0;
import com.wire.bots.hold.model.dto.InitializedDeviceDTO;
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
@Path("/initiate")
@Produces(MediaType.APPLICATION_JSON)
public class InitiateResourceV0 {
    private final DeviceManagementService deviceManagementService;

    public InitiateResourceV0(DeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }

    @POST
    @ServiceAuthorization
    @ApiOperation(value = "Initiate", response = InitResponse.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Bad request. Invalid Payload"),
        @ApiResponse(code = 500, message = "Something went wrong"),
        @ApiResponse(code = 200, message = "CryptoBox initiated")})
    public Response initiate(@ApiParam @Valid @NotNull InitPayloadV0 init) {
        try {
            final InitializedDeviceDTO initializedDeviceDTO =
                deviceManagementService.initiateLegalHoldDevice(
                    new QualifiedId(init.userId, null), //TODO Probably a good place to put the DEFAULT_DOMAIN
                    init.teamId
                );

            InitResponse response = new InitResponse();
            response.preKeys = initializedDeviceDTO.getPreKeys();
            response.lastPreKey = initializedDeviceDTO.getLastPreKey();
            response.fingerprint = initializedDeviceDTO.getFingerprint();
            return Response.
                    ok(response).
                    build();
        } catch (Exception e) {
            Logger.exception(e, "InitiateResourceV0 error: %s", e.getMessage());
            return Response
                    .ok(e)
                    .status(500)
                    .build();
        }
    }
}

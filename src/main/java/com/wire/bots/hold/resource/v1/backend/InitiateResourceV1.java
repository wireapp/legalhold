package com.wire.bots.hold.resource.v1.backend;

import com.wire.bots.hold.filters.ServiceAuthorization;
import com.wire.bots.hold.model.api.shared.InitResponse;
import com.wire.bots.hold.model.api.v1.InitPayloadV1;
import com.wire.bots.hold.model.dto.InitializedDeviceDTO;
import com.wire.bots.hold.service.DeviceManagementService;
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
@Path("/v1/initiate")
@Produces(MediaType.APPLICATION_JSON)
public class InitiateResourceV1 {
    private final DeviceManagementService deviceManagementService;

    public InitiateResourceV1(DeviceManagementService deviceManagementService) {
        this.deviceManagementService = deviceManagementService;
    }

    @POST
    @ServiceAuthorization
    @ApiOperation(value = "Initiate", response = InitResponse.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Bad request. Invalid Payload"),
        @ApiResponse(code = 500, message = "Something went wrong"),
        @ApiResponse(code = 200, message = "CryptoBox initiated")})
    public Response initiate(@ApiParam @Valid @NotNull InitPayloadV1 init) {
        try {
            final InitializedDeviceDTO initializedDeviceDTO =
                deviceManagementService.initiateLegalHoldDevice(
                    init.userId,
                    init.teamId
                );

            InitResponse response = new InitResponse();
            response.preKeys = initializedDeviceDTO.getPreKeys();
            response.lastPreKey = initializedDeviceDTO.getLastPreKey();
            response.fingerprint = initializedDeviceDTO.getFingerprint();
            return Response
                .ok(response)
                .build();
        } catch (Exception e) {
            Logger.exception(e, "InitiateResourceV1 error: %s", e.getMessage());
            return Response
                .serverError()
                .build();
        }
    }
}

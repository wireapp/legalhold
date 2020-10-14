package com.wire.bots.hold.resource;

import com.wire.bots.hold.Service;
import com.wire.bots.sdk.server.model.ErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.Objects;

@Api
@Path("/authorize")
public class AuthorizeResource {
    @POST
    @ApiOperation(value = "Authorize")
    public Response authenticate(@ApiParam @FormParam("token") String token) {
        try {
            final String auth = Service.instance.getConfig().auth;
            if (Objects.equals(auth, token)) {
                return Response.
                        status(Response.Status.SEE_OTHER).
                        header(HttpHeaders.LOCATION, "/index.html").
                        cookie(new NewCookie("W-Legal-Hold", "Bearer " + token)).
                        build();
            } else {
                return Response.
                        status(Response.Status.UNAUTHORIZED).
                        build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response
                    .ok(new ErrorMessage(e.getMessage()))
                    .status(500)
                    .build();
        }
    }
}

package com.wire.bots.hold.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/settings")
@Produces(MediaType.APPLICATION_JSON)
public class SettingsResource {
    @GET
    public Response settings() {
        //todo remove this endpoint if not needed
        return Response.
                ok().
                build();
    }
}

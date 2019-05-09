package com.wire.bots.hold.resource;

import com.wire.bots.hold.Service;
import com.wire.bots.hold.model.Config;

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
        Config config = Service.instance.getConfig();
        return Response.
                ok(config).
                build();
    }
}

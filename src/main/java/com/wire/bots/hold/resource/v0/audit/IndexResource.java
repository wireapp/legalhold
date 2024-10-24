package com.wire.bots.hold.resource.v0.audit;

import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.filters.ServiceAuthorization;
import com.wire.bots.hold.model.EventModel;
import com.wire.bots.hold.utils.HtmlGenerator;
import com.wire.xenon.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api
@Path("/index.html")
@Produces(MediaType.TEXT_HTML)
public class IndexResource {
    private final EventsDAO eventsDAO;

    public IndexResource(EventsDAO eventsDAO) {
        this.eventsDAO = eventsDAO;
    }

    @GET
    @ServiceAuthorization
    @ApiOperation(value = "List all Wire conversations")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Wire conversations")})
    public Response list() {
        try {
            EventModel model = new EventModel();
            model.events = eventsDAO.listConversations();
            String html = HtmlGenerator.execute(model, HtmlGenerator.TemplateType.INDEX);

            return Response.
                    ok(html, MediaType.TEXT_HTML).
                    build();
        } catch (Exception e) {
            Logger.exception("IndexResource.list: %s", e, e.getMessage());
            return Response
                    .ok(e.getMessage())
                    .status(500)
                    .build();
        }
    }
}

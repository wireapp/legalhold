package com.wire.bots.hold.resource.v0.audit;

import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.filters.ServiceAuthorization;
import com.wire.bots.hold.model.EventModel;
import com.wire.bots.hold.utils.Cache;
import com.wire.bots.hold.utils.HtmlGenerator;
import com.wire.xenon.tools.Logger;
import io.swagger.annotations.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Api
@Path("/events/{conversationId}{noop: (/)?}{conversationDomain:([^/]+?)?}")
@Produces(MediaType.TEXT_HTML)
public class EventsResource {
    private final EventsDAO eventsDAO;

    public EventsResource(EventsDAO eventsDAO) {
        this.eventsDAO = eventsDAO;
    }

    @GET
    @ServiceAuthorization
    @ApiOperation(value = "List all Wire events for this conversation")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Wire events")}
    )
    public Response list(
        @ApiParam @PathParam("conversationId") UUID conversationId,
        @ApiParam @PathParam("conversationDomain") String conversationDomain
    ) {
        boolean isValidDomain = conversationDomain != null && !conversationDomain.isEmpty();

        try {
            EventModel model = new EventModel();
            if (isValidDomain && !conversationDomain.equals(Cache.getFallbackDomain())) {
                model.events = eventsDAO.listAll(conversationId, conversationDomain);
            } else {
                model.events = eventsDAO.listAllDefaultDomain(conversationId, Cache.getFallbackDomain());
            }
            String html = HtmlGenerator.execute(model, HtmlGenerator.TemplateType.EVENTS);

            return Response.
                    ok(html, MediaType.TEXT_HTML).
                    build();
        } catch (Exception exception) {
            Logger.exception(exception, "EventsResource.list: %s", exception.getMessage());
            return Response
                    .ok(exception.getMessage())
                    .status(500)
                    .build();
        }
    }
}

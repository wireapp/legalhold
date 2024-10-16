package com.wire.bots.hold.resource.v0.audit;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.filters.ServiceAuthorization;
import com.wire.bots.hold.model.database.Event;
import com.wire.bots.hold.utils.Cache;
import com.wire.xenon.tools.Logger;
import io.swagger.annotations.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

@Api
@Path("/events/{conversationId}")
@Produces(MediaType.TEXT_HTML)
public class EventsResource {
    private final static MustacheFactory mf = new DefaultMustacheFactory();
    private final EventsDAO eventsDAO;

    public EventsResource(EventsDAO eventsDAO) {
        this.eventsDAO = eventsDAO;
    }

    @GET
    @ServiceAuthorization
    @ApiOperation(value = "List all Wire events for this conversation")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Wire events")})
    public Response list(@ApiParam @PathParam("conversationId") UUID conversationId) {
        try {
            // TODO: When the parameters of this resource changes to accept a QualifiedId, then we will need
            // to verify based on the domain which DAO query to call
            Model model = new Model();
            model.events = eventsDAO.listAllDefaultDomain(conversationId, Cache.getFallbackDomain());
            String html = execute(model);

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

    private Mustache compileTemplate() {
        String path = "templates/events.html";
        return mf.compile(path);
    }

    private String execute(Object model) throws IOException {
        Mustache mustache = compileTemplate();
        try (StringWriter sw = new StringWriter()) {
            mustache.execute(new PrintWriter(sw), model).flush();
            return sw.toString();
        }
    }

    static class Model {
        List<Event> events;
    }
}

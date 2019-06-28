package com.wire.bots.hold.resource;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.Access;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.tools.Logger;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.UUID;

import static com.wire.bots.hold.utils.Tools.hexify;

@Api
@Path("/list")
@Produces(MediaType.TEXT_HTML)
public class ListingResource {
    private final static MustacheFactory mf = new DefaultMustacheFactory();
    private final CryptoFactory cryptoFactory;
    private final AccessDAO accessDAO;

    public ListingResource(AccessDAO accessDAO, CryptoFactory cryptoFactory) {
        this.cryptoFactory = cryptoFactory;
        this.accessDAO = accessDAO;
    }

    @GET
    @ApiOperation(value = "List all legal hold devices")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Legal Hold Devices")})
    public Response list() {
        try {
            ArrayList<Legal> legals = new ArrayList<>();
            for (Access a : accessDAO.list(50)) {
                try (Crypto crypto = cryptoFactory.create(a.userId.toString())) {
                    byte[] fingerprint = crypto.getLocalFingerprint();
                    Legal legal = new Legal();
                    legal.userId = a.userId;
                    legal.clientId = a.clientId;
                    legal.fingerprint = hexify(fingerprint);
                    legal.last = a.last;
                    legal.updated = a.updated;
                    legal.created = a.created;

                    legals.add(legal);
                }
            }

            Model model = new Model();
            model.legals = legals;
            String html = execute(model);

            return Response.
                    ok(html, MediaType.TEXT_HTML).
                    build();
        } catch (Exception e) {
            Logger.error("ListingResource.list: %s", e);
            return Response
                    .ok(e.getMessage())
                    .status(500)
                    .build();
        }
    }

    private Mustache compileTemplate() {
        String path = "templates/devices.html";
        return mf.compile(path);
    }

    private String execute(Object model) throws IOException {
        Mustache mustache = compileTemplate();
        try (StringWriter sw = new StringWriter()) {
            mustache.execute(new PrintWriter(sw), model).flush();
            return sw.toString();
        }
    }

    class Legal {
        UUID last;
        UUID userId;
        String clientId;
        String fingerprint;
        String updated;
        String created;
    }

    class Model {
        ArrayList<Legal> legals;
    }
}
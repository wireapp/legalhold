package com.wire.bots.hold.resource.v0.audit;

import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.filters.ServiceAuthorization;
import com.wire.bots.hold.model.database.LHAccess;
import com.wire.bots.hold.utils.CryptoDatabaseFactory;
import com.wire.bots.hold.utils.HtmlGenerator;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.crypto.Crypto;
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
import java.util.ArrayList;
import java.util.UUID;

import static com.wire.bots.hold.utils.Tools.hexify;

@Api
@Path("/devices.html")
@Produces(MediaType.TEXT_HTML)
public class DevicesResource {
    private final CryptoDatabaseFactory cryptoFactory;
    private final AccessDAO accessDAO;

    public DevicesResource(AccessDAO accessDAO, CryptoDatabaseFactory cryptoFactory) {
        this.cryptoFactory = cryptoFactory;
        this.accessDAO = accessDAO;
    }

    @GET
    @ServiceAuthorization
    @ApiOperation(value = "List all legal hold devices")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Something went wrong"),
            @ApiResponse(code = 200, message = "Legal Hold Devices")})
    public Response list() {
        try {
            ArrayList<Legal> legals = new ArrayList<>();
            for (LHAccess a : accessDAO.list(50)) {
                try (Crypto crypto = cryptoFactory.create(a.userId)) {
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
            String html = HtmlGenerator.execute(model, HtmlGenerator.TemplateType.DEVICES);

            return Response.
                    ok(html, MediaType.TEXT_HTML).
                    build();
        } catch (Exception e) {
            Logger.exception("DevicesResource.list: %s", e, e.getMessage());
            return Response
                    .ok(e.getMessage())
                    .status(500)
                    .build();
        }
    }

    static class Legal {
        UUID last;
        QualifiedId userId;
        String clientId;
        String fingerprint;
        String updated;
        String created;
    }

    static class Model {
        ArrayList<Legal> legals;
    }
}

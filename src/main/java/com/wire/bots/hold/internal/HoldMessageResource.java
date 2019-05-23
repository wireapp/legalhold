package com.wire.bots.hold.internal;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.hold.utils.HoldClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.exceptions.MissingStateException;
import com.wire.bots.sdk.server.model.ErrorMessage;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.server.resources.MessageResourceBase;
import com.wire.bots.sdk.tools.Logger;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/{userId}/messages")
public class HoldMessageResource extends MessageResourceBase {
    private final HoldClientRepo repo;

    public HoldMessageResource(MessageHandlerBase handler, HoldClientRepo repo) {
        super(handler, null, repo);
        this.repo = repo;
    }

    @Override
    protected WireClient getWireClient(String userId, Payload payload) throws CryptoException {
        UUID uuid = UUID.fromString(userId);
        return repo.getClient(uuid, payload.data.recipient, payload.convId);
    }

    @POST
    public Response newMessage(@PathParam("userId") UUID userId,
                               @Valid @NotNull Payload payload) {

        try (WireClient client = getWireClient(userId.toString(), payload)) {
            handleMessage(payload, client);
        } catch (CryptoException e) {
            Logger.error("newMessage: %s %s", userId, e);
            return Response.
                    status(503).
                    entity(new ErrorMessage(e.toString())).
                    build();
        } catch (MissingStateException e) {
            Logger.error("newMessage: %s %s", userId, e);
            return Response.
                    status(410).
                    entity(new ErrorMessage(e.toString())).
                    build();
        } catch (Exception e) {
            Logger.error("newMessage: %s %s", userId, e);
            return Response.
                    status(400).
                    entity(new ErrorMessage(e.toString())).
                    build();
        }

        return Response.
                ok().
                status(200).
                build();
    }

    @Override
    protected boolean isValid(String auth) {
        return true;
    }
}

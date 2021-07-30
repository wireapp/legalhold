package com.wire.bots.hold;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.hold.utils.HoldClientRepo;
import com.wire.xenon.MessageHandlerBase;
import com.wire.xenon.MessageResourceBase;
import com.wire.xenon.WireClient;
import com.wire.xenon.backend.models.Payload;
import com.wire.xenon.exceptions.MissingStateException;
import com.wire.xenon.tools.Logger;

import java.util.UUID;

public class HoldMessageResource extends MessageResourceBase {
    private final HoldClientRepo repo;

    public HoldMessageResource(MessageHandlerBase handler, HoldClientRepo repo) {
        super(handler);
        this.repo = repo;
    }

    protected WireClient getWireClient(UUID userId, Payload payload) throws CryptoException {
        return repo.getClient(userId, payload.data.recipient, payload.convId);
    }

    public boolean onNewMessage(UUID userId, UUID id, Payload payload) {

        try (WireClient client = getWireClient(userId, payload)) {
            handleMessage(id, payload, client);
        } catch (CryptoException | MissingStateException e) {
            Logger.error("newMessage: %s %s", userId, e.getMessage());
            return false;
        } catch (Exception e) {
            Logger.exception("newMessage: %s %s", e, userId, e.getMessage());
            return false;
        }

        return true;
    }
}

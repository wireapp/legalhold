package com.wire.bots.hold.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.LHAccess;
import com.wire.helium.API;
import com.wire.xenon.tools.Logger;

import javax.ws.rs.client.Client;
import java.util.List;

public class SanityCheck extends HealthCheck {

    private final AccessDAO accessDAO;
    private final Client client;

    public SanityCheck(AccessDAO accessDAO, Client client) {
        this.accessDAO = accessDAO;
        this.client = client;
    }

    @Override
    protected Result check() {
        try {
            LHAccess single = accessDAO.getSingle();
            API api = new API(client, null, single.token);

            String created = single.created;
            List<LHAccess> accessList = accessDAO.list(100, created);

            while (!accessList.isEmpty()) {
                Logger.info("SanityCheck: checking %d devices, created: %s", accessList.size(), created);
                for (LHAccess access : accessList) {
                    boolean hasDevice = api.hasDevice(access.userId, access.clientId);

                    if (!access.enabled && hasDevice)
                        return Result.unhealthy("User %s is NOT tracked in LH", access.userId);

                    if (access.enabled && !hasDevice)
                        return Result.unhealthy("User %s IS tracked in LH", access.userId);

                    created = access.created;
                }

                accessList = accessDAO.list(100, created);
            }

            return Result.healthy();
        } catch (Exception e) {
            Logger.exception("SanityCheck failed.", e);
            return Result.unhealthy(e.getMessage());
        } finally {
            Logger.debug("Finished SanityCheck");
        }
    }
}

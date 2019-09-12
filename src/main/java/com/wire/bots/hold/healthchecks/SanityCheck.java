package com.wire.bots.hold.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.LHAccess;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.API;

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

            List<LHAccess> accessList = accessDAO.listAll();
            for (LHAccess access : accessList) {
                boolean hasDevice = api.hasDevice(access.userId, access.clientId);

                if (!access.enabled && hasDevice)
                    return Result.unhealthy("User %s is NOT tracked in LH", access.userId);

                if (access.enabled && !hasDevice)
                    return Result.unhealthy("User %s IS tracked in LH", access.userId);
            }

            return Result.healthy();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.unhealthy(e.getMessage());
        } finally {
            Logger.debug("Finished SanityCheck");
        }
    }
}
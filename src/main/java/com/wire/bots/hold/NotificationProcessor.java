package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.database.LHAccess;
import com.wire.bots.hold.service.DeviceManagementService;
import com.wire.bots.hold.utils.LoginClientExtension;
import com.wire.helium.API;
import com.wire.helium.models.Access;
import com.wire.helium.models.Event;
import com.wire.helium.models.NotificationList;
import com.wire.xenon.backend.models.Payload;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.exceptions.AuthException;
import com.wire.xenon.exceptions.HttpException;
import com.wire.xenon.tools.Logger;

import javax.ws.rs.client.Client;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class NotificationProcessor implements Runnable {
    private static final int DEFAULT_NOTIFICATION_SIZE = 100;

    private final Client client;
    private final AccessDAO accessDAO;
    private final HoldMessageResource messageResource;
    private final DeviceManagementService deviceManagementService;

    NotificationProcessor(
        Client client,
        AccessDAO accessDAO,
        HoldMessageResource messageResource,
        DeviceManagementService deviceManagementService)
    {
        this.client = client;
        this.accessDAO = accessDAO;
        this.messageResource = messageResource;
        this.deviceManagementService = deviceManagementService;
    }

    @Override
    public void run() {
        try {
            List<LHAccess> devices = accessDAO.listEnabled();
            Logger.info("Devices: %d", devices.size());

            for (LHAccess device : devices) {
                process(device);
            }
        } catch (Exception e) {
            Logger.exception(e, "NotificationProcessor: %s", e.getMessage());
        }
    }

    private void process(LHAccess device) {
        try {
            Logger.debug("`GET /notifications`: user: %s, last: %s", device.userId, device.last);

            final Access access = LoginClientExtension.refreshToken(client, device.clientId, device.cookie);
            accessDAO.update(device.userId.id, device.userId.domain, access.getAccessToken(), access.getCookie().value);
            final API api = new API(client, null, access.getAccessToken());

            if (!device.mlsClientCreated) {
                deviceManagementService.configureMlsClient(device.userId, device.clientId, access.getCookie().value, api);
            }

            NotificationList notificationList = api.retrieveNotifications(
                device.clientId,
                device.last,
                DEFAULT_NOTIFICATION_SIZE
            );

            process(device.userId, device.clientId, notificationList);
        } catch (AuthException e) {
            accessDAO.disable(device.userId.id, device.userId.domain);
            Logger.exception(e, "NotificationProcessor: Disabled LH device for user: %s, error: %s", device.userId, e.getMessage());
        } catch (HttpException e) {
            Logger.exception(e, "NotificationProcessor: Couldn't retrieve notifications, error: %s", e.getMessage());
        } catch (Exception e) {
            Logger.exception(e, "NotificationProcessor: user: %s, last: %s, error: %s", device.userId, device.last, e.getMessage());
        }
    }

    private void process(QualifiedId userId, String clientId, NotificationList notificationList) {
        for (Event event : notificationList.notifications) {
            for (Payload payload : event.payload) {
                if (process(userId, clientId, payload, event.id)) {
                    Logger.debug("Processed: `%s` conv: %s, user: %s, client:%s, eventId: %s",
                            payload.type,
                            payload.conversation,
                            userId,
                            clientId,
                            event.id);
                } else {
                    Logger.error("Failed to process: user: %s, client:%s, event: %s", userId, clientId, event.id);
                }
            }

            accessDAO.updateLast(userId.id, userId.domain, event.id);
        }
    }

    private boolean process(QualifiedId userId, String clientId, Payload payload, UUID eventId) {
        trace(payload);

        Logger.info("Payload: %s %s, from: %s", payload.type, userId, payload.from);

        if (payload.from == null || payload.data == null) return true;

        final boolean wasMessageSent = messageResource.onNewMessage(userId, clientId, eventId, payload);

        if (!wasMessageSent) {
            Logger.error("Failed to process: %s user: %s, from: %s:", payload.type, userId, payload.from);
        }

        return wasMessageSent;
    }

    private void trace(Payload payload) {
        if (Logger.getLevel() == Level.FINE) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Logger.debug(objectMapper.writeValueAsString(payload));
            } catch (JsonProcessingException e) {
                Logger.exception(e, "Exception during JSON parsing - %s.", e.getMessage());
            }
        }
    }
}

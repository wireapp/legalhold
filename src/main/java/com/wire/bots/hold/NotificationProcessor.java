package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.database.LHAccess;
import com.wire.helium.API;
import com.wire.helium.LoginClient;
import com.wire.helium.models.Access;
import com.wire.helium.models.Event;
import com.wire.helium.models.NotificationList;
import com.wire.xenon.backend.models.Payload;
import com.wire.xenon.exceptions.AuthException;
import com.wire.xenon.exceptions.HttpException;
import com.wire.xenon.tools.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Cookie;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class NotificationProcessor implements Runnable {
    private static final int DEFAULT_NOTIFICATION_SIZE = 100;

    private final Client client;
    private final AccessDAO accessDAO;
    private final HoldMessageResource messageResource;

    NotificationProcessor(Client client, AccessDAO accessDAO, HoldMessageResource messageResource) {
        this.client = client;
        this.accessDAO = accessDAO;
        this.messageResource = messageResource;
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

    private Access getAccess(Cookie cookie) throws HttpException {
        LoginClient loginClient = new LoginClient(client);
        return loginClient.renewAccessToken(cookie);
    }

    private void process(LHAccess device) {
        UUID userId = device.userId;

        try {
            final API api = new API(client, null, device.token);

            Logger.debug("`GET /notifications`: user: %s, last: %s", userId, device.last);

            String cookieValue = device.cookie;

            Cookie cookie = new Cookie("zuid", device.cookie);

            Access access = getAccess(cookie);

            if (access.getCookie() != null) {
                Logger.info("Set-Cookie: user: %s", userId);
                cookieValue = access.getCookie().value;
            }

            accessDAO.update(userId, access.getAccessToken(), cookieValue);

            device.token = access.getAccessToken();

            NotificationList notificationList = api.retrieveNotifications(
                device.clientId,
                device.last,
                DEFAULT_NOTIFICATION_SIZE
            );

            process(userId, notificationList);
        } catch (AuthException e) {
            accessDAO.disable(userId);
            Logger.info("Disabled LH device for user: %s, error: %s", userId, e.getMessage());
        } catch (HttpException e) {
            Logger.exception(e, "NotificationProcessor: Couldn't retrieve notifications, error: %s", e.getMessage());
        } catch (Exception e) {
            Logger.exception(e, "NotificationProcessor: user: %s, last: %s, error: %s", userId, device.last, e.getMessage());
        }
    }

    private void process(UUID userId, NotificationList notificationList) {
        for (Event event : notificationList.notifications) {
            for (Payload payload : event.payload) {
                if (!process(userId, payload, event.id)) {
                    Logger.error("Failed to process: user: %s, event: %s", userId, event.id);
                } else {
                    Logger.debug("Processed: `%s` conv: %s, user: %s, eventId: %s",
                            payload.type,
                            payload.conversation,
                            userId,
                            event.id);
                }
            }

            accessDAO.updateLast(userId, event.id);
        }
    }

    private boolean process(UUID userId, Payload payload, UUID id) {
        trace(payload);

        Logger.debug("Payload: %s %s, from: %s",
                payload.type,
                userId,
                payload.from);

        if (payload.from == null || payload.data == null) return true;

        final boolean wasMessageSent = messageResource.onNewMessage(userId, id, payload);

        if (!wasMessageSent) {
            Logger.error("process: `%s` user: %s, from: %s:%s, error: %s", payload.type, userId, payload.from, payload.data.sender);
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

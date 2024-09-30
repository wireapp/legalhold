package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.Config;
import com.wire.bots.hold.model.LHAccess;
import com.wire.bots.hold.model.Notification;
import com.wire.bots.hold.model.NotificationList;
import com.wire.helium.LoginClient;
import com.wire.helium.models.Access;
import com.wire.xenon.backend.models.Payload;
import com.wire.xenon.exceptions.AuthException;
import com.wire.xenon.exceptions.HttpException;
import com.wire.xenon.tools.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class NotificationProcessor implements Runnable {
    private final Client client;
    private final AccessDAO accessDAO;
    private final HoldMessageResource messageResource;
    private final WebTarget api;

    NotificationProcessor(Client client, AccessDAO accessDAO, Config config, HoldMessageResource messageResource) {
        this.client = client;
        this.accessDAO = accessDAO;
        this.messageResource = messageResource;

        api = client.target(config.apiHost);
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

            NotificationList notificationList = retrieveNotifications(device);

            process(userId, notificationList);

        } catch (AuthException e) {
            accessDAO.disable(userId);
            Logger.info("Disabled LH device for user: %s, error: %s", userId, e.getMessage());
        } catch (Exception e) {
            Logger.exception(e, "NotificationProcessor: user: %s, last: %s, error: %s", userId, device.last, e.getMessage());
        }
    }

    private static String bearer(String token) {
        return token == null ? null : String.format("Bearer %s", token);
    }

    private void process(UUID userId, NotificationList notificationList) {

        for (Notification notif : notificationList.notifications) {
            for (Payload payload : notif.payload) {
                if (!process(userId, payload, notif.id)) {
                    Logger.error("Failed to process: user: %s, notif: %s", userId, notif.id);
                    //return;
                } else {
                    Logger.debug("Processed: `%s` conv: %s, user: %s, notifId: %s",
                            payload.type,
                            payload.conversation,
                            userId,
                            notif.id);
                }
            }

            accessDAO.updateLast(userId, notif.id);
        }
    }

    private boolean process(UUID userId, Payload payload, UUID id) {
        trace(payload);

        Logger.debug("Payload: %s %s, from: %s",
                payload.type,
                userId,
                payload.from);

        if (payload.from == null || payload.data == null) return true;

        final boolean b = messageResource.onNewMessage(userId, id, payload);

        if (!b) {
            Logger.error("process: `%s` user: %s, from: %s:%s, error: %s", payload.type, userId, payload.from, payload.data.sender);
        }

        return b;
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

    //TODO remove this and use retrieveNotifications provided by Helium
    private NotificationList retrieveNotifications(LHAccess access) throws HttpException {
        Response response = api.path("notifications").queryParam("client", access.clientId).queryParam("since", access.last).queryParam("size", 100).request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, bearer(access.token)).get();

        int status = response.getStatus();

        if (status == 200) {
            return response.readEntity(NotificationList.class);
        }

        if (status == 404) {  //todo what???
            return response.readEntity(NotificationList.class);
        }

        if (status == 401) {   //todo nginx returns text/html for 401. Cannot deserialize as json
            response.readEntity(String.class);
            throw new AuthException(status);
        }

        if (status == 403) {
            throw response.readEntity(AuthException.class);
        }

        throw response.readEntity(HttpException.class);
    }
}

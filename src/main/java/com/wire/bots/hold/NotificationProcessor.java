package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.Notification;
import com.wire.bots.hold.model.NotificationList;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.API;
import com.wire.bots.sdk.user.model.Access;
import io.dropwizard.auth.AuthenticationException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class NotificationProcessor implements Runnable {
    private final Client client;
    private final AccessDAO accessDAO;

    NotificationProcessor(Client client, AccessDAO accessDAO) {
        this.client = client;
        this.accessDAO = accessDAO;
    }

    @Override
    public void run() {
        while (true) {
            try {
                List<com.wire.bots.hold.model.Access> accesses = accessDAO.listAll();
                Logger.debug("Devices: %d", accesses.size());

                for (com.wire.bots.hold.model.Access a : accesses) {
                    try {
                        NotificationList notificationList = retrieveNotifications(a, 100);
                        if (notificationList.notifications.isEmpty())
                            continue;

                        Logger.debug("Processing %d msg. %s:%s, last: %s",
                                notificationList.notifications.size(),
                                a.userId,
                                a.clientId,
                                a.last);

                        process(a.userId, a.clientId, notificationList);
                    } catch (AuthenticationException e) {
                        refreshToken(a.userId, a.token, a.cookie);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                int seconds = 30;
                Logger.debug("Sleeping %d seconds...\n", seconds);
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static String bearer(String token) {
        return token == null ? null : String.format("Bearer %s", token);
    }

    private void removeAccess(UUID userId) {
        accessDAO.remove(userId);
    }

    private void process(UUID userId, String clientId, NotificationList notificationList) throws Exception {
        for (Notification notif : notificationList.notifications) {
            for (Payload payload : notif.payload) {
                if (!process(userId, clientId, payload)) {
                    Logger.error("Failed to process: user: %s, notif: %s", userId, notif.id);
                    return;
                }
            }

            if (0 == accessDAO.updateLast(userId, notif.id, (int) Instant.now().getEpochSecond()))
                Logger.error("Failed to update Last. user: %s notif: %s", userId, notif.id);
        }
    }

    private boolean process(UUID userId, String clientId, Payload payload) throws JsonProcessingException {
        if (Logger.getLevel() == Level.FINE) {
            ObjectMapper objectMapper = new ObjectMapper();
            Logger.debug(objectMapper.writeValueAsString(payload));
        }

        Logger.debug("Payload: %s %s:%s, from: %s",
                payload.type,
                userId,
                clientId,
                payload.from);

        if (payload.from == null || payload.data == null)
            return true;

        Response response = client.target("http://localhost:8081/admin")
                .path(userId.toString())
                .path("messages")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200) {
            Logger.error("process: user: %s, error: %s, status: %d",
                    userId,
                    response.readEntity(String.class),
                    response.getStatus());
        }

        return response.getStatus() == 200;
    }

    private NotificationList retrieveNotifications(com.wire.bots.hold.model.Access access, int size)
            throws AuthenticationException {
        WebTarget target = client.target(Util.getHost())
                .path("notifications")
                .queryParam("client", access.clientId)
                .queryParam("since", access.last)
                .queryParam("size", size);

        Response response = target.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(access.token))
                .get();

        Logger.debug("retrieveNotifications: %s:%s, last: %s, status: %s",
                access.userId,
                access.clientId,
                access.last,
                response.getStatus());

        if (response.getStatus() == 401)
            throw new AuthenticationException(response.readEntity(String.class));

        return response.readEntity(NotificationList.class);
    }

    private void refreshToken(UUID userId, String token, String cookie) {
        try {
            Logger.debug("Refreshing token for: %s", userId);
            API api = new API(client, null, token);
            Access newAccess = api.renewAccessToken(cookie);
            cookie = newAccess.cookie != null ? newAccess.cookie : cookie;
            if (0 == accessDAO.update(userId, newAccess.token, cookie, (int) Instant.now().getEpochSecond()))
                Logger.error("refreshToken failed to update");
        } catch (com.wire.bots.sdk.exceptions.AuthenticationException e) {
            Logger.error("refreshToken: %s %s", userId, e);
            //removeAccess(userId);
        } catch (HttpException e) {
            e.printStackTrace();
        }
    }
}

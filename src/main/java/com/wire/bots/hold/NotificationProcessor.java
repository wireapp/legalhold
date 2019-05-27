package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.model.Config;
import com.wire.bots.hold.model.Notification;
import com.wire.bots.hold.model.NotificationList;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

public class NotificationProcessor implements Runnable {
    private final Client client;
    private final Database database;
    private final Config config;

    NotificationProcessor(Client client, Database database, Config config) {
        this.client = client;
        this.database = database;
        this.config = config;
    }

    @Override
    public void run() {
        while (true) {
            try {
                ArrayList<Database._Access> access = database.getAccess();
                Logger.debug("Devices: %d", access.size());

                for (Database._Access a : access) {
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

    private void refreshToken(UUID userId, String token, String cookie) {
        try {
            Logger.debug("Refreshing token for: %s", userId);
            API api = new API(client, null, token);
            Access newAccess = api.renewAccessToken(cookie);
            cookie = newAccess.cookie != null ? newAccess.cookie : cookie;
            if (!database.updateAccess(userId, newAccess.token, cookie))
                Logger.error("refreshToken failed to update");
        } catch (Exception e) {
            Logger.error("refreshToken: %s %s", userId, e);
            //removeAccess(userId);
        }
    }

    private void removeAccess(UUID userId) {
        try {
            database.removeAccess(userId);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }

    private void process(UUID userId, String clientId, NotificationList notificationList) throws Exception {
        for (Notification notif : notificationList.notifications) {
            for (Payload payload : notif.payload) {
                if (!process(userId, clientId, payload)) {
                    Logger.error("Failed to process: user: %s, notif: %s", userId, notif.id);
                    //return;
                }
            }

            if (!database.updateLast(userId, notif.id))
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

    private NotificationList retrieveNotifications(Database._Access access, int size)
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

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}

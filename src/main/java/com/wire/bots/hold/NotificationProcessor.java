package com.wire.bots.hold;

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

public class NotificationProcessor implements Runnable {
    private final Client client;
    private final Config config;
    private final Database database;

    NotificationProcessor(Client client, Config config) {
        this.client = client;
        this.config = config;
        this.database = new Database(config.storage);

    }

    @Override
    public void run() {
        while (true) {
            try {
                ArrayList<Database._Access> access = database.getAccess();
                Logger.info("Devices: %d", access.size());

                for (Database._Access a : access) {
                    try {
                        NotificationList notificationList = retrieveNotifications(a.clientId, a.last, a.token, 100);
                        if (notificationList.notifications.isEmpty())
                            continue;

                        Logger.info("");
                        Logger.info("Processing %d msg. %s:%s, last: %s",
                                notificationList.notifications.size(),
                                a.userId,
                                a.clientId,
                                a.last);

                        process(a.userId, notificationList);
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
                Logger.info("Sleeping %d seconds...\n", seconds);
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
                Logger.warning("refreshToken failed to update");
        } catch (Exception e) {
            Logger.error("refreshToken: %s %s", userId, e);
        }
    }

    private void process(UUID userId, NotificationList notificationList) throws SQLException {
        for (Notification notif : notificationList.notifications) {
            for (Payload payload : notif.payload) {
                if (!process(userId, payload)) {
                    //Logger.error("Failed to process: user: %s, notif: %s", userId, notif.id);
                    //return;
                }
            }

            if (!database.updateLast(userId, notif.id))
                Logger.warning("Failed to update Last. user: %s notif: %s", userId, notif.id);
        }
    }

    private boolean process(UUID userId, Payload payload) {
        if (!payload.type.equals("conversation.otr-message-add")) {
            return true;
        }

        Logger.info("Payload: %s:%s, from: %s:%s",
                userId,
                payload.data.recipient,
                payload.from,
                payload.data.sender);
        
        Response response = client.target(config.baseUrl)
                .path("bots")
                .path(userId.toString())
                .path("messages")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(config.auth))
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200) {
            Logger.debug("process: user: %s, error: %s, status: %d",
                    userId,
                    response.readEntity(String.class),
                    response.getStatus());
        }

        return response.getStatus() == 200;
    }

    private NotificationList retrieveNotifications(String clientId, String last, String token, int size)
            throws AuthenticationException {
        WebTarget target = client.target(Util.getHost())
                .path("notifications")
                .queryParam("client", clientId)
                .queryParam("since", last)
                .queryParam("size", size);

        Response response = target.request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .get();

        Logger.debug("retrieveNotifications: %s, last: %s, status: %s", clientId, last, response.getStatus());

        if (response.getStatus() == 401)
            throw new AuthenticationException(response.readEntity(String.class));

        return response.readEntity(NotificationList.class);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}

package com.wire.bots.hold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.model.LHAccess;
import com.wire.bots.hold.model.Notification;
import com.wire.bots.hold.model.NotificationList;
import com.wire.bots.sdk.exceptions.AuthException;
import com.wire.bots.sdk.exceptions.HttpException;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.LoginClient;
import com.wire.bots.sdk.user.model.Access;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
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

    NotificationProcessor(Client client, AccessDAO accessDAO) {
        this.client = client;
        this.accessDAO = accessDAO;
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
            //e.printStackTrace();
            Logger.error("NotificationProcessor: %s", e);
        }
    }

    private Access getAccess(Cookie cookie) throws HttpException {
        LoginClient loginClient = new LoginClient(client);
        return loginClient.renewAccessToken(cookie);
    }

    private void process(LHAccess device) {
        UUID userId = device.userId;
        try {
            Logger.debug("`GET /notifications`: user: %s, last: %s",
                    userId,
                    device.last);

            String cookieValue = device.cookie;

            Cookie cookie = new Cookie("zuid", device.cookie);

            Access access = getAccess(cookie);

            if (access.getCookie() != null) {
                Logger.info("Set-Cookie: user: %s", userId);
                cookieValue = access.getCookie().getValue();
            }

            accessDAO.update(userId, access.token, cookieValue);

            device.token = access.getToken();

            NotificationList notificationList = retrieveNotifications(device, 100);

            process(userId, device.clientId, notificationList);

        } catch (AuthException e) {
            accessDAO.disable(userId);
            Logger.warning("Disabled LH device for user: %s, error: ", userId, e);
        } catch (Exception e) {
            Logger.error("NotificationProcessor: user: %s, last: %s, error: %s", userId, device.last, e);
        }
    }

    private static String bearer(String token) {
        return token == null ? null : String.format("Bearer %s", token);
    }

    private void process(UUID userId, String clientId, NotificationList notificationList) throws Exception {

        for (Notification notif : notificationList.notifications) {
            for (Payload payload : notif.payload) {
                if (!process(userId, clientId, payload, notif.id)) {
                    Logger.error("Failed to process: user: %s, notif: %s", userId, notif.id);
                    //return;
                } else {
                    Logger.debug("Processed: `%s` conv: %s, user: %s:%s, notifId: %s",
                            payload.type,
                            payload.convId,
                            userId,
                            clientId,
                            notif.id);
                }
            }

            accessDAO.updateLast(userId, notif.id);
        }
    }

    private boolean process(UUID userId, String clientId, Payload payload, UUID id) throws JsonProcessingException {
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
                .queryParam("id", id)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200) {
            Logger.error("process: `%s` user: %s, from: %s:%s, error: %s",
                    payload.type,
                    userId,
                    payload.from,
                    payload.data.sender,
                    response.readEntity(String.class));
        }

        return response.getStatus() == 200;
    }

    private NotificationList retrieveNotifications(LHAccess LHAccess, int size)
            throws HttpException {
        Response response = client.target(Util.getHost())
                .path("notifications")
                .queryParam("client", LHAccess.clientId)
                .queryParam("since", LHAccess.last)
                .queryParam("size", size)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(LHAccess.token))
                .get();

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

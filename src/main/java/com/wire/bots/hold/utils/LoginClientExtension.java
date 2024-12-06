package com.wire.bots.hold.utils;

import com.wire.helium.LoginClient;
import com.wire.helium.models.Access;
import com.wire.xenon.exceptions.HttpException;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Cookie;

public class LoginClientExtension {
    public static Access refreshToken(Client client, String clientId, String cookieValue) {
        try {
            LoginClient loginClient = new LoginClient(client);
            Cookie cookie = new Cookie("zuid", cookieValue);
            Access access = loginClient.renewAccessToken(clientId, cookie);
            if (access.getCookie() == null) {
                // If the cookie is not returned, we need sett the previous one as fallback
                com.wire.helium.models.Cookie cookie1 = new com.wire.helium.models.Cookie();
                cookie1.name = "zuid";
                cookie1.value = cookieValue;
                access.setCookie(cookie1);
            }
            return access;
        } catch (HttpException e) {
            throw new RuntimeException("LoginException, cannot fetch access token with the provided refreshToken", e);
        }
    }
}

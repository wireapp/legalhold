package com.wire.bots.hold.filters;

import com.wire.bots.hold.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Objects;

@Provider
public class ServiceAuthenticationFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        Cookie authCookie = requestContext.getCookies().get("W-Legal-Hold");
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authCookie == null && authHeader == null) {
            Exception cause = new IllegalArgumentException("Missing Authorization");
            throw new WebApplicationException(cause, Response.Status.UNAUTHORIZED);
        }

        if (authHeader == null)
            authHeader = authCookie.getValue();

        final String token = String.format("Bearer %s", Service.instance.getConfig().token);

        if (!Objects.equals(token, authHeader)) {
            Exception cause = new IllegalArgumentException("Wrong service token");
            throw new WebApplicationException(cause, Response.Status.UNAUTHORIZED);
        }
    }

    @Provider
    public static class ServiceAuthenticationFeature implements DynamicFeature {
        @Override
        public void configure(ResourceInfo resourceInfo, FeatureContext context) {
            if (resourceInfo.getResourceMethod().getAnnotation(ServiceAuthorization.class) != null) {
                context.register(ServiceAuthenticationFilter.class);
            }
        }
    }
}
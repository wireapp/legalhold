package com.wire.bots.hold.monitoring;

import org.slf4j.MDC;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

/**
 * Filter that sets MDC.
 */
@Provider
public class RequestMdcFactoryFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
        // save id generated by the Nginx
        addIfNotNull("forwardedFor", requestContext.getHeaderString("X-Request-Id"));
        // generate unique id for each request in the application
        addIfNotNull("appRequest", UUID.randomUUID().toString());
        // header from proxy
        addIfNotNull("forwardedFor", requestContext.getHeaderString("X-Forwarded-For"));
        addIfNotNull("realIp", requestContext.getHeaderString("X-Real-IP"));
    }

    private void addIfNotNull(final String key, @Nullable String value) {
        if (value != null && !value.trim().isEmpty()) {
            MDC.put(key, value);
        }
    }
}

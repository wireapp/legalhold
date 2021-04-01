package com.wire.bots.hold.monitoring;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.Level;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Layout used on Wire production services in the ELK stack - for access events -> HTTP log.
 */
public class AccessEventJsonLayout extends AbstractJsonLayout<IAccessEvent> {
    @Override
    public String doLayout(IAccessEvent event) {
        final Map<String, Object> jsonMap = new LinkedHashMap<>(10);

        jsonMap.put("@timestamp", formatTime(event));
        jsonMap.put("type", "http");
        jsonMap.put("logger", "HttpRequest");

        jsonMap.put("level", Level.INFO.levelStr);
        jsonMap.put("requestURI", event.getRequestURI());
        // put there query only if it is not empty
        final String query = event.getQueryString();
        if (query != null && !query.trim().isEmpty()) {
            jsonMap.put("query", query);
        }
        jsonMap.put("remoteHost", event.getRemoteHost());
        jsonMap.put("remoteAddr", event.getRemoteAddr());
        jsonMap.put("method", event.getMethod());
        jsonMap.put("elapsedMls", event.getElapsedTime());
        // we check for null, even though there shouldn't be null, better be safe then sorry
        final HttpServletResponse response = event.getResponse();
        if (response != null) {
            jsonMap.put("responseStatus", response.getStatus());
        }
        return finalizeLog(jsonMap);
    }
}

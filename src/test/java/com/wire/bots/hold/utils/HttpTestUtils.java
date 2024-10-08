package com.wire.bots.hold.utils;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.wire.bots.hold.Config;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;

public class HttpTestUtils {
    public static Client createHttpClient(Config config, Environment env) {
        return new JerseyClientBuilder(env)
            .using(config.getJerseyClient())
            .withProvider(MultiPartFeature.class)
            .withProvider(JacksonJsonProvider.class)
            .build("test-client");
    }
}

package com.wire.bots.hold.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.helium.API;
import com.wire.xenon.backend.models.Conversation;
import com.wire.xenon.backend.models.Member;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.UUID;

public class LegalHoldAPI extends API {
    private final WebTarget conversationsPath;

    private final UUID convId;
    private final String token;

    public LegalHoldAPI(Client client, UUID convId, String token) {
        super(client, convId, token);

        this.convId = convId;
        this.token = token;

        WebTarget target = client
                .target(host());

        conversationsPath = target.path("legalhold/conversations");
    }

    @Override
    public Conversation getConversation() {
        _Conv conv = conversationsPath.
                path(convId.toString()).
                request().
                header(HttpHeaders.AUTHORIZATION, bearer(token)).
                get(_Conv.class);

        Conversation ret = new Conversation();
        ret.name = conv.name;
        ret.id = conv.id;
        ret.members = conv.members.others;
        return ret;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class _Conv {
        @JsonProperty
        public UUID id;

        @JsonProperty
        public String name;

        @JsonProperty
        public _Members members;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class _Members {
        @JsonProperty
        public List<Member> others;
    }
}

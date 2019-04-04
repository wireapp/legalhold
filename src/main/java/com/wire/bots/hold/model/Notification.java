package com.wire.bots.hold.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.bots.sdk.server.model.Payload;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification {
    @JsonProperty
    @NotNull
    public ArrayList<Payload> payload;

    @JsonProperty
    @NotNull
    public String id;
}

package com.wire.bots.hold.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.xenon.backend.models.Payload;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification {
    @JsonProperty
    @NotNull
    public List<Payload> payload;

    @JsonProperty
    @NotNull
    public UUID id;
}

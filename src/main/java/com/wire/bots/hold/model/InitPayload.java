package com.wire.bots.hold.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InitPayload {
    @JsonProperty
    @NotNull
    public UUID userId;

    @JsonProperty
    @NotNull
    public UUID teamId;
}

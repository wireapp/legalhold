package com.wire.bots.hold.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmPayload {
    @JsonProperty("access_token")
    @NotNull
    public String accessToken;

    @JsonProperty("refresh_token")
    @NotNull
    public String refreshToken;

    @JsonProperty
    @NotNull
    public String clientId;

    @JsonProperty
    @NotNull
    public UUID userId;

    @JsonProperty
    @NotNull
    public UUID teamId;
}

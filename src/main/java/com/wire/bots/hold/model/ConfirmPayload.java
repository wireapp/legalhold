package com.wire.bots.hold.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmPayload {
    @JsonProperty("access_token")
    public String accessToken;

    @JsonProperty("refresh_token")
    @NotNull
    public String refreshToken;

    @JsonProperty("client_id")
    @NotNull
    public String clientId;

    @JsonProperty("user_id")
    @NotNull
    public UUID userId;

    @JsonProperty("team_id")
    @NotNull
    public UUID teamId;
}

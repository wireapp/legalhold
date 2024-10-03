package com.wire.bots.hold.model.api.v0;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmPayloadV0 {
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

package com.wire.bots.hold.model.api.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.xenon.backend.models.QualifiedId;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfirmPayloadV1 {
    @JsonProperty("refresh_token")
    @NotNull
    public String refreshToken;

    @JsonProperty("client_id")
    @NotNull
    public String clientId;

    @JsonProperty("qualified_user_id")
    @NotNull
    public QualifiedId userId;

    @JsonProperty("team_id")
    @NotNull
    public UUID teamId;
}

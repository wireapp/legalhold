package com.wire.bots.hold.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.xenon.models.otr.PreKey;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitResponse {
    @JsonProperty("last_prekey")
    @NotNull
    public PreKey lastPreKey;

    @JsonProperty("prekeys")
    @NotNull
    @NotEmpty
    public ArrayList<PreKey> preKeys;

    @JsonProperty
    @NotNull
    @NotEmpty
    public String fingerprint;
}
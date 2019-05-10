package com.wire.bots.hold.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wire.bots.sdk.models.otr.PreKey;

import java.util.ArrayList;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitResponse {
    @JsonProperty("last_prekey")
    public PreKey lastPreKey;

    @JsonProperty("prekeys")
    public ArrayList<PreKey> preKeys;

    @JsonProperty
    public String fingeprint;
}
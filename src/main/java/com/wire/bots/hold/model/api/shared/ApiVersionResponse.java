package com.wire.bots.hold.model.api.shared;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ApiVersionResponse {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private final List<Integer> supported;

    public ApiVersionResponse(List<Integer> supported) {
        this.supported = supported;
    }
}

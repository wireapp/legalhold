package com.wire.bots.hold.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationList {
    @JsonProperty("has_more")
    @NotNull
    public Boolean hasMore;

    @JsonProperty
    @NotNull
    public ArrayList<Notification> notifications;
}

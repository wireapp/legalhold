package com.wire.bots.hold.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public class Event {
    public UUID messageId;
    public UUID conversationId;
    public String type;
    public JsonNode payload;
    public String time;
}

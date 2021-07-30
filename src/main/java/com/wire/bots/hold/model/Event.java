package com.wire.bots.hold.model;

import java.util.UUID;

public class Event {
    public UUID eventId;
    public UUID conversationId;
    public String type;
    public String payload;
    public String time;
}

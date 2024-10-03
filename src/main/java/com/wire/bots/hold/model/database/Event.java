package com.wire.bots.hold.model.database;

import java.util.UUID;

public class Event {
    public UUID eventId;
    public UUID conversationId;
    public UUID userId;
    public String type;
    public String payload;
    public String time;
}

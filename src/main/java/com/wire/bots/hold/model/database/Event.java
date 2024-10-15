package com.wire.bots.hold.model.database;

import java.util.UUID;

public class Event {
    public UUID eventId;
    public UUID conversationId;
    public String conversationDomain; // Keeping values split instead of using QualifiedId because of HTML templating
    public UUID userId;
    public String userDomain; // Keeping values split instead of using QualifiedId because of HTML templating
    public String type;
    public String payload;
    public String time;
}

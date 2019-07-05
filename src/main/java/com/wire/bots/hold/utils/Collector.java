package com.wire.bots.hold.utils;

import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.user.API;

import javax.annotation.Nullable;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Collector {
    private final API api;

    private LinkedList<Day> days = new LinkedList<>();
    private String convName;

    public Collector(API api) {
        this.api = api;
    }

    private static Message newMessage(TextMessage event) throws ParseException {
        Message message = new Message();
        message.text = event.getText();
        message.time = toTime(event.getTime());
        return message;
    }

    private static Day newDay(Sender sender, String dateTime) throws ParseException {
        Day day = new Day();
        day.date = toDate(dateTime);
        day.senders.add(sender);
        return day;
    }

    private static String toColor(int accent) {
        switch (accent) {
            case 1:
                return "#2391d3";
            case 2:
                return "#00c800";
            case 3:
                return "#febf02";
            case 4:
                return "#fb0807";
            case 5:
                return "#ff8900";
            case 6:
                return "#fe5ebd";
            default:
                return "#9c00fe";
        }
    }

    static String toTime(String timestamp) throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(timestamp);
        DateFormat df = new SimpleDateFormat("HH:mm");
        return df.format(date);
    }

    static String toDate(String timestamp) throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(timestamp);
        DateFormat df = new SimpleDateFormat("dd MMM, yyyy");
        return df.format(date);
    }

    public void add(TextMessage event) throws ParseException {
        Message message = newMessage(event);

        UUID senderId = event.getUserId();
        String dateTime = event.getTime();

        User user = Cache.getUser(api, senderId);
        if (user != null) {
            append(message, senderId, user.name, user.accent, dateTime);
        }
    }

    public void add(ImageMessage event) throws ParseException {
        Message message = newMessage(event);

        UUID senderId = event.getUserId();
        String dateTime = event.getTime();

        User user = Cache.getUser(api, senderId);
        if (user != null) {
            append(message, senderId, user.name, user.accent, dateTime);
        }
    }

    public void add(String text, String dateTime) throws ParseException {
        Message message = new Message();
        message.text = text;
        message.time = toTime(dateTime);

        String senderName = "System";
        int accent = 4;

        append(message, null, senderName, accent, dateTime);
    }

    private void append(Message message, @Nullable UUID senderId, String senderName, int accent, String dateTime)
            throws ParseException {
        Sender sender = newSender(senderId, senderName, accent, message);
        Day day = newDay(sender, dateTime);

        if (days.isEmpty()) {
            days.add(day);
            return;
        }

        Day lastDay = days.getLast();
        if (!lastDay.equals(day)) {
            days.add(day);
            return;
        }

        Sender lastSender = lastDay.senders.getLast();
        if (lastSender.equals(sender)) {
            lastSender.messages.add(message);
        } else {
            lastDay.senders.add(sender);
        }
    }

    private Message newMessage(ImageMessage event) throws ParseException {
        Message message = new Message();
        message.time = toTime(event.getTime());

        File file = Cache.getImage(api, event);
        if (file != null && file.exists())
            message.image = String.format("file://%s", file.getAbsolutePath());
        return message;
    }

    private Sender newSender(UUID senderId, String senderName, int accent, Message message) {
        Sender sender = new Sender();
        sender.senderId = senderId;
        sender.name = senderName;
        sender.accent = toColor(accent);
        sender.messages.add(message);

        File file = Cache.getProfileImage(api, sender.senderId);
        if (file != null && file.exists())
            sender.avatar = String.format("file://%s", file.getAbsolutePath());

        return sender;
    }

    public Conversation getConversation() {
        Conversation ret = new Conversation();
        ret.days = days;
        ret.title = convName;
        return ret;
    }

    public void setConvName(String convName) {
        this.convName = convName;
    }

    public static class Conversation {
        public LinkedList<Day> days = new LinkedList<>();
        public String title;
    }


    public static class Day {
        public String date;
        public LinkedList<Sender> senders = new LinkedList<>();

        public boolean equals(Day d) {
            return Objects.equals(date, d.date);
        }
    }

    public static class Message {
        public String text;
        public String image;
        public String time;
    }

    public static class Sender {
        public UUID senderId;
        public String avatar;
        public String name;
        public String accent;
        public ArrayList<Message> messages = new ArrayList<>();

        public boolean equals(Sender s) {
            return Objects.equals(senderId, s.senderId);
        }
    }
}

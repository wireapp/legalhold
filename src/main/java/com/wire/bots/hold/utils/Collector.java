package com.wire.bots.hold.utils;

import com.wire.bots.sdk.models.MessageAssetBase;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.User;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Collector {
    private final Cache cache;

    private LinkedList<Day> days = new LinkedList<>();
    private String convName;


    public Collector(Cache cache) {
        this.cache = cache;
    }

    public void add(TextMessage event) throws ParseException {
        Message message = new Message();
        message.text = Helper.markdown2Html(event.getText(), true);
        message.time = toTime(event.getTime());

        UUID senderId = event.getUserId();
        String dateTime = event.getTime();

        User user = cache.getUser(senderId);
        append(user, message, dateTime);
    }

    public void add(MessageAssetBase event) throws ParseException {
        File file = cache.getAssetFile(event);
        if (file.exists()) {
            Message message = new Message();
            message.time = toTime(event.getTime());

            String assetFilename = getFilename(file, "images");

            String mimeType = event.getMimeType();
            if (mimeType.startsWith("image")) {
                message.image = assetFilename;
            } else {
                String url = String.format("<a href=\"%s\">%s</a>",
                        assetFilename,
                        event.getName());
                message.text = Helper.markdown2Html(url, false);
            }

            UUID senderId = event.getUserId();
            User user = cache.getUser(senderId);

            append(user, message, event.getTime());
        }
    }

    public void add(String text, String dateTime) throws ParseException {
        Message message = new Message();
        message.system = Helper.markdown2Html(text, true);
        message.time = toTime(dateTime);
        User user = new User();
        user.id = UUID.randomUUID();
        user.name = "System";
        user.accent = 4;

        append(user, message, dateTime);
    }

    private Sender newSender(User user, Message message) {
        Sender sender = new Sender();
        sender.senderId = user.id;
        sender.name = user.name;
        sender.accent = toColor(user.accent);
        sender.messages.add(message);

        File file = cache.getProfileImage(user);
        if (file.exists()) {
            sender.avatar = getFilename(file, "avatars");
        }
        return sender;
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

    private void append(User user, Message message, String dateTime) throws ParseException {
        Sender sender = newSender(user, message);
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

    private String getFilename(File file, String dir) {
        return String.format("/legalhold/%s/%s", dir, file.getName());
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
        LinkedList<Day> days = new LinkedList<>();
        String title;

        public String getTitle() {
            return title;
        }
    }

    public static class Day {
        String date;
        LinkedList<Sender> senders = new LinkedList<>();

        boolean equals(Day d) {
            return Objects.equals(date, d.date);
        }
    }

    public static class Message {
        String text;
        String image;
        String system;
        String time;
    }

    public static class Sender {
        UUID senderId;
        String avatar;
        String name;
        String accent;
        ArrayList<Message> messages = new ArrayList<>();

        boolean equals(Sender s) {
            return Objects.equals(senderId, s.senderId);
        }
    }
}

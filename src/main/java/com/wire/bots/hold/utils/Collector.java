package com.wire.bots.hold.utils;

import com.wire.xenon.backend.models.User;
import com.wire.xenon.models.OriginMessage;
import com.wire.xenon.models.TextMessage;

import javax.annotation.Nullable;
import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Collector {
    private final Cache cache;
    private final LinkedList<Day> days = new LinkedList<>();
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
        Sender sender = sender(user, message);
        append(sender, message, dateTime);
    }

    public void add(OriginMessage event) throws ParseException {
        File file = cache.getAssetFile(event.getMessageId());
        if (file != null && file.exists()) {
            Message message = new Message();
            message.time = toTime(event.getTime());

            if (event.getMimeType().startsWith("image")) {
                message.image = getFilename(file);
            } else {
                String url = String.format("<a href=\"%s\">%s</a>",
                        getFilename(file),
                        event.getName());
                message.text = Helper.markdown2Html(url, false);
            }

            UUID senderId = event.getUserId();
            User user = cache.getUser(senderId);

            Sender sender = sender(user, message);
            append(sender, message, event.getTime());
        }
    }

    public void addSystem(String text, String dateTime, String type) throws ParseException {
        Message message = new Message();
        message.text = Helper.markdown2Html(text, true);
        message.time = toTime(dateTime);

        Sender sender = system(message, type);
        append(sender, message, dateTime);
    }

    private Sender sender(User user, Message message) {
        Sender sender = new Sender();
        sender.senderId = user.id;
        sender.name = user.name;
        sender.accent = toColor(user.accent);
        sender.avatar = getAvatar(user);
        sender.messages.add(message);
        return sender;
    }

    private Sender system(Message message, String type) {
        Sender sender = new Sender();
        sender.system = "system";
        sender.senderId = UUID.randomUUID();
        sender.avatar = systemIcon(type);
        sender.messages.add(message);
        return sender;
    }

    @Nullable
    private String systemIcon(String type) {
        final String base = "/assets/";
        switch (type) {
            case "conversation.create":
            case "conversation.member-join":
                return base + "icons8-plus-24.png";
            case "conversation.member-leave":
                return base + "icons8-minus-24.png";
            case "conversation.rename":
            case "conversation.otr-message-add.edit-text":
                return base + "icons8-edit-30.png";
            case "conversation.otr-message-add.call":
                return base + "icons8-end-call-30.png";
            case "conversation.otr-message-add.delete-text":
                return base + "icons8-delete.png";

            default:
                return null;
        }
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

    private void append(Sender sender, Message message, String dateTime) throws ParseException {
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

    private String getFilename(File file) {
        return String.format("/%s/%s", "images", file.getName());
    }

    private String getAvatar(User user) {
        File file = cache.getProfileImage(user);
        String ret = String.format("/%s/%s", "avatars", file.getName());
        return file.exists() ? ret : null;
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
        String time;
    }

    public static class Sender {
        UUID senderId;
        String avatar;
        String name;
        String accent;
        String system;
        ArrayList<Message> messages = new ArrayList<>();

        boolean equals(Sender s) {
            return Objects.equals(senderId, s.senderId);
        }
    }
}

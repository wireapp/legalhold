package com.wire.bots.hold.utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.wire.xenon.tools.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class HtmlGenerator {

    private final static MustacheFactory mustacheFactory;

    static {
        mustacheFactory = new DefaultMustacheFactory();
    }

    private static Mustache indexTemplate() {
        String path = "templates/index.html";
        return mustacheFactory.compile(path);
    }

    private static Mustache eventsTemplate() {
        String path = "templates/events.html";
        return mustacheFactory.compile(path);
    }

    private static Mustache devicesTemplate() {
        String path = "templates/devices.html";
        return mustacheFactory.compile(path);
    }

    private static Mustache conversationTemplate() {
        String path = "templates/conversation.html";
        return mustacheFactory.compile(path);
    }

    public static String execute(Object model, TemplateType templateType) {
        Mustache template = null;

        switch (templateType) {
            case INDEX:
                template = indexTemplate();
                break;
            case EVENTS:
                template = eventsTemplate();
                break;
            case DEVICES:
                template = devicesTemplate();
                break;
            case CONVERSATION:
                template = conversationTemplate();
                break;
        }

        try (StringWriter sw = new StringWriter()) {
            template.execute(new PrintWriter(sw), model).flush();
            return sw.toString();
        } catch (IOException exception) {
            Logger.exception(exception, "Unable to compile HTML template");
            return "";
        }
    }

    public enum TemplateType {
        INDEX,
        EVENTS,
        DEVICES,
        CONVERSATION
    }
}

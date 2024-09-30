package com.wire.bots.hold;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.wire.bots.hold.utils.Collector;
import com.wire.bots.hold.utils.PdfGenerator;
import com.wire.bots.hold.utils.TestCache;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.models.OriginMessage;
import com.wire.xenon.models.PhotoPreviewMessage;
import com.wire.xenon.models.TextMessage;
import com.wire.xenon.tools.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.text.ParseException;
import java.util.UUID;

public class MessageTemplateTest {
    private static TextMessage txt(QualifiedId userId, String time, String text) {
        TextMessage msg = new TextMessage(UUID.randomUUID(),
            UUID.randomUUID(),
            new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString()),
            UUID.randomUUID().toString(),
            userId,
            time);
        msg.setText(text);
        return msg;
    }

    private static OriginMessage asset(QualifiedId userId, String time, String name, String mime) {
        return new PhotoPreviewMessage(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString()),
            UUID.randomUUID().toString(),
            userId,
            time,
            mime, 0, name, 0, 0);
    }

    @Before
    public void makeDirs() {
        File f = new File("src/test/output");
        f.mkdir();
    }

    @Test
    public void templateHtmlTest() throws Exception {
        Mustache mustache = compileTemplate("conversation.html");

        Collector.Conversation conversation = getConversation();
        String html = execute(mustache, conversation);
        assert html != null;
        File file = new File(String.format("src/test/output/%s.html", conversation.getTitle()));
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(file))) {
            os.write(html.getBytes());
        }
    }

    @Test
    public void templatePdfTest() throws Exception {
        Mustache mustache = compileTemplate("conversation.html");

        Collector.Conversation conversation = getConversation();
        String html = execute(mustache, conversation);
        assert html != null;
        String pdfFilename = String.format("src/test/output/%s.pdf", conversation.getTitle());
        PdfGenerator.save(pdfFilename, html, "file:src/test");
    }

    @SuppressWarnings("SameParameterValue")
    private Mustache compileTemplate(String template) {
        MustacheFactory mf = new DefaultMustacheFactory();
        String path = String.format("templates/%s", template);
        Mustache mustache = mf.compile(path);
        Assert.assertNotNull(path, mustache);
        return mustache;
    }

    private String execute(Mustache mustache, Object model) {
        try (StringWriter sw = new StringWriter()) {
            mustache.execute(new PrintWriter(sw), model).flush();
            return sw.toString();
        } catch (Exception e) {
            Logger.exception("Mustache template write failed.", e);
            return null;
        }
    }

    private Collector.Conversation getConversation() throws ParseException {
        final String thursday = "2019-07-08T08:35:21.348Z";
        final String friday = "2019-07-09T18:21:17.548Z";
        final String saturday = "2019-07-10T21:11:47.149Z";
        final QualifiedId user1 = new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString());
        final QualifiedId user2 = new QualifiedId(UUID.randomUUID(), UUID.randomUUID().toString());

        Collector collector = new Collector(new TestCache());
        String name = "Message Template Test";
        collector.setConvName(name);
        final String format = String.format("**Dejan** created conversation **%s** with: \n- **Lipis**", name);
        collector.addSystem(format, thursday, "conversation.create");
        collector.add(txt(user1, thursday, "Privet! Kak dela?"));
        collector.add(txt(user2, thursday, "Ladna"));
        collector.add(asset(user2, thursday, "i_know", "video/mp4"));
        collector.add(txt(user1, thursday, "😃🐠😴🤧✍️👉👨‍🚒👨‍🏫👩‍👦👨‍👧‍👦🐥🐧🐾🐁🐕🎋🐲🐉"));
        collector.add(txt(user1, thursday, "4"));
        collector.add(txt(user2, thursday, "5 👍"));
        collector.add(txt(user2, thursday, "😃Lorem ipsum **dolor** sit amet, consectetur adipiscing elit, sed " +
                "do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
                " quis nostrud exercitation ullamco _laboris_ nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum"));
        collector.add(txt(user1, friday, "7"));
        collector.add(txt(user2, saturday, "8"));
        collector.add(asset(user2, saturday, "ognjiste2", "image/png"));
        collector.add(asset(user2, saturday, "small", "image/png"));
        collector.add(txt(user1, saturday, "9"));
        collector.add(txt(user1, saturday, "10"));
        collector.add(txt(user2, saturday, "```collector.addSystem(img(dejan, friday," +
                " \"SP\", \"image/jpeg\"));\n" +
                "        collector.addSystem(txt(dejan, friday, \"7\"));\n" +
                "        collector.addSystem(txt(lipis, saturday, \"8\"));\n" +
                "        collector.addSystem(img(lipis, saturday, \"ognjiste2\", \"image/png\"));\n" +
                "        collector.addSystem(img(lipis, saturday, \"small\", \"image/png\"));\n" +
                "```"));
        collector.add(txt(user1, saturday, "12"));
        collector.add(txt(user2, saturday, "13"));
        collector.add(asset(user1, saturday, "ognjiste", "image/png"));
        collector.add(txt(user2, saturday, "14"));
        collector.add(txt(user2, saturday, "15"));
        collector.add(txt(user1, saturday, "Lorem ipsum **dolor** sit amet, consectetur adipiscing elit, sed " +
                "do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
                " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non _proident_, sunt in culpa qui officia deserunt mollit anim id est" +
                " laborum."));
        collector.add(txt(user2, saturday, "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed " +
                "do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
                " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est" +
                " laborum."));
        collector.add(txt(user1, saturday, "This is some url [google](https://google.com)"));
        collector.add(txt(user1, saturday, "https://google.com"));
        collector.add(txt(user2, saturday, "This is some url https://google.com and some text"));
        collector.add(txt(user1, saturday, "These two urls https://google.com https://wire.com"));
        collector.addSystem("**Lipis** left the conversation", saturday, "conversation.member-leave");
        collector.addSystem("**Tiago** joined the conversation", saturday, "conversation.member-join");
        collector.addSystem("**Tiago** deleted text: 'some text'", saturday, "conversation.otr-message-add.delete-text");
        collector.addSystem("**Tiago** called", saturday, "conversation.otr-message-add.call");

        collector.add(asset(user1, saturday, "ognjiste2", "image/png"));

        return collector.getConversation();
    }
}

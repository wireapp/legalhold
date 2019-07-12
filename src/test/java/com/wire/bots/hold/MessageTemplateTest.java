package com.wire.bots.hold;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.wire.bots.hold.utils.Collector;
import com.wire.bots.hold.utils.PdfGenerator;
import com.wire.bots.hold.utils.TestCache;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.text.ParseException;
import java.util.UUID;

import static com.wire.bots.hold.Consts.dejan;
import static com.wire.bots.hold.Consts.lipis;

public class MessageTemplateTest {
    private static TextMessage txt(UUID userId, String time, String text) {
        TextMessage msg = new TextMessage(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID().toString(), userId);
        msg.setText(text);
        msg.setTime(time);
        return msg;
    }

    private static ImageMessage img(UUID userId, String time, String assetId) {
        ImageMessage msg = new ImageMessage(UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                userId,
                assetId,
                null,
                null,
                null,
                0L,
                null,
                null);
        msg.setTime(time);
        return msg;
    }

    @Test
    public void templateTest() throws Exception {
        File f = new File("src/test/output");
        f.mkdir();

        Mustache mustache = compileTemplate("conversation.html");

        Collector.Conversation conversation = getConversation();
        String html = execute(mustache, conversation);
        assert html != null;
        File file = new File(String.format("src/test/output/%s.html", conversation.getTitle()));
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(file))) {
            os.write(html.getBytes());
        }

        conversation = getConversation();
        html = execute(mustache, conversation);
        String pdfFilename = String.format("src/test/output/%s.pdf", conversation.getTitle());
        PdfGenerator.save(pdfFilename, html, "file:src/test");
    }

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
            e.printStackTrace();
            return null;
        }
    }

    private Collector.Conversation getConversation() throws ParseException {
        final String thursday = "2019-07-08T08:35:21.348Z";
        final String friday = "2019-07-09T18:21:17.548Z";
        final String saturday = "2019-07-10T21:11:47.149Z";

        Collector collector = new Collector(new TestCache());
        collector.setConvName("Message Template Test");
        collector.add(img(dejan, thursday, "ognjiste"));
        collector.add("New conversations created by **Dejan** with: \n- **Lipis**", thursday);
        collector.add(txt(dejan, thursday, "Privet! Kak dela?"));
        collector.add(txt(lipis, thursday, "Ladna"));
        collector.add(txt(dejan, thursday, "😃🐠😴🤧✍️👉👨‍🚒👨‍🏫👩‍👦👨‍👧‍👦🐥🐧🐾🐁🐕🎋🐲🐉"));
        collector.add(txt(dejan, thursday, "4"));
        collector.add(txt(lipis, thursday, "5 👍"));
        collector.add(txt(lipis, thursday, "😃Lorem ipsum **dolor** sit amet, consectetur adipiscing elit, sed " +
                "do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
                " quis nostrud exercitation ullamco _laboris_ nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum"));
        collector.add(txt(dejan, friday, "7"));
        collector.add(txt(lipis, saturday, "8"));
        collector.add(img(lipis, saturday, "ognjiste2"));
        collector.add(img(lipis, saturday, "small"));
        collector.add(txt(dejan, saturday, "9"));
        collector.add(txt(dejan, saturday, "10"));
        collector.add(txt(lipis, saturday, "```collector.add(img(dejan, friday," +
                " \"SP\", \"image/jpeg\"));\n" +
                "        collector.add(txt(dejan, friday, \"7\"));\n" +
                "        collector.add(txt(lipis, saturday, \"8\"));\n" +
                "        collector.add(img(lipis, saturday, \"ognjiste2\", \"image/png\"));\n" +
                "        collector.add(img(lipis, saturday, \"small\", \"image/png\"));\n" +
                "```"));
        collector.add(txt(dejan, saturday, "12"));
        collector.add(txt(lipis, saturday, "13"));
        collector.add(img(dejan, saturday, "ognjiste"));
        collector.add(txt(lipis, saturday, "14"));
        collector.add(txt(lipis, saturday, "15"));
        collector.add(txt(dejan, saturday, "Lorem ipsum **dolor** sit amet, consectetur adipiscing elit, sed " +
                "do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
                " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non _proident_, sunt in culpa qui officia deserunt mollit anim id est" +
                " laborum."));
        collector.add(txt(lipis, saturday, "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed " +
                "do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
                " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
                "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
                "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est" +
                " laborum."));
        collector.add(txt(dejan, saturday, "This is some url [google](https://google.com)"));
        collector.add(txt(dejan, saturday, "https://google.com"));
        collector.add(txt(lipis, saturday, "This is some url https://google.com and some text"));
        collector.add(txt(dejan, saturday, "These two urls https://google.com https://wire.com"));
        collector.add("**Lipis** left the conversation", saturday);
        collector.add("**Tiago** joined the conversation", saturday);
        collector.add(img(dejan, saturday, "ognjiste2"));

        return collector.getConversation();
    }
}

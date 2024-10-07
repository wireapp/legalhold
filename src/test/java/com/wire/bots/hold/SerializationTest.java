package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.xenon.models.PhotoPreviewMessage;
import com.wire.xenon.models.TextMessage;
import org.junit.Test;

import java.io.IOException;

public class SerializationTest {
    @Test
    public void imageMessage() throws IOException {
        String payload = "{" +
                "\"userId\":\"dcd9bc95-955c-46de-bf0c-e35b86a06f52\"," +
                "\"clientId\":\"d21e42795ec5fe04\"," +
                "\"conversationId\":\"4303922a-4c79-418e-a5c1-6306b2106a42\"," +
                "\"messageId\":\"5f7e26e9-a14e-4345-8afb-0efc05fe9e29\"," +
                "\"time\":\"2019-07-04T10:36:02.693Z\"," +
                "\"assetKey\":\"3-1-0150a13c-0ee0-4e76-af4e-8a5f3577ea2e\"," +
                "\"assetToken\":\"ot_5tB-jq76JQOKDKPqKhg==\"," +
                "\"otrKey\":\"3udfVWHPFIV3CmF1Qb8MxoNHgrxaW7bnwJd6N/wig9Y=\"," +
                "\"mimeType\":\"image/jpeg\"," +
                "\"size\":319754," +
                "\"sha256\":\"W4Q9auNoyM8nTmHkLwKniTcY9Dh8Z69Omws++fa6z3c=\"," +
                "\"name\":null," +
                "\"height\":1085," +
                "\"width\":1448," +
                "\"tag\":null" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        PhotoPreviewMessage message = mapper.readValue(payload, PhotoPreviewMessage.class);
    }

    @Test
    public void txtMessage() throws IOException {
        String payload = "{" +
                "\"userId\":\"dcd9bc95-955c-46de-bf0c-e35b86a06f52\"," +
                "\"clientId\":\"d21e42795ec5fe04\"," +
                "\"conversationId\":\"4303922a-4c79-418e-a5c1-6306b2106a42\"," +
                "\"messageId\":\"362523ff-5064-48b9-b13f-9d17c661d4b5\"," +
                "\"time\":\"2019-07-04T09:05:37.994Z\"," +
                "\"text\":\"123\"" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        TextMessage message = mapper.readValue(payload, TextMessage.class);
    }

    @Test
    public void txtMessageNulls() throws IOException {
        String payload = "{\n" +
                "    \"clientId\": \"c0a19344b9290f12\", \n" +
                "    \"conversationId\": \"2f2730f4-260e-4485-aa48-07184842f9f0\", \n" +
                "    \"mentions\": [], \n" +
                "    \"messageId\": \"de9bdca1-f1b6-435c-90f9-ad8ad6124102\", \n" +
                "    \"quotedMessageId\": null, \n" +
                "    \"quotedMessageSha256\": null, \n" +
                "    \"text\": \"Hello world!\", \n" +
                "    \"time\": \"2020-05-06T15:52:12.175Z\", \n" +
                "    \"userId\": \"a5aef76b-4dd4-41f1-9e27-cdbc0f94f0c9\"\n" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        TextMessage message = mapper.readValue(payload, TextMessage.class);

        final String s = mapper.writeValueAsString(message);
    }
}

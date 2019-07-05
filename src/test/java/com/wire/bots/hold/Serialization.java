package com.wire.bots.hold;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import org.junit.Test;

import java.io.IOException;

public class Serialization {

    @Test
    public void imageMessage() throws IOException {
        String payload = "{\"userId\":\"dcd9bc95-955c-46de-bf0c-e35b86a06f52\",\"clientId\":\"d21e42795ec5fe04\",\"conversationId\":\"4303922a-4c79-418e-a5c1-6306b2106a42\",\"messageId\":\"5f7e26e9-a14e-4345-8afb-0efc05fe9e29\",\"time\":\"2019-07-04T10:36:02.693Z\",\"assetKey\":\"3-1-0150a13c-0ee0-4e76-af4e-8a5f3577ea2e\",\"assetToken\":\"ot_5tB-jq76JQOKDKPqKhg==\",\"otrKey\":\"3udfVWHPFIV3CmF1Qb8MxoNHgrxaW7bnwJd6N/wig9Y=\",\"mimeType\":\"image/jpeg\",\"size\":319754,\"sha256\":\"W4Q9auNoyM8nTmHkLwKniTcY9Dh8Z69Omws++fa6z3c=\",\"name\":null,\"height\":1085,\"width\":1448,\"tag\":null}";
        ObjectMapper mapper = new ObjectMapper();
        ImageMessage message = mapper.readValue(payload, ImageMessage.class);
    }

    @Test
    public void txtMessage() throws IOException {
        String payload = "{\"userId\":\"dcd9bc95-955c-46de-bf0c-e35b86a06f52\",\"clientId\":\"d21e42795ec5fe04\",\"conversationId\":\"4303922a-4c79-418e-a5c1-6306b2106a42\",\"messageId\":\"362523ff-5064-48b9-b13f-9d17c661d4b5\",\"time\":\"2019-07-04T09:05:37.994Z\",\"text\":\"123\"}";
        ObjectMapper mapper = new ObjectMapper();
        TextMessage message = mapper.readValue(payload, TextMessage.class);
    }
}

package com.wire.bots.hold.utils;

import com.wire.bots.sdk.models.MessageAssetBase;
import com.wire.bots.sdk.server.model.User;

import java.io.File;
import java.util.UUID;

import static com.wire.bots.hold.Consts.dejan;

public class TestCache extends Cache {
    public TestCache() {
        super(null);
    }

    @Override
    public File getAssetFile(MessageAssetBase message) {
        String extension = Helper.getExtension(message.getMimeType());
        return new File(String.format("src/test/legalhold/images/%s.%s", message.getAssetKey(), extension));
    }

    @Override
    public File getProfileImage(User user) {
        return new File(String.format("src/test/legalhold/avatars/%s.png", user.id));
    }

    @Override
    public User getUser(UUID userId) {
        User user = new User();
        user.id = userId;
        user.name = userId.equals(dejan) ? "Dejan" : "Lipis";
        user.accent = userId.equals(dejan) ? 3 : 5;
        return user;
    }
}

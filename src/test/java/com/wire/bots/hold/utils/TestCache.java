package com.wire.bots.hold.utils;

import com.wire.xenon.backend.models.User;

import java.io.File;
import java.util.UUID;

import static com.wire.bots.hold.Consts.dejan;

public class TestCache extends Cache {
    public TestCache() {
        super(null, null);
    }

    @Override
    public File getAssetFile(UUID messageId) {
        String extension = Helper.getExtension("image/png");
        return new File(String.format("src/test/images/%s.%s", messageId, extension));
    }

    @Override
    public File getProfileImage(User user) {
        return new File(String.format("src/test/avatars/%s.png", user.id));
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

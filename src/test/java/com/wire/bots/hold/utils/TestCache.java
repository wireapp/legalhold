package com.wire.bots.hold.utils;

import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.backend.models.User;

import java.io.File;
import java.util.UUID;

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
    public User getUser(QualifiedId userId) {
        User dummyUser = new User();

        dummyUser.id = userId;
        dummyUser.name = userId.toString();
        dummyUser.accent = 3;

        return dummyUser;
    }
}

package com.wire.bots.hold.utils;

import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.API;

import javax.annotation.Nullable;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private static final ConcurrentHashMap<String, File> pictures = new ConcurrentHashMap<>();//<assetKey, Picture>
    private static final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();//<userId, User>
    private static final ConcurrentHashMap<UUID, File> profiles = new ConcurrentHashMap<>();//<userId, Picture>
    private final API api;

    public Cache(API api) {
        this.api = api;
    }

    @Nullable
    public File getImage(ImageMessage message) {
        return pictures.computeIfAbsent(message.getAssetKey(), k -> Helper.downloadImage(api, message));
    }

    @Nullable
    public File getProfileImage(User user) {
        return profiles.computeIfAbsent(user.id, k -> Helper.getProfile(api, user));
    }

    public User getUser(UUID userId) {
        return users.computeIfAbsent(userId, k -> {
            try {
                return api.getUser(userId);
            } catch (Exception e) {
                Logger.warning("Cache.getUser: userId: %s, ex: %s", userId, e);
                User user = new User();
                user.id = userId;
                user.name = userId.toString();
                return user;
            }
        });
    }
}

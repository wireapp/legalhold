package com.wire.bots.hold.utils;

import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.user.API;

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

    public static void clear() {
        pictures.clear();
        users.clear();
        profiles.clear();
    }

    public File getImage(ImageMessage message) {
        File file = pictures.computeIfAbsent(message.getAssetKey(), k -> {
            try {
                return Helper.downloadImage(api, message);
            } catch (Exception e) {
                Logger.warning("Cache.getImage: %s", e);
                return null;
            }
        });

        if (file == null)
            file = Helper.imageFile(message.getAssetKey(), message.getMimeType());
        return file;
    }

    public File getProfileImage(User user) {
        File file = profiles.computeIfAbsent(user.id, k -> {
            try {
                return Helper.getProfile(api, user);
            } catch (Exception e) {
                Logger.warning("Cache.getProfileImage: userId: %s, ex: %s", user.id, e);
                return null;
            }
        });

        if (file == null)
            file = new File(Helper.avatarFile(user.id));
        return file;
    }

    public User getUser(UUID userId) {
        User user = users.computeIfAbsent(userId, k -> {
            try {
                return api.getUser(userId);
            } catch (Exception e) {
                Logger.warning("Cache.getUser: userId: %s, ex: %s", userId, e);
                return null;
            }
        });

        if (user == null) {
            user = new User();
            user.id = userId;
            user.name = userId.toString();
        }
        return user;
    }
}

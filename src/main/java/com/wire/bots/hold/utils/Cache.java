package com.wire.bots.hold.utils;

import com.wire.helium.API;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.exceptions.HttpException;
import com.wire.xenon.models.MessageAssetBase;
import com.wire.xenon.tools.Logger;

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

    public File getAssetFile(MessageAssetBase message) {
        File file = pictures.computeIfAbsent(message.getAssetKey(), k -> {
            try {
                return Helper.downloadAsset(api, message);
            } catch (Exception e) {
                Logger.exception("Cache.getAssetFile: %s", e, e.getMessage());
                return null;
            }
        });

        if (file == null)
            file = Helper.assetFile(message.getAssetKey(), message.getMimeType());
        return file;
    }

    public File getProfileImage(User user) {
        File file = profiles.computeIfAbsent(user.id, k -> {
            try {
                return Helper.getProfile(api, user);
            } catch (Exception e) {
                Logger.exception("Cache.getProfileImage: userId: %s, ex: %s", e, user.id, e.getMessage());
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
            } catch (HttpException e) {
                Logger.exception("Cache.getUser: userId: %s, ex: %s", e, userId, e.getMessage());
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

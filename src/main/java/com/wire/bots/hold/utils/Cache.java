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

    @Nullable
    public static File getImage(API api, ImageMessage message) {
        return pictures.computeIfAbsent(message.getAssetKey(), k -> {
            try {
                return Helper.downloadImage(api, message);
            } catch (Exception e) {
                Logger.warning("Cache.getImage: asset: %s, ex: %s", message.getAssetKey(), e);
                return null;
            }
        });
    }

    @Nullable
    public static File getProfileImage(API api, UUID userId) {
        if (userId == null)
            return null;

        return profiles.computeIfAbsent(userId, k -> {
            try {
                return Helper.getProfile(api, userId);
            } catch (Exception e) {
                Logger.warning("Cache.getProfileImage: user: %s, ex: %s", userId, e);
                return null;
            }
        });
    }

    @Nullable
    public static User getUser(API api, UUID userId) {
        if (userId == null)
            return null;

        return users.computeIfAbsent(userId, k -> {
            try {
                return api.getUser(userId);
            } catch (Exception e) {
                Logger.warning("Cache.getUser: userId: %s, ex: %s", userId, e);
                return null;
            }
        });
    }
}

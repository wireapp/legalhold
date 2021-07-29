package com.wire.bots.hold.utils;

import com.wire.bots.hold.DAO.AssetsDAO;
import com.wire.helium.API;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.exceptions.HttpException;
import com.wire.xenon.tools.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private static final ConcurrentHashMap<UUID, File> assets = new ConcurrentHashMap<>();//<messageId, File>
    private static final ConcurrentHashMap<UUID, User> users = new ConcurrentHashMap<>();//<userId, User>
    private static final ConcurrentHashMap<UUID, File> profiles = new ConcurrentHashMap<>();//<userId, Picture>
    private final API api;
    private final AssetsDAO assetsDAO;

    public Cache(API api, AssetsDAO assetsDAO) {
        this.api = api;
        this.assetsDAO = assetsDAO;
    }

    public static void clear() {
        assets.clear();
        users.clear();
        profiles.clear();
    }

    @Nullable
    public File getAssetFile(UUID messageId) {

        return assets.computeIfAbsent(messageId, k -> {
            try {
                final AssetsDAO.Asset asset = assetsDAO.get(messageId);
                File f = new File(String.format("%s.%s", messageId, Helper.getExtension(asset.mimeType)));
                Helper.save(asset.data, f);
                return f;
            } catch (Exception e) {
                Logger.exception("Cache.getAssetFile: %s", e, e.getMessage());
                return null;
            }
        });
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

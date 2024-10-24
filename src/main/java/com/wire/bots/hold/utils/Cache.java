package com.wire.bots.hold.utils;

import com.wire.bots.hold.DAO.AssetsDAO;
import com.wire.helium.API;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.exceptions.HttpException;
import com.wire.xenon.tools.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private static String FALLBACK_DOMAIN = null;

    private static final ConcurrentHashMap<UUID, File> assets = new ConcurrentHashMap<>(); // <messageId, File>
    private static final ConcurrentHashMap<QualifiedId, User> users = new ConcurrentHashMap<>(); // <QualifiedId, User>
    private static final ConcurrentHashMap<QualifiedId, File> profiles = new ConcurrentHashMap<>(); // <QualifiedId, Picture>
    private final API api;
    private final AssetsDAO assetsDAO;

    public Cache(API api, AssetsDAO assetsDAO) {
        this.api = api;
        this.assetsDAO = assetsDAO;
    }

    public static void setFallbackDomain(String domain) {
            FALLBACK_DOMAIN = domain;
    }

    public static String getFallbackDomain() {
        return FALLBACK_DOMAIN;
    }

    @Nullable
    public File getAssetFile(UUID messageId) {

        return assets.computeIfAbsent(messageId, k -> {
            try {
                final AssetsDAO.Asset asset = assetsDAO.get(messageId);
                File f = Helper.assetFile(asset.messageId, asset.mimeType);
                Helper.save(asset.data, f);
                return f;
            } catch (Exception e) {
                Logger.exception(e,"Cache.getAssetFile: %s", e.getMessage());
                return null;
            }
        });
    }

    public File getProfileImage(User user) {
        File file = profiles.computeIfAbsent(user.id, k -> {
            try {
                // TODO: Remove condition for null as String when new Xenon version is released
                if (user.id.domain == null || user.id.domain.equals("null") || user.id.domain.isEmpty()) {
                    user.id.domain = FALLBACK_DOMAIN;
                }

                return Helper.getProfile(api, user);
            } catch (Exception e) {
                Logger.exception(e, "Cache.getProfileImage: userId: %s, ex: %s", user.id, e.getMessage());
                return null;
            }
        });

        if (file == null)
            file = new File(Helper.avatarFile(user.id));
        return file;
    }

    public User getUser(QualifiedId userId) {
        return users.computeIfAbsent(userId, k -> {
            try {
                // TODO: Remove condition for null as String when new Xenon version is released
                if (userId.domain == null || userId.domain.equals("null") || userId.domain.isEmpty()) {
                    userId.domain = FALLBACK_DOMAIN;
                }

                return api.getUser(userId);
            } catch (HttpException e) {
                Logger.exception(e, "Cache.getUser: userId: %s, ex: %s", userId, e.getMessage());
                User user = new User();
                user.id = userId;
                user.name = userId.toString();
                return user;
            }
        });
    }
}

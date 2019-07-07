package com.wire.bots.hold.utils;

import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.server.model.Asset;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.API;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

class Helper {

    static File getProfile(API api, User user) {
        String filename = avatarPath(user.id);
        File file = new File(filename);
        try {
            for (Asset asset : user.assets) {
                if (asset.size.equals("preview")) {
                    byte[] profile = api.downloadAsset(asset.key, null);
                    save(profile, file);
                    break;
                }
            }
        } catch (Exception e) {
            Logger.warning("getProfile: %s", e);
        }
        return file;
    }

    static File downloadImage(API api, ImageMessage message) {
        File file = getFile(message.getAssetKey(), message.getMimeType());
        try {
            byte[] cipher = api.downloadAsset(message.getAssetKey(), message.getAssetToken());

            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(cipher);
            if (!Arrays.equals(sha256, message.getSha256()))
                throw new Exception("Failed sha256 check");

            byte[] image = Util.decrypt(message.getOtrKey(), cipher);
            save(image, file);
        } catch (Exception e) {
            Logger.warning("downloadImage: %s", e);
        }
        return file;
    }

    private static void save(byte[] image, File file) throws IOException {
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(file))) {
            os.write(image);
        }
    }

    private static File getFile(String assetKey, String mimeType) {
        String[] split = mimeType.split("/");
        String extension = split.length == 1 ? split[0] : split[1];
        String filename = String.format("images/%s.%s", assetKey, extension);
        return new File(filename);
    }

    private static String avatarPath(UUID senderId) {
        return String.format("avatars/%s.png", senderId);
    }
}

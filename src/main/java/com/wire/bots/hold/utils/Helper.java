package com.wire.bots.hold.utils;

import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.server.model.Asset;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Util;
import com.wire.bots.sdk.user.API;

import javax.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

class Helper {

    @Nullable
    static File getProfile(API api, UUID userId) throws Exception {
        if (userId == null)
            return null;

        User user = Cache.getUser(api, userId);
        if (user == null)
            return null;

        for (Asset asset : user.assets) {
            if (asset.size.equals("preview")) {
                String filename = avatarPath(user.id);
                File file = new File(filename);
                try (DataOutputStream os = new DataOutputStream(new FileOutputStream(file))) {
                    byte[] profile = api.downloadAsset(asset.key, null);
                    os.write(profile);
                }
                return file;
            }
        }
        return null;
    }

    static File downloadImage(API api, ImageMessage message) throws Exception {
        byte[] cipher = api.downloadAsset(message.getAssetKey(), message.getAssetToken());

        byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(cipher);
        if (!Arrays.equals(sha256, message.getSha256()))
            throw new Exception("Failed sha256 check");

        byte[] image = Util.decrypt(message.getOtrKey(), cipher);

        return saveImage(image, message.getAssetKey(), message.getMimeType());
    }

    private static File saveImage(byte[] image, String assetKey, String mimeType) throws IOException {
        File file = getFile(assetKey, mimeType);
        if (!file.exists()) {
            try (DataOutputStream os = new DataOutputStream(new FileOutputStream(file))) {
                os.write(image);
            }
        }
        return file;
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

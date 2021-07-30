package com.wire.bots.hold.utils;

import com.wire.helium.API;
import com.wire.xenon.backend.models.Asset;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.exceptions.HttpException;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

class Helper {

    static File getProfile(API api, User user) throws IOException, HttpException {
        String filename = avatarFile(user.id);
        File file = new File(filename);
        if (user.assets == null)
            return file;

        for (Asset asset : user.assets) {
            if (asset.size.equals("preview")) {
                byte[] profile = api.downloadAsset(asset.key, null);
                save(profile, file);
                break;
            }
        }
        return file;
    }

    public static File save(byte[] image, File file) throws IOException {
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(file))) {
            os.write(image);
        }
        return file;
    }

    static File assetFile(UUID messageId, String mimeType) {
        String extension = getExtension(mimeType);
        String filename = String.format("images/%s.%s", messageId, extension);
        return new File(filename);
    }

    static String getExtension(String mimeType) {
        String[] split = mimeType.split("/");
        return split.length == 1 ? split[0] : split[1];
    }

    static String avatarFile(UUID senderId) {
        return String.format("avatars/%s.png", senderId);
    }

    static String markdown2Html(String text, Boolean escape) {
        List<Extension> extensions = Collections.singletonList(AutolinkExtension.create());

        Parser parser = Parser
                .builder()
                .extensions(extensions)
                .build();

        Node document = parser.parse(text);
        HtmlRenderer renderer = HtmlRenderer
                .builder()
                .escapeHtml(escape)
                .extensions(extensions)
                .build();
        return renderer.render(document);
    }
}

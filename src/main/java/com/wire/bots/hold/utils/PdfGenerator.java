package com.wire.bots.hold.utils;

import com.openhtmltopdf.extend.FSSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.XRLog;

import java.io.*;

public class PdfGenerator {
    private static final String[] fonts = new String[]{
            "Arial-Unicode.ttf",
            "Arial-Bold.ttf"
    };
    private static final String FONT_FAMILY = "Arial";
    private static final PdfRendererBuilder builder;

    static {
        XRLog.setLoggingEnabled(false);
        builder = new PdfRendererBuilder().useSVGDrawer(new BatikSVGDrawer());
        for (String font : fonts) {
            builder.useFont(new FSSupplier<InputStream>() {
                @Override
                public InputStream supply() {
                    return getClass().getClassLoader().getResourceAsStream(String.format("fonts/%s", font));
                }
            }, FONT_FAMILY);
        }
    }

    public static byte[] convert(String html, String baseUri) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            build(html, baseUri, out);
            return out.toByteArray();
        }
    }

    public static File save(String filename, String html, String baseUri) throws Exception {
        File file = new File(filename);
        try (OutputStream out = new FileOutputStream(file)) {
            build(html, baseUri, out);
        }
        return file;
    }

    private static void build(String html, String baseUri, OutputStream out) throws Exception {
        builder
                .useUriResolver((bu, uri) -> {
                    if (uri.contains(":"))
                        return uri.contains(".") ? uri : null;
                    return bu + uri;
                })
                .withHtmlContent(html, baseUri)
                .toStream(out)
                .run();
    }
}

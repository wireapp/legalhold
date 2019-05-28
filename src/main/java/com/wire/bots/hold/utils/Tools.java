package com.wire.bots.hold.utils;

public class Tools {
    public static String hexify(byte bytes[]) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bytes.length; i += 2) {
            buf.append((char) bytes[i]);
            buf.append((char) bytes[i + 1]);
            buf.append(" ");
        }
        return buf.toString().trim();
    }
}

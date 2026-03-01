package com.hypersystems.hyperrewards.util;

import com.hypixel.hytale.server.core.Message;
import java.util.ArrayList;
import java.util.List;

public final class ColorUtil {

    private ColorUtil() {}

    public static Message color(String text) {
        if (text == null || !text.contains("&")) return Message.raw(text != null ? text : "");
        List<Message> parts = new ArrayList<>();
        String[] segments = text.split("(?=&[0-9a-fk-or])");
        for (String seg : segments) {
            if (seg.length() < 2 || seg.charAt(0) != '&') {
                parts.add(Message.raw(seg));
                continue;
            }
            char code = seg.charAt(1);
            String content = seg.substring(2);
            String hex = getHexFromCode(code);
            if (hex != null) parts.add(Message.raw(content).color(hex));
            else parts.add(Message.raw(content));
        }
        return Message.join(parts.toArray(new Message[0]));
    }

    public static String getHexFromCode(char code) {
        return switch (code) {
            case '0' -> "#000000"; case '1' -> "#0000AA"; case '2' -> "#00AA00";
            case '3' -> "#00AAAA"; case '4' -> "#AA0000"; case '5' -> "#AA00AA";
            case '6' -> "#FFAA00"; case '7' -> "#AAAAAA"; case '8' -> "#555555";
            case '9' -> "#5555FF"; case 'a' -> "#55FF55"; case 'b' -> "#55FFFF";
            case 'c' -> "#FF5555"; case 'd' -> "#FF55FF"; case 'e' -> "#FFFF55";
            case 'f' -> "#FFFFFF"; default -> null;
        };
    }
}

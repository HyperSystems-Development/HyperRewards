package com.hypersystems.hyperrewards.util;

import java.util.concurrent.TimeUnit;

public final class TimeUtil {

    private TimeUtil() {}

    public static String format(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return hours + "h " + minutes + "m";
    }

    public static long parseTime(String input) {
        if (input == null || input.isEmpty()) return -1;
        try {
            String number = input.replaceAll("[^0-9]", "");
            String unit = input.replaceAll("[0-9]", "").toLowerCase();
            if (number.isEmpty()) return -1;
            long val = Long.parseLong(number);
            return switch (unit) {
                case "s" -> val * 1000;
                case "m" -> val * 60 * 1000;
                case "h" -> val * 60 * 60 * 1000;
                case "d" -> val * 24 * 60 * 60 * 1000;
                default -> val;
            };
        } catch (Exception e) {
            return -1;
        }
    }
}

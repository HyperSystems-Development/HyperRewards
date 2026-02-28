package com.zib.playtime.milestones;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.zib.playtime.Playtime;
import com.zib.playtime.config.PlaytimeConfig;
import com.zib.playtime.listeners.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Sends periodic rest reminders to players based on continuous session time.
 * Session-based only - no DB tracking needed. Resets on reconnect.
 */
public class RestReminderManager {

    private final Logger logger = LoggerFactory.getLogger("Playtime-RestReminder");

    // Tracks the last reminder time for each player (epoch millis)
    private final ConcurrentHashMap<UUID, Long> lastReminderTime = new ConcurrentHashMap<>();

    @SuppressWarnings("deprecation")
    public void checkReminders() {
        PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
        if (!config.restReminder.enabled) return;

        long interval = config.restReminder.intervalMs;
        if (interval <= 0) return;

        List<PlayerRef> players = new ArrayList<>(Universe.get().getPlayers());
        for (PlayerRef player : players) {
            processPlayer(player, config, interval);
        }

        // Clean up entries for players no longer online
        Set<UUID> onlineUuids = new HashSet<>();
        for (PlayerRef p : players) {
            onlineUuids.add(p.getUuid());
        }
        lastReminderTime.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
    }

    private void processPlayer(PlayerRef player, PlaytimeConfig config, long interval) {
        UUID uuid = player.getUuid();
        long sessionTime = SessionListener.getCurrentSession(uuid);

        if (sessionTime < interval) return;

        long now = System.currentTimeMillis();
        Long lastReminder = lastReminderTime.get(uuid);

        // Send reminder if we haven't sent one yet, or if enough time has passed
        if (lastReminder == null || (now - lastReminder) >= interval) {
            String sessionFormatted = formatTime(sessionTime);
            String msg = config.restReminder.message
                    .replace("%session_time%", sessionFormatted)
                    .replace("%player%", player.getUsername());

            Universe.get().sendMessage(uuid, color(msg));
            lastReminderTime.put(uuid, now);
        }
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return hours + "h " + minutes + "m";
    }

    private Message color(String text) {
        if (!text.contains("&")) return Message.raw(text);
        List<Message> messageParts = new ArrayList<>();
        String[] parts = text.split("(?=&[0-9a-fk-or])");
        for (String part : parts) {
            if (part.length() < 2 || part.charAt(0) != '&') {
                messageParts.add(Message.raw(part));
                continue;
            }
            char code = part.charAt(1);
            String content = part.substring(2);
            String hex = getHexFromCode(code);
            if (hex != null) messageParts.add(Message.raw(content).color(hex));
            else messageParts.add(Message.raw(content));
        }
        return Message.join(messageParts.toArray(new Message[0]));
    }

    private String getHexFromCode(char code) {
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

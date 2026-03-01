package com.hypersystems.hyperrewards.milestones;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypersystems.hyperrewards.HyperRewards;
import com.hypersystems.hyperrewards.config.HyperRewardsConfig;
import com.hypersystems.hyperrewards.listeners.SessionListener;
import com.hypersystems.hyperrewards.util.ColorUtil;
import com.hypersystems.hyperrewards.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends periodic rest reminders to players based on continuous session time.
 * Session-based only - no DB tracking needed. Resets on reconnect.
 */
public class RestReminderManager {

    private final Logger logger = LoggerFactory.getLogger("HyperRewards-RestReminder");

    // Tracks the last reminder time for each player (epoch millis)
    private final ConcurrentHashMap<UUID, Long> lastReminderTime = new ConcurrentHashMap<>();

    @SuppressWarnings("deprecation")
    public void checkReminders() {
        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
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

    private void processPlayer(PlayerRef player, HyperRewardsConfig config, long interval) {
        UUID uuid = player.getUuid();
        long sessionTime = SessionListener.getCurrentSession(uuid);

        if (sessionTime < interval) return;

        long now = System.currentTimeMillis();
        Long lastReminder = lastReminderTime.get(uuid);

        // Send reminder if we haven't sent one yet, or if enough time has passed
        if (lastReminder == null || (now - lastReminder) >= interval) {
            String sessionFormatted = TimeUtil.format(sessionTime);
            String msg = config.restReminder.message
                    .replace("%session_time%", sessionFormatted)
                    .replace("%player%", player.getUsername());

            player.sendMessage(ColorUtil.color(msg));
            lastReminderTime.put(uuid, now);
        }
    }

}

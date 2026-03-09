package com.hypersystems.hyperrewards.listeners;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypersystems.hyperrewards.HyperRewards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SessionListener {

    private static final Logger logger = LoggerFactory.getLogger("HyperRewards-Session");
    private static final ConcurrentHashMap<UUID, Long> joinTimes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> nameCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> historicalCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastSavedDuration = new ConcurrentHashMap<>();
    private static final ExecutorService asyncLoader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "HyperRewards-AsyncLoader");
        t.setDaemon(true);
        return t;
    });

    public static void onJoin(PlayerConnectEvent event) {
        UUID uuid = event.getPlayerRef().getUuid();
        String name = event.getPlayerRef().getUsername();
        long now = System.currentTimeMillis();

        joinTimes.put(uuid, now);
        nameCache.put(uuid, name);
        lastSavedDuration.remove(uuid);

        asyncLoader.submit(() -> {
            try {
                long dbTime = HyperRewards.get().getService().getTotalPlaytime(uuid.toString());
                historicalCache.put(uuid, dbTime);
            } catch (Exception e) {
                logger.error("Failed to load historical playtime for {}", name, e);
            }
        });
    }

    public static void onQuit(PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayerRef().getUuid();
        String name = event.getPlayerRef().getUsername();
        processSessionSave(uuid, name);
    }

    /**
     * Saves all active sessions without clearing state.
     * Called periodically to protect against data loss from crashes.
     */
    public static void saveActiveSessions() {
        for (Map.Entry<UUID, Long> entry : joinTimes.entrySet()) {
            UUID uuid = entry.getKey();
            String name = nameCache.getOrDefault(uuid, "Unknown");
            long start = entry.getValue();
            long currentDuration = System.currentTimeMillis() - start;

            Long previouslySaved = lastSavedDuration.get(uuid);
            long increment = previouslySaved != null ? currentDuration - previouslySaved : currentDuration;

            if (increment < 30_000) {
                continue; // skip if less than 30 seconds of new time
            }

            try {
                HyperRewards.get().getService().saveSession(uuid.toString(), name, start, increment);
                lastSavedDuration.put(uuid, currentDuration);
            } catch (Exception e) {
                logger.error("Failed to save active session for {}", name, e);
            }
        }
    }

    /**
     * Saves all sessions and clears state. Called on shutdown.
     */
    public static void saveAllSessions() {
        for (Map.Entry<UUID, Long> entry : joinTimes.entrySet()) {
            UUID uuid = entry.getKey();
            String name = nameCache.getOrDefault(uuid, "Unknown");
            processSessionSave(uuid, name);
        }
        joinTimes.clear();
        historicalCache.clear();
        nameCache.clear();
        lastSavedDuration.clear();
        shutdownExecutor();
    }

    private static void processSessionSave(UUID uuid, String name) {
        Long start = joinTimes.remove(uuid);
        if (start == null) return;

        long totalDuration = System.currentTimeMillis() - start;
        Long previouslySaved = lastSavedDuration.get(uuid);
        long increment = previouslySaved != null ? totalDuration - previouslySaved : totalDuration;

        if (increment > 0) {
            try {
                HyperRewards.get().getService().saveSession(uuid.toString(), name, start, increment);
            } catch (Exception e) {
                logger.error("Failed to save session for {}", name, e);
            }
        }

        historicalCache.remove(uuid);
        nameCache.remove(uuid);
        lastSavedDuration.remove(uuid);
    }

    public static long getCurrentSession(UUID uuid) {
        Long start = joinTimes.get(uuid);
        if (start == null) return 0;
        return System.currentTimeMillis() - start;
    }

    /**
     * Returns only the unsaved portion of the current session.
     * Used by DB queries to avoid double-counting already-saved increments.
     */
    public static long getUnsavedSessionTime(UUID uuid) {
        Long start = joinTimes.get(uuid);
        if (start == null) return 0;
        long total = System.currentTimeMillis() - start;
        Long saved = lastSavedDuration.get(uuid);
        return saved != null ? total - saved : total;
    }

    public static long getLiveTotalTime(UUID uuid) {
        long history = historicalCache.getOrDefault(uuid, 0L);
        long current = getCurrentSession(uuid);
        return history + current;
    }

    private static void shutdownExecutor() {
        asyncLoader.shutdown();
        try {
            if (!asyncLoader.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncLoader.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncLoader.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

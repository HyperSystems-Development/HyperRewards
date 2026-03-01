package com.hypersystems.hyperrewards.api;

import com.hypersystems.hyperrewards.BuildInfo;
import com.hypersystems.hyperrewards.HyperRewards;
import com.hypersystems.hyperrewards.HyperRewardsService;
import com.hypersystems.hyperrewards.config.Milestone;
import com.hypersystems.hyperrewards.listeners.SessionListener;
import com.hypersystems.hyperrewards.util.TimeUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HyperRewardsAPI {

    private static volatile HyperRewardsAPI instance;
    private final HyperRewardsService service;

    private HyperRewardsAPI() {
        this.service = HyperRewards.get().getService();
    }

    /**
     * Get the singleton instance of the API.
     * @return HyperRewardsAPI instance.
     */
    public static HyperRewardsAPI get() {
        if (instance == null) {
            instance = new HyperRewardsAPI();
        }
        return instance;
    }

    /**
     * Get the total playtime of a player in milliseconds.
     * This includes the current live session if the player is online.
     * * @param uuid The UUID of the player.
     * @return Total playtime in ms.
     */
    public long getTotalPlaytime(UUID uuid) {
        return service.getTotalPlaytime(uuid.toString());
    }

    /**
     * Get playtime for a specific period.
     * This includes the current live session if the player is online.
     * * @param uuid The UUID of the player.
     * @param period The period: "daily", "weekly", "monthly", "all".
     * @return Playtime in ms.
     */
    public long getPlaytime(UUID uuid, String period) {
        return service.getPlaytime(uuid.toString(), period);
    }

    /**
     * Get the rank of a player for a specific period.
     * * @param uuid The UUID of the player.
     * @param period The period: "daily", "weekly", "monthly", "all".
     * @return The rank (1-based), or 0 if not ranked.
     */
    public int getRank(UUID uuid, String period) {
        return service.getRank(uuid.toString(), period);
    }

    /**
     * Get the top 10 players for a specific period.
     * * @param period The period: "daily", "weekly", "monthly", "all".
     * @return A map of Username -> Playtime (ms).
     */
    public Map<String, Long> getTopPlayers(String period) {
        return service.getTopPlayers(period);
    }

    /**
     * Get the current live session duration for an online player.
     * @param uuid The UUID of the player.
     * @return Current session time in ms, or 0 if not online.
     */
    public long getCurrentSessionTime(UUID uuid) {
        return SessionListener.getCurrentSession(uuid);
    }

    /**
     * Get the list of configured milestones.
     * @return Unmodifiable list of milestones.
     */
    public List<Milestone> getMilestones() {
        return Collections.unmodifiableList(
                HyperRewards.get().getConfigManager().getConfig().milestones.list
        );
    }

    /**
     * Check if a player has claimed a specific milestone.
     * @param uuid The UUID of the player.
     * @param milestoneId The milestone ID.
     * @return true if the milestone has been claimed.
     */
    public boolean hasMilestoneClaimed(UUID uuid, String milestoneId) {
        return HyperRewards.get().getDatabaseManager().hasMilestoneClaimed(uuid.toString(), milestoneId);
    }

    /**
     * Get the plugin version.
     * @return Version string from BuildInfo.
     */
    public String getVersion() {
        return BuildInfo.VERSION;
    }

    /**
     * Utility: Format milliseconds into a readable string (e.g. "2h 5m").
     * @param millis Time in milliseconds.
     * @return Formatted string.
     */
    public String formatTime(long millis) {
        return TimeUtil.format(millis);
    }
}

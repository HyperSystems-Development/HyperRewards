package com.hypersystems.hyperrewards.milestones;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypersystems.hyperrewards.HyperRewards;
import com.hypersystems.hyperrewards.api.HyperRewardsAPI;
import com.hypersystems.hyperrewards.config.Milestone;
import com.hypersystems.hyperrewards.config.HyperRewardsConfig;
import com.hypersystems.hyperrewards.database.DatabaseManager;
import com.hypersystems.hyperrewards.integration.HyperPermsIntegration;
import com.hypersystems.hyperrewards.util.ColorUtil;
import com.hypersystems.hyperrewards.util.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MilestoneManager {

    private final DatabaseManager db;
    private final Logger logger = LoggerFactory.getLogger("HyperRewards-Milestones");

    public MilestoneManager(DatabaseManager db) {
        this.db = db;
    }

    @SuppressWarnings("deprecation")
    public void checkMilestones() {
        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
        if (!config.milestones.enabled || config.milestones.list.isEmpty()) return;

        List<PlayerRef> players = new ArrayList<>(Universe.get().getPlayers());
        for (PlayerRef player : players) {
            processPlayer(player, config);
        }
    }

    private void processPlayer(PlayerRef player, HyperRewardsConfig config) {
        String uuid = player.getUuid().toString();

        for (Milestone milestone : config.milestones.list) {
            long playtime = HyperRewardsAPI.get().getPlaytime(player.getUuid(), milestone.period);

            if (playtime >= milestone.timeRequirement) {
                if (milestone.repeatable) {
                    // For repeatable milestones, check if claimed within the current period
                    if (!db.hasMilestoneClaimedInPeriod(uuid, milestone.id, milestone.period)) {
                        triggerMilestone(player, milestone);
                    }
                } else {
                    // One-time milestones: check if ever claimed
                    if (!db.hasMilestoneClaimed(uuid, milestone.id)) {
                        triggerMilestone(player, milestone);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void triggerMilestone(PlayerRef player, Milestone milestone) {
        String uuid = player.getUuid().toString();
        String username = player.getUsername();
        String timeFormatted = HyperRewardsAPI.get().formatTime(milestone.timeRequirement);

        logger.info("Player {} reached milestone [{}]", username, milestone.id);

        // 1. Log to database
        db.logMilestoneClaim(uuid, milestone.id);

        // 2. Grant permissions via HyperPerms
        if (milestone.grantPermissions != null && HyperPermsIntegration.isAvailable()) {
            for (String perm : milestone.grantPermissions) {
                HyperPermsIntegration.setPermission(player.getUuid(), perm);
            }
        }

        // 3. Add to HyperPerms group
        if (milestone.addToGroup != null && !milestone.addToGroup.isEmpty() && HyperPermsIntegration.isAvailable()) {
            HyperPermsIntegration.addToGroup(player.getUuid(), milestone.addToGroup);
        }

        // 4. Execute console commands
        if (milestone.commands != null && !milestone.commands.isEmpty()) {
            CommandUtil.executeAsConsole(player, milestone.commands, logger);
        }

        // 5. Send private message
        if (milestone.privateMessage != null && !milestone.privateMessage.isEmpty()) {
            String msg = milestone.privateMessage
                    .replace("%player%", username)
                    .replace("%time%", timeFormatted)
                    .replace("%milestone%", milestone.id);
            Universe.get().sendMessage(player.getUuid(), ColorUtil.color(msg));
        }

        // 6. Broadcast announcement
        if (milestone.broadcastMessage != null && !milestone.broadcastMessage.isEmpty()) {
            String msg = milestone.broadcastMessage
                    .replace("%player%", username)
                    .replace("%time%", timeFormatted)
                    .replace("%milestone%", milestone.id);
            Universe.get().sendMessage(ColorUtil.color(msg));
        }
    }

}

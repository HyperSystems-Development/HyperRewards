package com.hypersystems.hyperrewards.rewards;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypersystems.hyperrewards.HyperRewards;
import com.hypersystems.hyperrewards.api.HyperRewardsAPI;
import com.hypersystems.hyperrewards.config.HyperRewardsConfig;
import com.hypersystems.hyperrewards.config.Reward;
import com.hypersystems.hyperrewards.database.DatabaseManager;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypersystems.hyperrewards.util.ColorUtil;
import com.hypersystems.hyperrewards.util.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RewardManager {

    private final DatabaseManager db;
    private final Logger logger = LoggerFactory.getLogger("HyperRewards-Rewards");

    public RewardManager(DatabaseManager db) {
        this.db = db;
    }

    @SuppressWarnings("deprecation")
    public void checkRewards() {
        List<PlayerRef> players = new ArrayList<>(Universe.get().getPlayers());
        for (PlayerRef player : players) {
            processPlayer(player);
        }
    }

    @SuppressWarnings("deprecation")
    private void processPlayer(PlayerRef player) {
        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
        String uuid = player.getUuid().toString();

        for (Reward reward : config.rewards) {
            // Per-world filter: skip if reward is world-specific and player isn't in that world
            if (reward.world != null && !reward.world.isEmpty()) {
                UUID worldUuid = player.getWorldUuid();
                if (worldUuid == null) continue;
                World playerWorld = Universe.get().getWorld(worldUuid);
                if (playerWorld == null) continue;
                if (!reward.world.equalsIgnoreCase(playerWorld.getName())) continue;
            }

            long playTime = HyperRewardsAPI.get().getPlaytime(player.getUuid(), reward.period);

            if (playTime >= reward.timeRequirement) {
                if (!db.hasClaimedReward(uuid, reward)) {
                    giveReward(player, reward);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void giveReward(PlayerRef player, Reward reward) {
        // 1. Log claim to database
        db.logRewardClaim(player.getUuid().toString(), reward.id);

        final String username = player.getUsername();
        logger.info("Granting reward [" + reward.id + "] to " + username);

        // 2. Execute reward commands
        CommandUtil.executeAsConsole(player, reward.commands, logger);

        // 3. Broadcast Message
        if (reward.broadcastMessage != null && !reward.broadcastMessage.isEmpty()) {
            String timeFormatted = HyperRewardsAPI.get().formatTime(reward.timeRequirement);
            String msg = reward.broadcastMessage
                    .replace("%player%", username)
                    .replace("%time%", timeFormatted)
                    .replace("%reward%", reward.id);

            Universe.get().sendMessage(ColorUtil.color(msg));
        }
    }

    public boolean isClaimed(String uuid, Reward reward) {
        return db.hasClaimedReward(uuid, reward);
    }

}

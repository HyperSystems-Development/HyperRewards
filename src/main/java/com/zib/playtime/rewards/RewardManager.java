package com.zib.playtime.rewards;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.zib.playtime.Playtime;
import com.zib.playtime.api.PlaytimeAPI;
import com.zib.playtime.config.PlaytimeConfig;
import com.zib.playtime.config.Reward;
import com.zib.playtime.database.DatabaseManager;
import com.zib.playtime.util.ColorUtil;
import com.zib.playtime.util.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RewardManager {

    private final DatabaseManager db;
    private final Logger logger = LoggerFactory.getLogger("Playtime-Rewards");

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
        PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
        String uuid = player.getUuid().toString();

        for (Reward reward : config.rewards) {
            long playTime = PlaytimeAPI.get().getPlaytime(player.getUuid(), reward.period);

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
            String timeFormatted = PlaytimeAPI.get().formatTime(reward.timeRequirement);
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
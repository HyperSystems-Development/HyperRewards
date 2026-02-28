package com.zib.playtime.milestones;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.zib.playtime.Playtime;
import com.zib.playtime.api.PlaytimeAPI;
import com.zib.playtime.config.Milestone;
import com.zib.playtime.config.PlaytimeConfig;
import com.zib.playtime.database.DatabaseManager;
import com.zib.playtime.integration.HyperPermsIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MilestoneManager {

    private final DatabaseManager db;
    private final Logger logger = LoggerFactory.getLogger("Playtime-Milestones");

    public MilestoneManager(DatabaseManager db) {
        this.db = db;
    }

    @SuppressWarnings("deprecation")
    public void checkMilestones() {
        PlaytimeConfig config = Playtime.get().getConfigManager().getConfig();
        if (!config.milestones.enabled || config.milestones.list.isEmpty()) return;

        List<PlayerRef> players = new ArrayList<>(Universe.get().getPlayers());
        for (PlayerRef player : players) {
            processPlayer(player, config);
        }
    }

    private void processPlayer(PlayerRef player, PlaytimeConfig config) {
        String uuid = player.getUuid().toString();

        for (Milestone milestone : config.milestones.list) {
            long playtime = PlaytimeAPI.get().getPlaytime(player.getUuid(), milestone.period);

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
        String timeFormatted = PlaytimeAPI.get().formatTime(milestone.timeRequirement);

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
            executeCommands(player, milestone);
        }

        // 5. Send private message
        if (milestone.privateMessage != null && !milestone.privateMessage.isEmpty()) {
            String msg = milestone.privateMessage
                    .replace("%player%", username)
                    .replace("%time%", timeFormatted)
                    .replace("%milestone%", milestone.id);
            Universe.get().sendMessage(player.getUuid(), color(msg));
        }

        // 6. Broadcast announcement
        if (milestone.broadcastMessage != null && !milestone.broadcastMessage.isEmpty()) {
            String msg = milestone.broadcastMessage
                    .replace("%player%", username)
                    .replace("%time%", timeFormatted)
                    .replace("%milestone%", milestone.id);
            Universe.get().sendMessage(color(msg));
        }
    }

    @SuppressWarnings("deprecation")
    private void executeCommands(PlayerRef player, Milestone milestone) {
        World world = Universe.get().getWorld(player.getWorldUuid());
        if (world == null) {
            world = Universe.get().getDefaultWorld();
        }

        if (world != null) {
            final String username = player.getUsername();
            world.execute(() -> {
                CommandManager cm = CommandManager.get();
                CommandSender consoleSender = new CommandSender() {
                    @Override public String getDisplayName() { return "Console"; }
                    @Override public UUID getUuid() { return new UUID(0, 0); }
                    @Override public void sendMessage(Message message) { logger.info("[ConsoleOutput]: {}", message); }
                    @Override public boolean hasPermission(String p) { return true; }
                    @Override public boolean hasPermission(String p, boolean d) { return true; }
                };

                for (String cmd : milestone.commands) {
                    String parsedCmd = cmd.replace("%player%", username).trim();
                    if (parsedCmd.startsWith("/")) parsedCmd = parsedCmd.substring(1);
                    if (parsedCmd.startsWith("\"") && parsedCmd.endsWith("\"")) {
                        parsedCmd = parsedCmd.substring(1, parsedCmd.length() - 1);
                    }

                    try {
                        cm.handleCommand(consoleSender, parsedCmd);
                    } catch (Exception e) {
                        logger.error("Failed to execute milestone command: {}", parsedCmd, e);
                    }
                }
            });
        }
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

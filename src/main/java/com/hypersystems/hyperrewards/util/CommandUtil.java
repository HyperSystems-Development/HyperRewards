package com.hypersystems.hyperrewards.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

public final class CommandUtil {

    private CommandUtil() {}

    @SuppressWarnings("deprecation")
    public static void executeAsConsole(PlayerRef player, List<String> commands, Logger logger) {
        World world = Universe.get().getWorld(player.getWorldUuid());
        if (world == null) {
            world = Universe.get().getDefaultWorld();
        }

        if (world == null) {
            logger.error("Could not find a valid world to execute commands for {}", player.getUsername());
            return;
        }

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

            for (String cmd : commands) {
                String parsedCmd = cmd.replace("%player%", username).trim();
                if (parsedCmd.startsWith("/")) parsedCmd = parsedCmd.substring(1);
                if (parsedCmd.startsWith("\"") && parsedCmd.endsWith("\"")) {
                    parsedCmd = parsedCmd.substring(1, parsedCmd.length() - 1);
                }
                try {
                    cm.handleCommand(consoleSender, parsedCmd);
                } catch (Exception e) {
                    logger.error("Failed to execute command: {}", parsedCmd, e);
                }
            }
        });
    }
}

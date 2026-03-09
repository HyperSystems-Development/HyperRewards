package com.hypersystems.hyperrewards;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypersystems.hyperrewards.config.ConfigManager;
import com.hypersystems.hyperrewards.config.HyperRewardsConfig;
import com.hypersystems.hyperrewards.database.DatabaseManager;
import com.hypersystems.hyperrewards.commands.HyperRewardsCommand;
import com.hypersystems.hyperrewards.integration.HyperPermsIntegration;
import com.hypersystems.hyperrewards.listeners.SessionListener;
import com.hypersystems.hyperrewards.milestones.MilestoneManager;
import com.hypersystems.hyperrewards.milestones.RestReminderManager;
import com.hypersystems.hyperrewards.rewards.RewardManager;
import com.hypersystems.hyperrewards.update.UpdateChecker;
import com.hypersystems.hyperrewards.util.ColorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class HyperRewards extends JavaPlugin {

    private static HyperRewards INSTANCE;
    private static final Logger logger = LoggerFactory.getLogger("HyperRewards");

    private static final String UPDATE_URL = "https://api.github.com/repos/HyperSystems-Development/HyperRewards/releases/latest";

    private ConfigManager configManager;
    private DatabaseManager db;
    private HyperRewardsService service;
    private RewardManager rewardManager;
    private MilestoneManager milestoneManager;
    private RestReminderManager restReminderManager;
    @Nullable private UpdateChecker updateChecker;

    public HyperRewards(@NotNull JavaPluginInit init) {
        super(init);
        INSTANCE = this;
    }

    public static HyperRewards get() { return INSTANCE; }

    @Override
    protected void setup() {
        File dataFolder = new File("mods/HyperRewards");
        if (!dataFolder.exists()) dataFolder.mkdirs();

        configManager = new ConfigManager(dataFolder);
        configManager.init();
        HyperRewardsConfig cfg = configManager.getConfig();

        db = new DatabaseManager(dataFolder);
        db.init();
        service = new HyperRewardsService(db);

        // Init HyperPerms integration (soft dependency - auto-detects via reflection)
        HyperPermsIntegration.init();

        // Init managers
        rewardManager = new RewardManager(db);
        milestoneManager = new MilestoneManager(db);
        restReminderManager = new RestReminderManager();

        String cmdName = cfg.command.name;
        String[] aliases = cfg.command.aliases.toArray(new String[0]);
        this.getCommandRegistry().registerCommand(new HyperRewardsCommand(cmdName, aliases));

        this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, SessionListener::onJoin);
        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, SessionListener::onQuit);

        // Update checker
        if (cfg.updates.enabled) {
            Path modsFolder = new File("mods").toPath();
            updateChecker = new UpdateChecker(BuildInfo.VERSION, UPDATE_URL, modsFolder);
            updateChecker.checkForUpdates(); // async initial check

            // Notify admins on join
            this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, event -> {
                if (updateChecker == null || !updateChecker.hasUpdateAvailable()) return;
                var player = event.getPlayer();
                if (player == null || !player.hasPermission("playtime.admin")) return;

                HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                    UpdateChecker.UpdateInfo info = updateChecker.getCachedUpdate();
                    if (info == null) return;
                    player.sendMessage(ColorUtil.color("&6[HyperRewards] &eA new version is available!"));
                    player.sendMessage(ColorUtil.color("&7Current: &fv" + updateChecker.getCurrentVersion()
                            + " &7-> Latest: &av" + info.version()));
                    player.sendMessage(ColorUtil.color("&7Run &e/playtime update &7to download it."));
                }, 2, TimeUnit.SECONDS);
            });
        }

        // Schedule periodic tasks (rewards, milestones, rest reminders) - every 1 minute
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                SessionListener.saveActiveSessions();
                rewardManager.checkRewards();
                if (cfg.milestones.enabled) {
                    milestoneManager.checkMilestones();
                }
                if (cfg.restReminder.enabled) {
                    restReminderManager.checkReminders();
                }
            } catch (Exception e) {
                logger.error("Error in scheduled task", e);
            }
        }, 1, 1, TimeUnit.MINUTES);

        logger.info("HyperRewards v{} loaded. Command: /{}", BuildInfo.VERSION, cmdName);
        if (HyperPermsIntegration.isAvailable()) {
            logger.info("HyperPerms integration enabled");
        }
    }

    @Override
    protected void shutdown() {
        SessionListener.saveAllSessions();
        if (db != null) db.close();
    }

    public HyperRewardsService getService() { return service; }
    public ConfigManager getConfigManager() { return configManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public MilestoneManager getMilestoneManager() { return milestoneManager; }
    public RestReminderManager getRestReminderManager() { return restReminderManager; }
    public DatabaseManager getDatabaseManager() { return db; }
    @Nullable public UpdateChecker getUpdateChecker() { return updateChecker; }
}

package com.hypersystems.hyperrewards.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HyperRewardsConfig {

    public int configVersion = 2;
    public DatabaseSettings database = new DatabaseSettings();
    public CommandSettings command = new CommandSettings();
    public PeriodSettings periods = new PeriodSettings();
    public MessageSettings messages = new MessageSettings();
    public GuiSettings gui = new GuiSettings();
    public List<Reward> rewards = new ArrayList<>();
    public MilestoneSettings milestones = new MilestoneSettings();
    public RestReminderSettings restReminder = new RestReminderSettings();
    public IntegrationSettings integrations = new IntegrationSettings();

    public void setDefaults() {
        if (database == null) database = new DatabaseSettings();
        if (command == null) command = new CommandSettings();
        if (periods == null) periods = new PeriodSettings();
        if (messages == null) messages = new MessageSettings();
        if (gui == null) gui = new GuiSettings();
        if (rewards == null) rewards = new ArrayList<>();
        if (milestones == null) milestones = new MilestoneSettings();
        if (milestones.list == null) milestones.list = new ArrayList<>();
        if (restReminder == null) restReminder = new RestReminderSettings();
        if (integrations == null) integrations = new IntegrationSettings();

        if (command.topStyle == null) command.topStyle = "text";
        if (command.aliases == null) command.aliases = Arrays.asList("pt", "play", "time");
    }

    public static class DatabaseSettings {
        public String type = "sqlite";
        public String host = "localhost";
        public int port = 3306;
        public String databaseName = "playtime_db";
        public String username = "root";
        public String password = "password";
        public boolean useSSL = false;
    }

    public static class CommandSettings {
        public String name = "playtime";
        public String description = "Check your playtime stats";
        public List<String> aliases = Arrays.asList("pt", "play", "time");
        public String topStyle = "text";
    }

    public static class PeriodSettings {
        public String daily = "daily";
        public String weekly = "weekly";
        public String monthly = "monthly";
        public String all = "all";
        public String reload = "reload";
    }

    public static class MessageSettings {
        public String selfCheck = "&dTotal Playtime: &e%time%";
        public String otherCheck = "&d%player%'s Playtime: &e%time%";

        public String leaderboardHeader = "&6--- Playtime Leaderboard (&e%period_name%&6) ---";
        public String leaderboardEntry = "&6#%rank% &e%player% &7: &f%time%";
        public String leaderboardEmpty = "&7No data available yet.";

        public String reloadSuccess = "&aConfiguration reloaded successfully!";
        public String reloadNoPermission = "&cYou do not have permission to reload.";
        public String reloadFailed = "&cFailed to reload config. Check console.";

        public String errorInvalidPeriod = "&cInvalid period. Use: %valid_periods%";
        public String errorConsole = "&cPlayers only.";
        public String noPermission = "&cYou do not have permission to use this command.";

        public String rewardAdded = "&aReward '%id%' added successfully!";
        public String rewardRemoved = "&aReward '%id%' removed successfully!";
        public String rewardNotFound = "&cReward '%id%' not found.";
        public String rewardBroadcast = "&6%player% &ehas played for &6%time% &eand claimed the &6%reward% &ereward!";

        public String rewardListHeader = "&6--- Server Rewards ---";
        public String rewardListEntry = "&e%id% &7(%period%): &f%status%";
        public String statusClaimed = "&a[CLAIMED]";
        public String statusAvailable = "&e[AVAILABLE]";
        public String statusLocked = "&c[LOCKED]";

        // Milestone messages
        public String milestoneAnnouncement = "&6%player% &ehas reached the &6%milestone% &emilestone! (&6%time%&e played)";
        public String milestonePrivate = "&a[Milestone] &7Congratulations! You reached the &e%milestone% &7milestone!";
        public String permissionGranted = "&a[HyperRewards] &7You've been granted permission: &e%permission%";
        public String groupAssigned = "&a[HyperRewards] &7You've been added to the &e%group% &7group!";

        // Rest reminder
        public String restReminderMsg = "&e[HyperRewards] &7You've been playing for &e%session_time%&7. Consider taking a break!";
    }

    public static class GuiSettings {
        public String title = "LEADERBOARD";
        public String buttonAll = "ALL TIME";
        public String buttonDaily = "DAILY";
        public String buttonWeekly = "WEEKLY";
        public String buttonMonthly = "MONTHLY";
        public String footerTitle = "YOUR STATS:";
        public String rankPrefix = "Rank: #";
        public String timePrefix = "Time: ";
    }

    public static class MilestoneSettings {
        public boolean enabled = true;
        public List<Milestone> list = new ArrayList<>();
    }

    public static class RestReminderSettings {
        public boolean enabled = true;
        public long intervalMs = 7200000; // 2 hours default
        public String message = "&e[HyperRewards] &7You've been playing for &e%session_time%&7. Consider taking a break!";
    }

    public static class IntegrationSettings {
        public boolean hyperPermsEnabled = true;
    }
}

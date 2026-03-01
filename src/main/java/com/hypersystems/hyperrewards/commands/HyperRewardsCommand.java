package com.hypersystems.hyperrewards.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypersystems.hyperrewards.BuildInfo;
import com.hypersystems.hyperrewards.HyperRewards;
import com.hypersystems.hyperrewards.api.HyperRewardsAPI;
import com.hypersystems.hyperrewards.config.Milestone;
import com.hypersystems.hyperrewards.config.HyperRewardsConfig;
import com.hypersystems.hyperrewards.config.Reward;
import com.hypersystems.hyperrewards.gui.LeaderboardGui;
import com.hypersystems.hyperrewards.integration.HyperPermsIntegration;
import com.hypersystems.hyperrewards.util.ColorUtil;
import com.hypersystems.hyperrewards.util.TimeUtil;

import java.util.*;

public class HyperRewardsCommand extends AbstractPlayerCommand {

    public HyperRewardsCommand(String name, String... aliases) {
        super(name, HyperRewards.get().getConfigManager().getConfig().command.description);

        if (aliases != null && aliases.length > 0) {
            this.addAliases(aliases);
        }

        addUsageVariant(new ActionCommand(name));
        addUsageVariant(new DoubleArgCommand(name));
        addUsageVariant(new RemoveRewardCommand(name));
        addUsageVariant(new AdminRewardCommand(name));
    }

    @Override
    protected boolean canGeneratePermission() { return false; }

    @Override
    protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref,
                           PlayerRef player, World world) {
        if (!ctx.sender().hasPermission("playtime.check")) {
            ctx.sendMessage(ColorUtil.color(HyperRewards.get().getConfigManager().getConfig().messages.noPermission));
            return;
        }

        long total = HyperRewards.get().getService().getTotalPlaytime(player.getUuid().toString());
        String msg = HyperRewards.get().getConfigManager().getConfig().messages.selfCheck
                .replace("%time%", TimeUtil.format(total))
                .replace("%player%", player.getUsername());
        ctx.sendMessage(ColorUtil.color(msg));
    }

    private static void showTop(CommandContext ctx, PlayerRef player, String periodArg) {
        if (!ctx.sender().hasPermission("playtime.top")) {
            ctx.sendMessage(ColorUtil.color(HyperRewards.get().getConfigManager().getConfig().messages.noPermission));
            return;
        }

        HyperRewardsConfig cfg = HyperRewards.get().getConfigManager().getConfig();
        HyperRewardsConfig.PeriodSettings p = cfg.periods;
        String mode = null;

        if (periodArg.equalsIgnoreCase(p.daily)) mode = "daily";
        else if (periodArg.equalsIgnoreCase(p.weekly)) mode = "weekly";
        else if (periodArg.equalsIgnoreCase(p.monthly)) mode = "monthly";
        else if (periodArg.equalsIgnoreCase(p.all)) mode = "all";

        if (mode == null) {
            String valid = String.join(", ", p.daily, p.weekly, p.monthly, p.all);
            ctx.sendMessage(ColorUtil.color(cfg.messages.errorInvalidPeriod.replace("%valid_periods%", valid)));
            return;
        }

        Map<String, Long> sorted = HyperRewards.get().getService().getTopPlayers(mode);
        ctx.sendMessage(ColorUtil.color(cfg.messages.leaderboardHeader.replace("%period_name%", periodArg)));

        if (sorted.isEmpty()) {
            ctx.sendMessage(ColorUtil.color(cfg.messages.leaderboardEmpty));
            return;
        }

        int rank = 1;
        for (Map.Entry<String, Long> entry : sorted.entrySet()) {
            String line = cfg.messages.leaderboardEntry
                    .replace("%rank%", String.valueOf(rank))
                    .replace("%player%", entry.getKey())
                    .replace("%time%", TimeUtil.format(entry.getValue()));
            ctx.sendMessage(ColorUtil.color(line));
            rank++;
        }
    }

    private static class ActionCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;

        public ActionCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("action", "Action", ArgTypes.STRING);
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            String arg = ctx.get(arg1);
            HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
            HyperRewardsConfig.PeriodSettings periods = config.periods;

            if (arg.equalsIgnoreCase("help")) {
                showHelp(ctx);
                return;
            }
            if (arg.equalsIgnoreCase("version")) {
                showVersion(ctx);
                return;
            }
            if (arg.equalsIgnoreCase("milestones")) {
                showMilestones(ctx, player);
                return;
            }
            if (arg.equalsIgnoreCase("rewards")) {
                listUserRewards(ctx, player, config);
                return;
            }
            if (arg.equalsIgnoreCase("admin")) {
                if (!ctx.sender().hasPermission("playtime.admin")) {
                    ctx.sendMessage(ColorUtil.color(config.messages.noPermission));
                    return;
                }
                showAdminGuide(ctx);
                return;
            }
            if (arg.equalsIgnoreCase("menu") || arg.equalsIgnoreCase("gui")) {
                if (!ctx.sender().hasPermission("playtime.gui")) {
                    ctx.sendMessage(ColorUtil.color(config.messages.noPermission));
                    return;
                }
                if (ctx.sender() instanceof Player senderPlayer) {
                    senderPlayer.getPageManager().openCustomPage(ref, store, new LeaderboardGui(player));
                }
                return;
            }
            if (arg.equalsIgnoreCase(periods.reload)) {
                if (!ctx.sender().hasPermission("playtime.reload")) {
                    ctx.sendMessage(ColorUtil.color(config.messages.reloadNoPermission));
                    return;
                }
                try {
                    HyperRewards.get().getConfigManager().load();
                    ctx.sendMessage(ColorUtil.color(config.messages.reloadSuccess));
                } catch (Exception e) {
                    ctx.sendMessage(ColorUtil.color(config.messages.reloadFailed));
                    e.printStackTrace();
                }
                return;
            }
            if (arg.equalsIgnoreCase("top")) {
                if (config.command.topStyle.equalsIgnoreCase("gui")) {
                    if (ctx.sender() instanceof Player senderPlayer) {
                        senderPlayer.getPageManager().openCustomPage(ref, store, new LeaderboardGui(player));
                    }
                } else {
                    showTop(ctx, player, periods.all);
                }
                return;
            }
            if (arg.equalsIgnoreCase(periods.daily) || arg.equalsIgnoreCase(periods.weekly) ||
                    arg.equalsIgnoreCase(periods.monthly) || arg.equalsIgnoreCase(periods.all)) {
                showTop(ctx, player, arg);
                return;
            }

            ctx.sendMessage(ColorUtil.color("&cUnknown command. Try /playtime help"));
        }

        private void showHelp(CommandContext ctx) {
            ctx.sendMessage(ColorUtil.color("&6--- HyperRewards Help ---"));
            ctx.sendMessage(ColorUtil.color("&e/playtime &7- Check your playtime"));
            ctx.sendMessage(ColorUtil.color("&e/playtime rewards &7- List your rewards"));
            ctx.sendMessage(ColorUtil.color("&e/playtime check <player> &7- View another player's playtime"));
            ctx.sendMessage(ColorUtil.color("&e/playtime milestones &7- View milestones progress"));
            ctx.sendMessage(ColorUtil.color("&e/playtime top [period] &7- Check leaderboard"));
            ctx.sendMessage(ColorUtil.color("&e/playtime version &7- Show plugin version info"));
            if (ctx.sender().hasPermission("playtime.admin")) {
                ctx.sendMessage(ColorUtil.color("&c--- Admin ---"));
                ctx.sendMessage(ColorUtil.color("&c/playtime admin &7- Show reward creation guide"));
                ctx.sendMessage(ColorUtil.color("&c/playtime admin listRewards &7- List configured rewards"));
                ctx.sendMessage(ColorUtil.color("&c/playtime admin listMilestones &7- List configured milestones"));
                ctx.sendMessage(ColorUtil.color("&c/playtime admin addReward &7- Add a new reward"));
                ctx.sendMessage(ColorUtil.color("&c/playtime admin removeReward <id> &7- Remove a reward"));
                ctx.sendMessage(ColorUtil.color("&c/playtime reload &7- Reload config"));
            }
        }

        private void showVersion(CommandContext ctx) {
            ctx.sendMessage(ColorUtil.color("&6--- HyperRewards Info ---"));
            ctx.sendMessage(ColorUtil.color("&eVersion: &f" + BuildInfo.VERSION));
            ctx.sendMessage(ColorUtil.color("&eJava: &f" + BuildInfo.JAVA_VERSION));
            ctx.sendMessage(ColorUtil.color("&eHyperPerms: &f" + (HyperPermsIntegration.isAvailable() ? "Connected" : "Not available")));
        }

        private void showMilestones(CommandContext ctx, PlayerRef player) {
            if (!ctx.sender().hasPermission("playtime.milestones")) {
                ctx.sendMessage(ColorUtil.color(HyperRewards.get().getConfigManager().getConfig().messages.noPermission));
                return;
            }

            HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
            if (!config.milestones.enabled || config.milestones.list.isEmpty()) {
                ctx.sendMessage(ColorUtil.color("&7No milestones configured."));
                return;
            }

            ctx.sendMessage(ColorUtil.color("&6--- Milestones ---"));
            String uuid = player.getUuid().toString();

            for (Milestone m : config.milestones.list) {
                boolean claimed = HyperRewards.get().getDatabaseManager().hasMilestoneClaimed(uuid, m.id);
                long playtime = HyperRewardsAPI.get().getPlaytime(player.getUuid(), m.period);
                boolean eligible = playtime >= m.timeRequirement;

                String status;
                if (claimed) {
                    status = "&a[COMPLETED]";
                } else if (eligible) {
                    status = "&e[READY]";
                } else {
                    status = "&c[" + TimeUtil.format(playtime) + " / " + TimeUtil.format(m.timeRequirement) + "]";
                }

                ctx.sendMessage(ColorUtil.color("&e" + m.id + " &7(" + m.period + "): " + status));
            }
        }

        private void showAdminGuide(CommandContext ctx) {
            ctx.sendMessage(ColorUtil.color("&6--- HyperRewards Reward Guide ---"));
            ctx.sendMessage(ColorUtil.color("&eHow to add a reward:"));
            ctx.sendMessage(ColorUtil.color("&f/playtime admin addReward <id> <period> <time> <command>"));
            ctx.sendMessage(ColorUtil.color("&7- &eid&7: Unique name (e.g. daily_gold)"));
            ctx.sendMessage(ColorUtil.color("&7- &eperiod&7: daily, weekly, monthly, or all"));
            ctx.sendMessage(ColorUtil.color("&7- &etime&7: 30m, 1h, 1d, 10s"));
            ctx.sendMessage(ColorUtil.color("&7- &ecommand&7: The console command. Use &f%player% &7for username."));
            ctx.sendMessage(ColorUtil.color("&7  Example: &f/playtime admin addReward daily_gold daily 1h \"give %player% gold 10\""));
        }

        private void listUserRewards(CommandContext ctx, PlayerRef player, HyperRewardsConfig cfg) {
            ctx.sendMessage(ColorUtil.color(cfg.messages.rewardListHeader));

            String uuid = player.getUuid().toString();

            for (Reward r : cfg.rewards) {
                // FIXED: Check claim status using RewardManager
                boolean claimed = HyperRewards.get().getRewardManager().isClaimed(uuid, r);
                long playtime = HyperRewardsAPI.get().getPlaytime(player.getUuid(), r.period);
                boolean eligible = playtime >= r.timeRequirement;

                String status;
                if (claimed) {
                    status = cfg.messages.statusClaimed;
                } else if (eligible) {
                    status = cfg.messages.statusAvailable;
                } else {
                    status = cfg.messages.statusLocked;
                }

                String line = cfg.messages.rewardListEntry
                        .replace("%id%", r.id)
                        .replace("%period%", r.period)
                        .replace("%status%", status + " &7(" + TimeUtil.format(r.timeRequirement) + ")");
                ctx.sendMessage(ColorUtil.color(line));
            }
        }
    }

    private static class DoubleArgCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;
        private final RequiredArg<String> arg2;

        public DoubleArgCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("arg1", "First Argument", ArgTypes.STRING);
            this.arg2 = withRequiredArg("arg2", "Second Argument", ArgTypes.STRING);
        }
        @Override protected boolean canGeneratePermission() { return false; }
        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            String a1 = ctx.get(arg1);
            String a2 = ctx.get(arg2);

            if (a1.equalsIgnoreCase("check")) {
                if (!ctx.sender().hasPermission("playtime.check.others")) {
                    ctx.sendMessage(ColorUtil.color(HyperRewards.get().getConfigManager().getConfig().messages.noPermission));
                    return;
                }
                String targetName = a2;
                String targetUuid = HyperRewards.get().getService().getUuidByUsername(targetName);
                if (targetUuid == null) {
                    ctx.sendMessage(ColorUtil.color("&cPlayer '" + targetName + "' not found."));
                    return;
                }
                long total = HyperRewards.get().getService().getTotalPlaytime(targetUuid);
                HyperRewardsConfig cfg = HyperRewards.get().getConfigManager().getConfig();
                String msg = cfg.messages.otherCheck
                        .replace("%player%", targetName)
                        .replace("%time%", TimeUtil.format(total));
                ctx.sendMessage(ColorUtil.color(msg));
                return;
            }

            if (a1.equalsIgnoreCase("top")) {
                showTop(ctx, player, a2.toLowerCase());
                return;
            }

            if (a1.equalsIgnoreCase("admin") && a2.equalsIgnoreCase("listRewards")) {
                if (!ctx.sender().hasPermission("playtime.admin")) {
                    ctx.sendMessage(ColorUtil.color("&cNo permission.")); return;
                }
                ctx.sendMessage(ColorUtil.color("&6--- Configured Rewards (Admin) ---"));
                HyperRewardsConfig cfg = HyperRewards.get().getConfigManager().getConfig();
                for (Reward r : cfg.rewards) {
                    ctx.sendMessage(ColorUtil.color("&eID: &f" + r.id));
                    ctx.sendMessage(ColorUtil.color("  &7Period: " + r.period));
                    ctx.sendMessage(ColorUtil.color("  &7Time: " + TimeUtil.format(r.timeRequirement)));
                    ctx.sendMessage(ColorUtil.color("  &7Cmd: " + (r.commands.isEmpty() ? "None" : r.commands.get(0))));
                }
                return;
            }

            if (a1.equalsIgnoreCase("admin") && a2.equalsIgnoreCase("listMilestones")) {
                if (!ctx.sender().hasPermission("playtime.admin")) {
                    ctx.sendMessage(ColorUtil.color("&cNo permission.")); return;
                }
                HyperRewardsConfig cfg = HyperRewards.get().getConfigManager().getConfig();
                ctx.sendMessage(ColorUtil.color("&6--- Configured Milestones (Admin) ---"));
                ctx.sendMessage(ColorUtil.color("&7Milestones enabled: &f" + cfg.milestones.enabled));
                if (cfg.milestones.list.isEmpty()) {
                    ctx.sendMessage(ColorUtil.color("&7No milestones configured."));
                } else {
                    for (Milestone m : cfg.milestones.list) {
                        ctx.sendMessage(ColorUtil.color("&eID: &f" + m.id));
                        ctx.sendMessage(ColorUtil.color("  &7Period: " + m.period));
                        ctx.sendMessage(ColorUtil.color("  &7Time: " + TimeUtil.format(m.timeRequirement)));
                        ctx.sendMessage(ColorUtil.color("  &7Permissions: " + (m.grantPermissions == null || m.grantPermissions.isEmpty() ? "None" : String.join(", ", m.grantPermissions))));
                        ctx.sendMessage(ColorUtil.color("  &7Group: " + (m.addToGroup == null ? "None" : m.addToGroup)));
                        ctx.sendMessage(ColorUtil.color("  &7Repeatable: " + m.repeatable));
                    }
                }
                return;
            }

            ctx.sendMessage(ColorUtil.color("&cUnknown command. Usage: /playtime <action> [arg]"));
        }
    }

    private static class RemoveRewardCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;
        private final RequiredArg<String> arg2;
        private final RequiredArg<String> idArg;

        public RemoveRewardCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("admin", "Admin", ArgTypes.STRING);
            this.arg2 = withRequiredArg("action", "Remove Reward", ArgTypes.STRING);
            this.idArg = withRequiredArg("id", "Reward ID", ArgTypes.STRING);
        }
        @Override protected boolean canGeneratePermission() { return false; }
        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            if (!ctx.get(arg1).equalsIgnoreCase("admin")) return;
            if (!ctx.get(arg2).equalsIgnoreCase("removeReward")) return;

            if (!ctx.sender().hasPermission("playtime.admin")) {
                ctx.sendMessage(ColorUtil.color("&cNo permission.")); return;
            }

            String id = ctx.get(idArg);
            HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();

            boolean removed = config.rewards.removeIf(r -> r.id.equals(id));

            if (removed) {
                HyperRewards.get().getConfigManager().save();
                ctx.sendMessage(ColorUtil.color(config.messages.rewardRemoved.replace("%id%", id)));
            } else {
                ctx.sendMessage(ColorUtil.color(config.messages.rewardNotFound.replace("%id%", id)));
            }
        }
    }

    private static class AdminRewardCommand extends AbstractPlayerCommand {
        private final RequiredArg<String> arg1;
        private final RequiredArg<String> arg2;
        private final RequiredArg<String> idArg;
        private final RequiredArg<String> periodArg;
        private final RequiredArg<String> timeArg;
        private final RequiredArg<String> commandArg;

        public AdminRewardCommand(String name) {
            super(name);
            this.arg1 = withRequiredArg("admin", "Admin", ArgTypes.STRING);
            this.arg2 = withRequiredArg("action", "Add", ArgTypes.STRING);
            this.idArg = withRequiredArg("id", "ID", ArgTypes.STRING);
            this.periodArg = withRequiredArg("period", "Period", ArgTypes.STRING);
            this.timeArg = withRequiredArg("time", "Time", ArgTypes.STRING);
            this.commandArg = withRequiredArg("command", "Command", ArgTypes.STRING);
        }

        @Override protected boolean canGeneratePermission() { return false; }

        @Override
        protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef player, World world) {
            if (!ctx.sender().hasPermission("playtime.admin")) {
                ctx.sendMessage(ColorUtil.color("&cYou do not have permission."));
                return;
            }

            if (!ctx.get(arg1).equalsIgnoreCase("admin") || !ctx.get(arg2).equalsIgnoreCase("addReward")) {
                return;
            }

            String id = ctx.get(idArg);
            String period = ctx.get(periodArg).toLowerCase();
            String timeStr = ctx.get(timeArg);
            String cmdToRun = ctx.get(commandArg);

            long ms = TimeUtil.parseTime(timeStr);
            if (ms <= 0) {
                ctx.sendMessage(ColorUtil.color("&cInvalid time format. Use 30m, 1h, 1d."));
                return;
            }

            List<String> cmds = new ArrayList<>();
            cmds.add(cmdToRun);

            String broadcast = "&6%player% &ehas played for &6%time% &eand claimed the &6" + id + " &ereward!";
            Reward newReward = new Reward(id, period, ms, cmds, broadcast);

            HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
            config.rewards.add(newReward);
            HyperRewards.get().getConfigManager().save();

            ctx.sendMessage(ColorUtil.color(config.messages.rewardAdded.replace("%id%", id)));
        }

    }
}

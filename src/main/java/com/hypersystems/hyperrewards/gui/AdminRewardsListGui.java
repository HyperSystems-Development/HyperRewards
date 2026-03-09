package com.hypersystems.hyperrewards.gui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypersystems.hyperrewards.HyperRewards;
import com.hypersystems.hyperrewards.config.HyperRewardsConfig;
import com.hypersystems.hyperrewards.config.Reward;
import com.hypersystems.hyperrewards.util.ColorUtil;
import com.hypersystems.hyperrewards.util.TimeUtil;

import javax.annotation.Nonnull;
import java.util.List;

public class AdminRewardsListGui extends InteractiveCustomUIPage<AdminActionData> {

    private final Player player;
    private final PlayerRef playerRef;
    private String currentTab = "all";
    private String pendingDeleteId = null;

    public AdminRewardsListGui(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminActionData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/AdminRewardsList.ui");

        bindButton(events, "#TabAll", "tab_all");
        bindButton(events, "#TabDaily", "tab_daily");
        bindButton(events, "#TabWeekly", "tab_weekly");
        bindButton(events, "#TabMonthly", "tab_monthly");
        bindButton(events, "#BtnAdd", "add");
        bindButton(events, "#BtnBack", "back");

        refreshList(cmd, events);
    }

    private void refreshList(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#TabAll.Disabled", currentTab.equals("all"));
        cmd.set("#TabDaily.Disabled", currentTab.equals("daily"));
        cmd.set("#TabWeekly.Disabled", currentTab.equals("weekly"));
        cmd.set("#TabMonthly.Disabled", currentTab.equals("monthly"));

        cmd.clear("#RewardsList");

        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
        List<Reward> rewards = config.rewards;
        int count = 0;

        for (Reward r : rewards) {
            if (!currentTab.equals("all") && !r.period.equalsIgnoreCase(currentTab)) continue;

            cmd.append("#RewardsList", "Pages/AdminRewardEntry.ui");
            String idx = "#RewardsList[" + count + "]";

            cmd.set(idx + " #RewardId.Text", r.id);
            cmd.set(idx + " #PeriodLabel.Text", r.period);
            cmd.set(idx + " #RepeatBadge.Text", r.repeatable ? "repeatable" : "one-time");
            cmd.set(idx + " #TimeReq.Text", "Requires: " + TimeUtil.format(r.timeRequirement));
            cmd.set(idx + " #CmdCount.Text", r.commands.size() + " command(s)");

            // Delete button shows "Confirm?" if pending
            if (r.id.equals(pendingDeleteId)) {
                cmd.set(idx + " #BtnDelete.Text", "Confirm?");
            }

            if (events != null) {
                events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #BtnEdit",
                        EventData.of("Action", "edit").append("TargetId", r.id), false);
                events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #BtnDelete",
                        EventData.of("Action", "delete").append("TargetId", r.id), false);
            }

            count++;
        }

        cmd.set("#EmptyMsg.Visible", count == 0);
    }

    private void bindButton(UIEventBuilder events, String id, String action) {
        events.addEventBinding(CustomUIEventBindingType.Activating, id, EventData.of("Action", action), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull AdminActionData data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        switch (data.action) {
            case "add" -> GuiHelper.navigate(player, ref, store, new AdminRewardDetailGui(player, playerRef, null));
            case "edit" -> {
                if (data.targetId != null) {
                    GuiHelper.navigate(player, ref, store, new AdminRewardDetailGui(player, playerRef, data.targetId));
                }
            }
            case "delete" -> {
                if (data.targetId == null) return;
                if (data.targetId.equals(pendingDeleteId)) {
                    // Confirmed delete
                    HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
                    boolean removed = config.rewards.removeIf(r -> r.id.equals(data.targetId));
                    if (removed) {
                        HyperRewards.get().getConfigManager().save();
                        player.sendMessage(ColorUtil.color("&aReward '" + data.targetId + "' deleted."));
                    }
                    pendingDeleteId = null;
                    // Rebuild page to refresh list
                    GuiHelper.navigate(player, ref, store, new AdminRewardsListGui(player, playerRef));
                } else {
                    // First click - show confirm
                    pendingDeleteId = data.targetId;
                    UICommandBuilder cmd = new UICommandBuilder();
                    refreshList(cmd, null);
                    this.sendUpdate(cmd);
                }
            }
            case "back" -> GuiHelper.navigate(player, ref, store, new AdminDashboardGui(player, playerRef));
            default -> {
                if (data.action.startsWith("tab_")) {
                    currentTab = data.action.substring(4);
                    pendingDeleteId = null;
                    UICommandBuilder cmd = new UICommandBuilder();
                    refreshList(cmd, null);
                    this.sendUpdate(cmd);
                }
            }
        }
    }
}

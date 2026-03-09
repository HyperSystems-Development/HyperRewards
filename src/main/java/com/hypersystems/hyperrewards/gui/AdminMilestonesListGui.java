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
import com.hypersystems.hyperrewards.config.Milestone;
import com.hypersystems.hyperrewards.util.ColorUtil;
import com.hypersystems.hyperrewards.util.TimeUtil;

import javax.annotation.Nonnull;
import java.util.List;

public class AdminMilestonesListGui extends InteractiveCustomUIPage<AdminActionData> {

    private final Player player;
    private final PlayerRef playerRef;
    private String currentTab = "all";

    public AdminMilestonesListGui(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminActionData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/AdminMilestonesList.ui");

        bindButton(events, "#TabAll", "tab_all");
        bindButton(events, "#TabOneTime", "tab_onetime");
        bindButton(events, "#TabRepeatable", "tab_repeatable");
        bindButton(events, "#BtnAdd", "add");
        bindButton(events, "#BtnBack", "back");

        refreshList(cmd, events);
    }

    private void refreshList(UICommandBuilder cmd, UIEventBuilder events) {
        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();

        // Tab active states
        cmd.set("#TabAll.Disabled", currentTab.equals("all"));
        cmd.set("#TabOneTime.Disabled", currentTab.equals("onetime"));
        cmd.set("#TabRepeatable.Disabled", currentTab.equals("repeatable"));

        cmd.clear("#MilestonesList");

        List<Milestone> milestones = config.milestones.list;
        int count = 0;

        for (Milestone m : milestones) {
            if (currentTab.equals("onetime") && m.repeatable) continue;
            if (currentTab.equals("repeatable") && !m.repeatable) continue;

            cmd.append("#MilestonesList", "Pages/AdminMilestoneEntry.ui");
            String idx = "#MilestonesList[" + count + "]";

            cmd.set(idx + " #MilestoneId.Text", m.id);
            cmd.set(idx + " #PeriodLabel.Text", m.period != null ? m.period : "all");
            cmd.set(idx + " #TimeReq.Text", "Requires: " + TimeUtil.format(m.timeRequirement));
            cmd.set(idx + " #RepeatBadge.Text", m.repeatable ? "REPEATABLE" : "ONE-TIME");

            String groupText = (m.addToGroup != null && !m.addToGroup.isEmpty())
                    ? "Group: " + m.addToGroup : "No group";
            cmd.set(idx + " #GroupLabel.Text", groupText);

            if (events != null) {
                events.addEventBinding(CustomUIEventBindingType.Activating,
                        idx + " #BtnEdit",
                        EventData.of("Action", "edit").append("TargetId", m.id), false);
                events.addEventBinding(CustomUIEventBindingType.Activating,
                        idx + " #BtnDelete",
                        EventData.of("Action", "delete").append("TargetId", m.id), false);
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
            case "back" -> GuiHelper.navigate(player, ref, store, new AdminDashboardGui(player, playerRef));
            case "add" -> GuiHelper.navigate(player, ref, store, new AdminMilestoneDetailGui(player, playerRef, null));
            case "edit" -> {
                if (data.targetId != null) {
                    GuiHelper.navigate(player, ref, store, new AdminMilestoneDetailGui(player, playerRef, data.targetId));
                }
            }
            case "delete" -> {
                if (data.targetId != null) {
                    HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
                    config.milestones.list.removeIf(m -> m.id.equals(data.targetId));
                    HyperRewards.get().getConfigManager().save();
                    player.sendMessage(ColorUtil.color("&aMilestone '" + data.targetId + "' deleted."));

                    UICommandBuilder cmd = new UICommandBuilder();
                    refreshList(cmd, null);
                    this.sendUpdate(cmd);
                }
            }
            default -> {
                if (data.action.startsWith("tab_")) {
                    this.currentTab = data.action.substring(4);
                    UICommandBuilder cmd = new UICommandBuilder();
                    refreshList(cmd, null);
                    this.sendUpdate(cmd);
                }
            }
        }
    }
}

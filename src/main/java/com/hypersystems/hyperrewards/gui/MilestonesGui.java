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
import com.hypersystems.hyperrewards.api.HyperRewardsAPI;
import com.hypersystems.hyperrewards.config.HyperRewardsConfig;
import com.hypersystems.hyperrewards.config.Milestone;
import com.hypersystems.hyperrewards.util.TimeUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class MilestonesGui extends InteractiveCustomUIPage<PageActionData> {

    private final Player player;
    private final PlayerRef playerRef;
    private String currentTab = "all";

    public MilestonesGui(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageActionData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/MilestonesPage.ui");

        HyperRewardsConfig.GuiSettings gui = HyperRewards.get().getConfigManager().getConfig().gui;

        cmd.set("#TitleText.Text", gui.milestonesTitle);
        cmd.set("#BtnBack.Text", gui.backButton);
        cmd.set("#TabAll.Text", gui.tabAll);
        cmd.set("#TabOneTime.Text", gui.tabOneTime);
        cmd.set("#TabRepeatable.Text", gui.tabRepeatable);

        bindButton(events, "#TabAll", "tab_all");
        bindButton(events, "#TabOneTime", "tab_onetime");
        bindButton(events, "#TabRepeatable", "tab_repeatable");
        bindButton(events, "#BtnBack", "back");

        refreshMilestones(cmd);
    }

    private void refreshMilestones(UICommandBuilder cmd) {
        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
        HyperRewardsConfig.GuiSettings gui = config.gui;
        String uuid = playerRef.getUuid().toString();

        // Tab active states
        cmd.set("#TabAll.Disabled", currentTab.equals("all"));
        cmd.set("#TabOneTime.Disabled", currentTab.equals("onetime"));
        cmd.set("#TabRepeatable.Disabled", currentTab.equals("repeatable"));

        cmd.clear("#MilestonesList");

        if (!config.milestones.enabled || config.milestones.list.isEmpty()) {
            cmd.set("#EmptyMsg.Visible", true);
            return;
        }

        List<Milestone> milestones = config.milestones.list;
        int count = 0;

        for (Milestone m : milestones) {
            if (currentTab.equals("onetime") && m.repeatable) continue;
            if (currentTab.equals("repeatable") && !m.repeatable) continue;

            cmd.append("#MilestonesList", "Pages/MilestoneEntry.ui");
            String idx = "#MilestonesList[" + count + "]";

            cmd.set(idx + " #MilestoneName.Text", m.id);
            cmd.set(idx + " #TimeReq.Text", "Requires: " + TimeUtil.format(m.timeRequirement));
            cmd.set(idx + " #PeriodLabel.Text", m.period + (m.repeatable ? " (repeatable)" : ""));

            boolean claimed;
            if (m.repeatable) {
                claimed = HyperRewards.get().getDatabaseManager()
                        .hasMilestoneClaimedInPeriod(uuid, m.id, m.period);
            } else {
                claimed = HyperRewards.get().getDatabaseManager()
                        .hasMilestoneClaimed(uuid, m.id);
            }

            long playtime = HyperRewardsAPI.get().getPlaytime(playerRef.getUuid(), m.period);
            boolean eligible = playtime >= m.timeRequirement;

            if (claimed) {
                cmd.set(idx + " #StatusBadge.Text", gui.milestoneCompleted);
                cmd.set(idx + " #StatusBadge.Style.TextColor", "#55FF55");
                cmd.set(idx + " #IndicatorBar.Background.Color", "#44cc44");
                cmd.set(idx + " #Progress.Value", 1.0f);
                cmd.set(idx + " #Progress.Bar.Color", "#55FF55");
                cmd.set(idx + " #ProgressText.Text", "100%");
            } else if (eligible) {
                cmd.set(idx + " #StatusBadge.Text", gui.milestoneReady);
                cmd.set(idx + " #StatusBadge.Style.TextColor", "#FFD700");
                cmd.set(idx + " #IndicatorBar.Background.Color", "#FFD700");
                cmd.set(idx + " #Progress.Value", 1.0f);
                cmd.set(idx + " #Progress.Bar.Color", "#FFD700");
                cmd.set(idx + " #ProgressText.Text", "Ready!");
            } else {
                cmd.set(idx + " #StatusBadge.Text", gui.milestoneInProgress);
                cmd.set(idx + " #StatusBadge.Style.TextColor", "#888888");
                cmd.set(idx + " #IndicatorBar.Background.Color", "#888888");

                float progress = m.timeRequirement > 0
                        ? Math.min(1.0f, (float) playtime / m.timeRequirement) : 0.0f;
                cmd.set(idx + " #Progress.Value", progress);
                cmd.set(idx + " #ProgressText.Text",
                        TimeUtil.format(playtime) + " / " + TimeUtil.format(m.timeRequirement) +
                                " (" + (int) (progress * 100) + "%)");
            }

            // Rewards preview
            List<String> previews = new ArrayList<>();
            if (m.grantPermissions != null && !m.grantPermissions.isEmpty()) {
                previews.add("Perms: " + String.join(", ", m.grantPermissions));
            }
            if (m.addToGroup != null && !m.addToGroup.isEmpty()) {
                previews.add("Group: " + m.addToGroup);
            }
            if (m.commands != null && !m.commands.isEmpty()) {
                previews.add("Commands: " + m.commands.size());
            }
            if (!previews.isEmpty()) {
                cmd.set(idx + " #RewardsPreview.Visible", true);
                cmd.set(idx + " #RewardsPreview.Text", String.join(" | ", previews));
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
                                @Nonnull PageActionData data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        if (data.action.equals("back")) {
            GuiHelper.navigate(player, ref, store, new MainMenuGui(player, playerRef));
            return;
        }

        if (data.action.startsWith("tab_")) {
            this.currentTab = data.action.substring(4);
            UICommandBuilder cmd = new UICommandBuilder();
            refreshMilestones(cmd);
            this.sendUpdate(cmd);
        }
    }
}

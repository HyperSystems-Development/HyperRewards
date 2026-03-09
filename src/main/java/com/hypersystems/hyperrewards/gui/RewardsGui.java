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
import com.hypersystems.hyperrewards.config.Reward;
import com.hypersystems.hyperrewards.util.TimeUtil;

import javax.annotation.Nonnull;
import java.util.List;

public class RewardsGui extends InteractiveCustomUIPage<PageActionData> {

    private final Player player;
    private final PlayerRef playerRef;
    private String currentTab = "all";

    public RewardsGui(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageActionData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/RewardsPage.ui");

        HyperRewardsConfig.GuiSettings gui = HyperRewards.get().getConfigManager().getConfig().gui;

        cmd.set("#TitleText.Text", gui.rewardsTitle);
        cmd.set("#BtnBack.Text", gui.backButton);
        cmd.set("#TabAll.Text", gui.tabAll);
        cmd.set("#TabDaily.Text", gui.buttonDaily);
        cmd.set("#TabWeekly.Text", gui.buttonWeekly);
        cmd.set("#TabMonthly.Text", gui.buttonMonthly);

        bindButton(events, "#TabAll", "tab_all");
        bindButton(events, "#TabDaily", "tab_daily");
        bindButton(events, "#TabWeekly", "tab_weekly");
        bindButton(events, "#TabMonthly", "tab_monthly");
        bindButton(events, "#BtnBack", "back");

        refreshRewards(cmd);
    }

    private void refreshRewards(UICommandBuilder cmd) {
        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
        HyperRewardsConfig.GuiSettings gui = config.gui;
        String uuid = playerRef.getUuid().toString();

        // Tab active states
        cmd.set("#TabAll.Disabled", currentTab.equals("all"));
        cmd.set("#TabDaily.Disabled", currentTab.equals("daily"));
        cmd.set("#TabWeekly.Disabled", currentTab.equals("weekly"));
        cmd.set("#TabMonthly.Disabled", currentTab.equals("monthly"));

        cmd.clear("#RewardsList");

        List<Reward> rewards = config.rewards;
        int count = 0;

        for (Reward reward : rewards) {
            if (!currentTab.equals("all") && !reward.period.equalsIgnoreCase(currentTab)) {
                continue;
            }

            cmd.append("#RewardsList", "Pages/RewardEntry.ui");
            String idx = "#RewardsList[" + count + "]";

            cmd.set(idx + " #RewardName.Text", reward.id);
            cmd.set(idx + " #TimeReq.Text", "Requires: " + TimeUtil.format(reward.timeRequirement));
            cmd.set(idx + " #PeriodLabel.Text", reward.period);

            boolean claimed = HyperRewards.get().getRewardManager().isClaimed(uuid, reward);
            long playtime = HyperRewardsAPI.get().getPlaytime(playerRef.getUuid(), reward.period);
            boolean eligible = playtime >= reward.timeRequirement;

            if (claimed) {
                cmd.set(idx + " #StatusBadge.Text", gui.rewardsClaimed);
                cmd.set(idx + " #StatusBadge.Style.TextColor", "#55FF55");
                cmd.set(idx + " #IndicatorBar.Background.Color", "#44cc44");
                cmd.set(idx + " #Progress.Value", 1.0f);
                cmd.set(idx + " #Progress.Bar.Color", "#55FF55");
                cmd.set(idx + " #ProgressText.Text", "100%");
            } else if (eligible) {
                cmd.set(idx + " #StatusBadge.Text", gui.rewardsAvailable);
                cmd.set(idx + " #StatusBadge.Style.TextColor", "#FFD700");
                cmd.set(idx + " #IndicatorBar.Background.Color", "#FFD700");
                cmd.set(idx + " #Progress.Value", 1.0f);
                cmd.set(idx + " #Progress.Bar.Color", "#FFD700");
                cmd.set(idx + " #ProgressText.Text", "Ready!");
            } else {
                long remaining = reward.timeRequirement - playtime;
                cmd.set(idx + " #StatusBadge.Text", gui.rewardsLocked);
                cmd.set(idx + " #StatusBadge.Style.TextColor", "#cc4444");
                cmd.set(idx + " #IndicatorBar.Background.Color", "#cc4444");

                float progress = reward.timeRequirement > 0
                        ? Math.min(1.0f, (float) playtime / reward.timeRequirement) : 0.0f;
                cmd.set(idx + " #Progress.Value", progress);
                cmd.set(idx + " #ProgressText.Text",
                        (int) (progress * 100) + "% - " + TimeUtil.format(remaining) + " left");
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
            refreshRewards(cmd);
            this.sendUpdate(cmd);
        }
    }
}

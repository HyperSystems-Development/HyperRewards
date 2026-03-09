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
import com.hypersystems.hyperrewards.listeners.SessionListener;
import com.hypersystems.hyperrewards.util.TimeUtil;

import javax.annotation.Nonnull;

public class PlayerStatsGui extends InteractiveCustomUIPage<PageActionData> {

    private final Player player;
    private final PlayerRef playerRef;
    private String currentPeriod = "all";

    public PlayerStatsGui(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageActionData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/PlayerStats.ui");

        HyperRewardsConfig.GuiSettings gui = HyperRewards.get().getConfigManager().getConfig().gui;

        cmd.set("#TitleText.Text", gui.statsTitle);
        cmd.set("#TotalLabel.Text", gui.statsTotalLabel);
        cmd.set("#PeriodLabel.Text", gui.statsPeriodLabel);
        cmd.set("#SessionLabel.Text", gui.statsSessionLabel);
        cmd.set("#RankLabel.Text", gui.statsRankLabel);

        cmd.set("#BtnAll.Text", gui.buttonAll);
        cmd.set("#BtnDaily.Text", gui.buttonDaily);
        cmd.set("#BtnWeekly.Text", gui.buttonWeekly);
        cmd.set("#BtnMonthly.Text", gui.buttonMonthly);
        cmd.set("#BtnBack.Text", gui.backButton);

        bindButton(events, "#BtnAll", "all");
        bindButton(events, "#BtnDaily", "daily");
        bindButton(events, "#BtnWeekly", "weekly");
        bindButton(events, "#BtnMonthly", "monthly");
        bindButton(events, "#BtnBack", "back");

        refreshStats(cmd);
    }

    private void refreshStats(UICommandBuilder cmd) {
        String uuid = playerRef.getUuid().toString();

        long totalTime = HyperRewards.get().getService().getTotalPlaytime(uuid);
        cmd.set("#TotalTime.Text", TimeUtil.format(totalTime));

        long periodTime = HyperRewards.get().getService().getPlaytime(uuid, currentPeriod);
        cmd.set("#PeriodTime.Text", TimeUtil.format(periodTime));

        long sessionTime = SessionListener.getCurrentSession(playerRef.getUuid());
        cmd.set("#SessionTime.Text", TimeUtil.format(sessionTime));

        int rank = HyperRewards.get().getService().getRank(uuid, currentPeriod);
        cmd.set("#RankValue.Text", rank > 0 ? "#" + rank : "N/A");

        // Disabled = active tab indicator
        cmd.set("#BtnAll.Disabled", currentPeriod.equals("all"));
        cmd.set("#BtnDaily.Disabled", currentPeriod.equals("daily"));
        cmd.set("#BtnWeekly.Disabled", currentPeriod.equals("weekly"));
        cmd.set("#BtnMonthly.Disabled", currentPeriod.equals("monthly"));
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

        this.currentPeriod = data.action;
        UICommandBuilder cmd = new UICommandBuilder();
        refreshStats(cmd);
        this.sendUpdate(cmd);
    }
}

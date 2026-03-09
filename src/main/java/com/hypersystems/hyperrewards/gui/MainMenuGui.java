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

public class MainMenuGui extends InteractiveCustomUIPage<PageActionData> {

    private final Player player;
    private final PlayerRef playerRef;

    public MainMenuGui(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageActionData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/MainMenu.ui");

        HyperRewardsConfig.GuiSettings gui = HyperRewards.get().getConfigManager().getConfig().gui;

        cmd.set("#TitleText.Text", gui.mainMenuTitle);
        cmd.set("#SubtitleText.Text", gui.mainMenuSubtitle);
        cmd.set("#BtnStats.Text", gui.mainMenuStatsButton);
        cmd.set("#BtnRewards.Text", gui.mainMenuRewardsButton);
        cmd.set("#BtnMilestones.Text", gui.mainMenuMilestonesButton);
        cmd.set("#BtnLeaderboard.Text", gui.mainMenuLeaderboardButton);

        // Populate quick stats
        String uuid = playerRef.getUuid().toString();
        long totalTime = HyperRewards.get().getService().getTotalPlaytime(uuid);
        cmd.set("#StatTotal.Text", TimeUtil.format(totalTime));

        long sessionTime = SessionListener.getCurrentSession(playerRef.getUuid());
        cmd.set("#StatSession.Text", TimeUtil.format(sessionTime));

        int rank = HyperRewards.get().getService().getRank(uuid, "all");
        cmd.set("#StatRank.Text", rank > 0 ? "#" + rank : "N/A");

        bindButton(events, "#BtnStats", "stats");
        bindButton(events, "#BtnRewards", "rewards");
        bindButton(events, "#BtnMilestones", "milestones");
        bindButton(events, "#BtnLeaderboard", "leaderboard");

        // Show admin button if player has permission
        if (player.hasPermission("playtime.admin")) {
            cmd.set("#AdminRow.Visible", true);
            bindButton(events, "#BtnAdmin", "admin");
        }
    }

    private void bindButton(UIEventBuilder events, String id, String action) {
        events.addEventBinding(CustomUIEventBindingType.Activating, id, EventData.of("Action", action), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull PageActionData data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        switch (data.action) {
            case "stats" -> GuiHelper.navigate(player, ref, store, new PlayerStatsGui(player, playerRef));
            case "rewards" -> GuiHelper.navigate(player, ref, store, new RewardsGui(player, playerRef));
            case "milestones" -> GuiHelper.navigate(player, ref, store, new MilestonesGui(player, playerRef));
            case "leaderboard" -> GuiHelper.navigate(player, ref, store, new LeaderboardGui(player, playerRef));
            case "admin" -> {
                if (player.hasPermission("playtime.admin")) {
                    GuiHelper.navigate(player, ref, store, new AdminDashboardGui(player, playerRef));
                }
            }
        }
    }
}

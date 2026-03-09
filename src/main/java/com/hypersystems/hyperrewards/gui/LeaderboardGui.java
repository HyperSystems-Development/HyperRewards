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
import com.hypersystems.hyperrewards.util.TimeUtil;

import javax.annotation.Nonnull;
import java.util.Map;

public class LeaderboardGui extends InteractiveCustomUIPage<LeaderboardData> {

    private final Player player;
    private final PlayerRef playerRef;
    private String currentPeriod = "all";

    public LeaderboardGui(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, LeaderboardData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/PlaytimeLeaderboard.ui");

        HyperRewardsConfig.GuiSettings gui = HyperRewards.get().getConfigManager().getConfig().gui;

        cmd.set("#BtnAll.Text", gui.buttonAll);
        cmd.set("#BtnDaily.Text", gui.buttonDaily);
        cmd.set("#BtnWeekly.Text", gui.buttonWeekly);
        cmd.set("#BtnMonthly.Text", gui.buttonMonthly);
        cmd.set("#FooterLabel.Text", gui.footerTitle);
        cmd.set("#BtnBack.Text", gui.backButton);

        bindButton(events, "#BtnAll", "all");
        bindButton(events, "#BtnDaily", "daily");
        bindButton(events, "#BtnWeekly", "weekly");
        bindButton(events, "#BtnMonthly", "monthly");
        bindButton(events, "#BtnBack", "back");

        refreshLeaderboard(cmd);
    }

    private void refreshLeaderboard(UICommandBuilder cmd) {
        HyperRewardsConfig.GuiSettings gui = HyperRewards.get().getConfigManager().getConfig().gui;

        String periodName = gui.buttonAll;
        if (currentPeriod.equals("daily")) periodName = gui.buttonDaily;
        else if (currentPeriod.equals("weekly")) periodName = gui.buttonWeekly;
        else if (currentPeriod.equals("monthly")) periodName = gui.buttonMonthly;

        cmd.set("#TitleText.Text", gui.title + " (" + periodName + ")");

        // Active tab state
        cmd.set("#BtnAll.Disabled", currentPeriod.equals("all"));
        cmd.set("#BtnDaily.Disabled", currentPeriod.equals("daily"));
        cmd.set("#BtnWeekly.Disabled", currentPeriod.equals("weekly"));
        cmd.set("#BtnMonthly.Disabled", currentPeriod.equals("monthly"));

        cmd.clear("#ListContainer");

        Map<String, Long> top = HyperRewards.get().getService().getTopPlayers(currentPeriod);
        String myName = playerRef.getUsername();

        int index = 0;
        for (Map.Entry<String, Long> entry : top.entrySet()) {
            if (index >= 10) break;
            cmd.append("#ListContainer", "Pages/PlaytimeLeaderboardEntry.ui");

            String elementId = "#ListContainer[" + index + "]";
            cmd.set(elementId + " #Rank.Text", "#" + (index + 1));
            cmd.set(elementId + " #Name.Text", entry.getKey());
            cmd.set(elementId + " #Time.Text", TimeUtil.format(entry.getValue()));

            // Rank colors for top 3
            String rankColor = switch (index) {
                case 0 -> "#00FFFF"; // Cyan
                case 1 -> "#C0C0C0"; // Silver
                case 2 -> "#CD7F32"; // Bronze
                default -> "#ffffff";
            };
            cmd.set(elementId + " #Rank.Style.TextColor", rankColor);

            // Highlight current player's row
            if (entry.getKey().equalsIgnoreCase(myName)) {
                cmd.set(elementId + " #EntryBg.Background.Color", "#2a4d7a");
                cmd.set(elementId + " #Name.Style.TextColor", "#5a8bd8");
                cmd.set(elementId + " #Name.Style.RenderBold", true);
            }

            index++;
        }

        long myTime = HyperRewards.get().getService().getPlaytime(playerRef.getUuid().toString(), currentPeriod);
        int myRank = HyperRewards.get().getService().getRank(playerRef.getUuid().toString(), currentPeriod);

        cmd.set("#SelfRank.Text", myRank > 0 ? gui.rankPrefix + myRank : gui.rankPrefix + "N/A");
        cmd.set("#SelfTime.Text", gui.timePrefix + TimeUtil.format(myTime));

        // Player count
        Map<String, Long> allPlayers = HyperRewards.get().getService().getTopPlayers(currentPeriod, 1000);
        cmd.set("#PlayerCount.Text", gui.playerCountLabel + allPlayers.size());
    }

    private void bindButton(UIEventBuilder events, String id, String action) {
        events.addEventBinding(CustomUIEventBindingType.Activating, id, EventData.of("Action", action), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull LeaderboardData data) {
        super.handleDataEvent(ref, store, data);

        if (data.action != null) {
            if (data.action.equals("back")) {
                GuiHelper.navigate(player, ref, store, new MainMenuGui(player, playerRef));
                return;
            }

            this.currentPeriod = data.action;
            UICommandBuilder cmd = new UICommandBuilder();
            refreshLeaderboard(cmd);
            this.sendUpdate(cmd);
        }
    }
}

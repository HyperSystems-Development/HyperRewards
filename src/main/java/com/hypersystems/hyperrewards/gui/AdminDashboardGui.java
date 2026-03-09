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
import com.hypersystems.hyperrewards.util.ColorUtil;

import javax.annotation.Nonnull;

public class AdminDashboardGui extends InteractiveCustomUIPage<AdminActionData> {

    private final Player player;
    private final PlayerRef playerRef;

    public AdminDashboardGui(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminActionData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/AdminDashboard.ui");

        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();

        cmd.set("#RewardsCount.Text", config.rewards.size() + " configured");
        cmd.set("#MilestonesCount.Text", config.milestones.list.size() + " configured");

        bindButton(events, "#BtnRewards", "rewards");
        bindButton(events, "#BtnMilestones", "milestones");
        bindButton(events, "#BtnSettings", "settings");
        bindButton(events, "#BtnReload", "reload");
        bindButton(events, "#BtnBack", "back");
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
            case "rewards" -> GuiHelper.navigate(player, ref, store, new AdminRewardsListGui(player, playerRef));
            case "milestones" -> GuiHelper.navigate(player, ref, store, new AdminMilestonesListGui(player, playerRef));
            case "settings" -> GuiHelper.navigate(player, ref, store, new AdminSettingsGui(player, playerRef));
            case "reload" -> {
                try {
                    HyperRewards.get().getConfigManager().load();
                    player.sendMessage(ColorUtil.color("&aConfiguration reloaded successfully!"));
                } catch (Exception e) {
                    player.sendMessage(ColorUtil.color("&cFailed to reload config."));
                }
                UICommandBuilder cmd = new UICommandBuilder();
                HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
                cmd.set("#RewardsCount.Text", config.rewards.size() + " configured");
                cmd.set("#MilestonesCount.Text", config.milestones.list.size() + " configured");
                this.sendUpdate(cmd);
            }
            case "back" -> GuiHelper.navigate(player, ref, store, new MainMenuGui(player, playerRef));
        }
    }
}

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

public class AdminSettingsGui extends InteractiveCustomUIPage<AdminSettingsData> {

    private final Player player;
    private final PlayerRef playerRef;
    private boolean workingRestEnabled;

    public AdminSettingsGui(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminSettingsData.CODEC);
        this.player = player;
        this.playerRef = playerRef;

        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
        this.workingRestEnabled = config.restReminder.enabled;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/AdminSettings.ui");

        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();

        // Set current values
        cmd.set("#RestEnabledCheck #CheckBox.Value", workingRestEnabled);
        cmd.set("#RestIntervalInput.Value", String.valueOf(config.restReminder.intervalMs / 60000));
        cmd.set("#RestMessageInput.Value", config.restReminder.message);

        // Show HyperPerms auto-detect status (read-only)
        cmd.set("#HyperPermsStatus.Text",
                com.hypersystems.hyperrewards.integration.HyperPermsIntegration.isAvailable()
                        ? "Detected" : "Not detected");

        // Bind checkbox toggle
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RestEnabledCheck #CheckBox",
                EventData.of("Action", "toggleRestEnabled"), false);

        // Bind save (no checkbox values in EventData - tracked in memory)
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSave",
                EventData.of("Action", "save")
                        .append("@RestInterval", "#RestIntervalInput.Value")
                        .append("@RestMessage", "#RestMessageInput.Value"),
                false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCancel",
                EventData.of("Action", "cancel"), false);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull AdminSettingsData data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        switch (data.action) {
            case "save" -> {
                HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();

                config.restReminder.enabled = workingRestEnabled;

                if (data.restInterval != null) {
                    try {
                        long minutes = Long.parseLong(data.restInterval.trim());
                        if (minutes > 0) {
                            config.restReminder.intervalMs = minutes * 60000;
                        }
                    } catch (NumberFormatException ignored) {}
                }

                if (data.restMessage != null && !data.restMessage.trim().isEmpty()) {
                    config.restReminder.message = data.restMessage;
                }

                HyperRewards.get().getConfigManager().save();
                player.sendMessage(ColorUtil.color("&aSettings saved."));
                GuiHelper.navigate(player, ref, store, new AdminDashboardGui(player, playerRef));
            }
            case "cancel" -> GuiHelper.navigate(player, ref, store, new AdminDashboardGui(player, playerRef));
            case "toggleRestEnabled" -> workingRestEnabled = !workingRestEnabled;
        }
    }
}

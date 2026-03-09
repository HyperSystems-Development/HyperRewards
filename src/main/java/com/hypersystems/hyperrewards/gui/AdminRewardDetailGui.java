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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class AdminRewardDetailGui extends InteractiveCustomUIPage<AdminDetailData> {

    private final Player player;
    private final PlayerRef playerRef;
    private final String editingId; // null = creating new
    private final List<String> workingCommands = new ArrayList<>();
    private String workingBroadcast = "";
    private boolean workingRepeatable = false;
    private String workingPeriod = "daily";
    private String workingTime = "";

    public AdminRewardDetailGui(@Nonnull Player player, @Nonnull PlayerRef playerRef, String editingId) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminDetailData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
        this.editingId = editingId;

        // Load existing reward data if editing
        if (editingId != null) {
            HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
            for (Reward r : config.rewards) {
                if (r.id.equals(editingId)) {
                    workingCommands.addAll(r.commands);
                    workingBroadcast = r.broadcastMessage != null ? r.broadcastMessage : "";
                    workingRepeatable = r.repeatable;
                    workingPeriod = r.period;
                    workingTime = String.valueOf(r.timeRequirement / 60000);
                    break;
                }
            }
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/AdminRewardDetail.ui");

        cmd.set("#TitleText.Text", editingId != null ? "EDIT REWARD" : "NEW REWARD");

        // Set form values
        if (editingId != null) {
            cmd.set("#IdInput.Value", editingId);
        }

        cmd.set("#TimeInput.Value", workingTime);
        cmd.set("#BroadcastInput.Value", workingBroadcast);

        // Period selection buttons - disable the active one
        updatePeriodButtons(cmd);

        // Repeatable checkbox
        cmd.set("#RepeatableCheck #CheckBox.Value", workingRepeatable);

        // Commands list
        buildCommandsList(cmd, events);

        // Bind period buttons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PeriodDaily",
                EventData.of("Action", "setPeriod").append("TargetId", "daily"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PeriodWeekly",
                EventData.of("Action", "setPeriod").append("TargetId", "weekly"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PeriodMonthly",
                EventData.of("Action", "setPeriod").append("TargetId", "monthly"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PeriodAll",
                EventData.of("Action", "setPeriod").append("TargetId", "all"), false);

        // Bind checkbox toggle
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RepeatableCheck #CheckBox",
                EventData.of("Action", "toggleRepeatable"), false);

        // Bind save/cancel/addCmd - include all form values in every action to preserve state
        EventData formFields = EventData.of("Action", "save")
                .append("@Id", "#IdInput.Value")
                .append("@Time", "#TimeInput.Value")
                .append("@Broadcast", "#BroadcastInput.Value");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSave", formFields, false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnAddCmd",
                EventData.of("Action", "addCommand")
                        .append("@Command", "#NewCmdInput.Value")
                        .append("@Id", "#IdInput.Value")
                        .append("@Time", "#TimeInput.Value")
                        .append("@Broadcast", "#BroadcastInput.Value"),
                false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCancel",
                EventData.of("Action", "cancel"), false);
    }

    private void updatePeriodButtons(UICommandBuilder cmd) {
        cmd.set("#PeriodDaily.Disabled", "daily".equals(workingPeriod));
        cmd.set("#PeriodWeekly.Disabled", "weekly".equals(workingPeriod));
        cmd.set("#PeriodMonthly.Disabled", "monthly".equals(workingPeriod));
        cmd.set("#PeriodAll.Disabled", "all".equals(workingPeriod));
    }

    private void buildCommandsList(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.clear("#CommandsList");
        for (int i = 0; i < workingCommands.size(); i++) {
            cmd.append("#CommandsList", "Pages/AdminCommandRow.ui");
            String idx = "#CommandsList[" + i + "]";
            cmd.set(idx + " #CmdText.Text", workingCommands.get(i));

            if (events != null) {
                events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #CmdDelete",
                        EventData.of("Action", "removeCommand")
                                .append("RemoveIndex", String.valueOf(i))
                                .append("@Id", "#IdInput.Value")
                                .append("@Time", "#TimeInput.Value")
                                .append("@Broadcast", "#BroadcastInput.Value"),
                        false);
            }
        }
    }

    private void preserveFormState(AdminDetailData data) {
        if (data.timeMinutes != null && !data.timeMinutes.isEmpty()) workingTime = data.timeMinutes;
        if (data.broadcastMessage != null) workingBroadcast = data.broadcastMessage;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull AdminDetailData data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        switch (data.action) {
            case "save" -> {
                String newId = data.id != null ? data.id.trim() : "";
                if (newId.isEmpty()) {
                    player.sendMessage(ColorUtil.color("&cReward ID cannot be empty."));
                    return;
                }

                preserveFormState(data);

                long timeMs;
                try {
                    timeMs = Long.parseLong(workingTime.trim()) * 60000;
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.color("&cInvalid time value. Enter minutes as a number."));
                    return;
                }

                if (timeMs <= 0) {
                    player.sendMessage(ColorUtil.color("&cTime must be greater than 0."));
                    return;
                }

                HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();

                if (editingId != null) {
                    // Remove old entry
                    config.rewards.removeIf(r -> r.id.equals(editingId));
                }

                // Check for duplicate ID (skip if same ID as editing)
                boolean duplicate = config.rewards.stream().anyMatch(r -> r.id.equals(newId));
                if (duplicate) {
                    player.sendMessage(ColorUtil.color("&cReward with ID '" + newId + "' already exists."));
                    return;
                }

                Reward reward = new Reward(newId, workingPeriod, timeMs,
                        new ArrayList<>(workingCommands), workingBroadcast, workingRepeatable);
                config.rewards.add(reward);
                HyperRewards.get().getConfigManager().save();

                String verb = editingId != null ? "updated" : "created";
                player.sendMessage(ColorUtil.color("&aReward '" + newId + "' " + verb + "."));
                GuiHelper.navigate(player, ref, store, new AdminRewardsListGui(player, playerRef));
            }
            case "cancel" -> GuiHelper.navigate(player, ref, store, new AdminRewardsListGui(player, playerRef));
            case "toggleRepeatable" -> workingRepeatable = !workingRepeatable;
            case "setPeriod" -> {
                if (data.targetId != null) {
                    workingPeriod = data.targetId;
                    UICommandBuilder cmd = new UICommandBuilder();
                    updatePeriodButtons(cmd);
                    this.sendUpdate(cmd);
                }
            }
            case "addCommand" -> {
                if (data.commandText != null && !data.commandText.trim().isEmpty()) {
                    preserveFormState(data);
                    workingCommands.add(data.commandText.trim());

                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#IdInput.Value", data.id != null ? data.id : "");
                    cmd.set("#TimeInput.Value", workingTime);
                    cmd.set("#BroadcastInput.Value", workingBroadcast);
                    cmd.set("#RepeatableCheck #CheckBox.Value", workingRepeatable);
                    cmd.set("#NewCmdInput.Value", "");
                    buildCommandsList(cmd, null);
                    this.sendUpdate(cmd);
                }
            }
            case "removeCommand" -> {
                if (data.removeIndex != null) {
                    try {
                        int idx = Integer.parseInt(data.removeIndex);
                        if (idx >= 0 && idx < workingCommands.size()) {
                            preserveFormState(data);
                            workingCommands.remove(idx);

                            UICommandBuilder cmd = new UICommandBuilder();
                            cmd.set("#IdInput.Value", data.id != null ? data.id : "");
                            cmd.set("#TimeInput.Value", workingTime);
                            cmd.set("#BroadcastInput.Value", workingBroadcast);
                            cmd.set("#RepeatableCheck #CheckBox.Value", workingRepeatable);
                            buildCommandsList(cmd, null);
                            this.sendUpdate(cmd);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }
}

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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class AdminMilestoneDetailGui extends InteractiveCustomUIPage<AdminDetailData> {

    private final Player player;
    private final PlayerRef playerRef;
    private final String editingId;

    private List<String> workingCommands;
    private List<String> workingPermissions;
    private String workingBroadcast;
    private String workingPrivateMessage;
    private String workingGroup;
    private boolean workingRepeatable;
    private String workingPeriod;
    private long workingTime;

    public AdminMilestoneDetailGui(@Nonnull Player player, @Nonnull PlayerRef playerRef, String editingId) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminDetailData.CODEC);
        this.player = player;
        this.playerRef = playerRef;
        this.editingId = editingId;
        this.workingCommands = new ArrayList<>();
        this.workingPermissions = new ArrayList<>();
        this.workingBroadcast = "";
        this.workingPrivateMessage = "";
        this.workingGroup = "";
        this.workingRepeatable = false;
        this.workingPeriod = "all";
        this.workingTime = 0;

        if (editingId != null) {
            loadFromConfig();
        }
    }

    private void loadFromConfig() {
        HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();
        for (Milestone m : config.milestones.list) {
            if (m.id.equals(editingId)) {
                workingCommands = m.commands != null ? new ArrayList<>(m.commands) : new ArrayList<>();
                workingPermissions = m.grantPermissions != null ? new ArrayList<>(m.grantPermissions) : new ArrayList<>();
                workingBroadcast = m.broadcastMessage != null ? m.broadcastMessage : "";
                workingPrivateMessage = m.privateMessage != null ? m.privateMessage : "";
                workingGroup = m.addToGroup != null ? m.addToGroup : "";
                workingRepeatable = m.repeatable;
                workingPeriod = m.period != null ? m.period : "all";
                workingTime = m.timeRequirement / 60000; // convert ms to minutes for display
                break;
            }
        }
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {

        cmd.append("Pages/AdminMilestoneDetail.ui");

        if (editingId != null) {
            cmd.set("#TitleText.Text", "EDIT MILESTONE");
            cmd.set("#IdInput.Value", editingId);
        } else {
            cmd.set("#TitleText.Text", "ADD MILESTONE");
            cmd.set("#IdInput.Value", "");
        }

        cmd.set("#TimeInput.Value", String.valueOf(workingTime));
        cmd.set("#BroadcastInput.Value", workingBroadcast);
        cmd.set("#PrivateInput.Value", workingPrivateMessage);
        cmd.set("#GroupInput.Value", workingGroup);
        cmd.set("#RepeatableCheck #CheckBox.Value", workingRepeatable);

        // Set period dropdown selection
        cmd.set("#PeriodDropdown.Value", workingPeriod);

        // Build permissions list
        buildPermsList(cmd, events);

        // Build commands list
        buildCommandsList(cmd, events);

        // Bind checkbox toggle
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RepeatableCheck #CheckBox",
                EventData.of("Action", "toggleRepeatable"), false);

        // Bind save - read all field values (except checkbox - tracked in memory)
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnSave",
                EventData.of("Action", "save")
                        .append("@Id", "#IdInput.Value")
                        .append("@Period", "#PeriodDropdown.Value")
                        .append("@Time", "#TimeInput.Value")
                        .append("@Broadcast", "#BroadcastInput.Value")
                        .append("@Private", "#PrivateInput.Value")
                        .append("@Group", "#GroupInput.Value"),
                false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnCancel",
                EventData.of("Action", "cancel"), false);

        // Bind add perm
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnAddPerm",
                EventData.of("Action", "addPerm")
                        .append("@Perm", "#NewPermInput.Value")
                        .append("@Id", "#IdInput.Value")
                        .append("@Period", "#PeriodDropdown.Value")
                        .append("@Time", "#TimeInput.Value")
                        .append("@Broadcast", "#BroadcastInput.Value")
                        .append("@Private", "#PrivateInput.Value")
                        .append("@Group", "#GroupInput.Value"),
                false);

        // Bind add command
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BtnAddCmd",
                EventData.of("Action", "addCmd")
                        .append("@Command", "#NewCmdInput.Value")
                        .append("@Id", "#IdInput.Value")
                        .append("@Period", "#PeriodDropdown.Value")
                        .append("@Time", "#TimeInput.Value")
                        .append("@Broadcast", "#BroadcastInput.Value")
                        .append("@Private", "#PrivateInput.Value")
                        .append("@Group", "#GroupInput.Value"),
                false);
    }

    private void buildPermsList(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.clear("#PermsList");
        for (int i = 0; i < workingPermissions.size(); i++) {
            cmd.append("#PermsList", "Pages/AdminPermRow.ui");
            String idx = "#PermsList[" + i + "]";
            cmd.set(idx + " #PermText.Text", workingPermissions.get(i));

            if (events != null) {
                events.addEventBinding(CustomUIEventBindingType.Activating,
                        idx + " #PermDelete",
                        EventData.of("Action", "removePerm")
                                .append("RemoveIndex", String.valueOf(i))
                                .append("@Id", "#IdInput.Value")
                                .append("@Period", "#PeriodDropdown.Value")
                                .append("@Time", "#TimeInput.Value")
                                .append("@Broadcast", "#BroadcastInput.Value")
                                .append("@Private", "#PrivateInput.Value")
                                .append("@Group", "#GroupInput.Value"),
                        false);
            }
        }
    }

    private void buildCommandsList(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.clear("#CommandsList");
        for (int i = 0; i < workingCommands.size(); i++) {
            cmd.append("#CommandsList", "Pages/AdminCommandRow.ui");
            String idx = "#CommandsList[" + i + "]";
            cmd.set(idx + " #CmdText.Text", workingCommands.get(i));

            if (events != null) {
                events.addEventBinding(CustomUIEventBindingType.Activating,
                        idx + " #CmdDelete",
                        EventData.of("Action", "removeCmd")
                                .append("RemoveIndex", String.valueOf(i))
                                .append("@Id", "#IdInput.Value")
                                .append("@Period", "#PeriodDropdown.Value")
                                .append("@Time", "#TimeInput.Value")
                                .append("@Broadcast", "#BroadcastInput.Value")
                                .append("@Private", "#PrivateInput.Value")
                                .append("@Group", "#GroupInput.Value"),
                        false);
            }
        }
    }

    private void preserveFormState(AdminDetailData data) {
        if (data.period != null && !data.period.isEmpty()) workingPeriod = data.period;
        if (data.timeMinutes != null && !data.timeMinutes.isEmpty()) {
            try {
                workingTime = Long.parseLong(data.timeMinutes.trim());
            } catch (NumberFormatException ignored) {}
        }
        if (data.broadcastMessage != null) workingBroadcast = data.broadcastMessage;
        if (data.privateMessage != null) workingPrivateMessage = data.privateMessage;
        if (data.addToGroup != null) workingGroup = data.addToGroup;
        // repeatable is tracked via ValueChanged toggle, not from EventData
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull AdminDetailData data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) return;

        switch (data.action) {
            case "save" -> {
                if (data.id == null || data.id.trim().isEmpty()) {
                    player.sendMessage(ColorUtil.color("&cMilestone ID is required."));
                    return;
                }

                preserveFormState(data);

                HyperRewardsConfig config = HyperRewards.get().getConfigManager().getConfig();

                // Remove old entry if editing
                if (editingId != null) {
                    config.milestones.list.removeIf(m -> m.id.equals(editingId));
                }

                // Check for duplicate ID
                String newId = data.id.trim();
                boolean duplicate = config.milestones.list.stream().anyMatch(m -> m.id.equals(newId));
                if (duplicate) {
                    player.sendMessage(ColorUtil.color("&cA milestone with ID '" + newId + "' already exists."));
                    return;
                }

                Milestone milestone = new Milestone();
                milestone.id = newId;
                milestone.period = workingPeriod;
                milestone.timeRequirement = workingTime * 60000; // minutes to ms
                milestone.repeatable = workingRepeatable;
                milestone.broadcastMessage = workingBroadcast.isEmpty() ? null : workingBroadcast;
                milestone.privateMessage = workingPrivateMessage.isEmpty() ? null : workingPrivateMessage;
                milestone.addToGroup = workingGroup.isEmpty() ? null : workingGroup;
                milestone.grantPermissions = new ArrayList<>(workingPermissions);
                milestone.commands = new ArrayList<>(workingCommands);

                config.milestones.list.add(milestone);
                HyperRewards.get().getConfigManager().save();

                String verb = editingId != null ? "updated" : "created";
                player.sendMessage(ColorUtil.color("&aMilestone '" + newId + "' " + verb + "."));
                GuiHelper.navigate(player, ref, store, new AdminMilestonesListGui(player, playerRef));
            }
            case "cancel" -> GuiHelper.navigate(player, ref, store, new AdminMilestonesListGui(player, playerRef));
            case "toggleRepeatable" -> workingRepeatable = !workingRepeatable;
            case "addPerm" -> {
                if (data.permText != null && !data.permText.trim().isEmpty()) {
                    preserveFormState(data);
                    workingPermissions.add(data.permText.trim());

                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#IdInput.Value", data.id != null ? data.id : "");
                    cmd.set("#PeriodDropdown.Value", workingPeriod);
                    cmd.set("#TimeInput.Value", String.valueOf(workingTime));
                    cmd.set("#BroadcastInput.Value", workingBroadcast);
                    cmd.set("#PrivateInput.Value", workingPrivateMessage);
                    cmd.set("#GroupInput.Value", workingGroup);
                    cmd.set("#RepeatableCheck #CheckBox.Value", workingRepeatable);
                    cmd.set("#NewPermInput.Value", "");
                    buildPermsList(cmd, null);
                    buildCommandsList(cmd, null);
                    this.sendUpdate(cmd);
                }
            }
            case "removePerm" -> {
                if (data.removeIndex != null) {
                    try {
                        int index = Integer.parseInt(data.removeIndex);
                        if (index >= 0 && index < workingPermissions.size()) {
                            preserveFormState(data);
                            workingPermissions.remove(index);

                            UICommandBuilder cmd = new UICommandBuilder();
                            cmd.set("#IdInput.Value", data.id != null ? data.id : "");
                            cmd.set("#PeriodDropdown.Value", workingPeriod);
                            cmd.set("#TimeInput.Value", String.valueOf(workingTime));
                            cmd.set("#BroadcastInput.Value", workingBroadcast);
                            cmd.set("#PrivateInput.Value", workingPrivateMessage);
                            cmd.set("#GroupInput.Value", workingGroup);
                            cmd.set("#RepeatableCheck #CheckBox.Value", workingRepeatable);
                            buildPermsList(cmd, null);
                            buildCommandsList(cmd, null);
                            this.sendUpdate(cmd);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            case "addCmd" -> {
                if (data.commandText != null && !data.commandText.trim().isEmpty()) {
                    preserveFormState(data);
                    workingCommands.add(data.commandText.trim());

                    UICommandBuilder cmd = new UICommandBuilder();
                    cmd.set("#IdInput.Value", data.id != null ? data.id : "");
                    cmd.set("#PeriodDropdown.Value", workingPeriod);
                    cmd.set("#TimeInput.Value", String.valueOf(workingTime));
                    cmd.set("#BroadcastInput.Value", workingBroadcast);
                    cmd.set("#PrivateInput.Value", workingPrivateMessage);
                    cmd.set("#GroupInput.Value", workingGroup);
                    cmd.set("#RepeatableCheck #CheckBox.Value", workingRepeatable);
                    cmd.set("#NewCmdInput.Value", "");
                    buildPermsList(cmd, null);
                    buildCommandsList(cmd, null);
                    this.sendUpdate(cmd);
                }
            }
            case "removeCmd" -> {
                if (data.removeIndex != null) {
                    try {
                        int index = Integer.parseInt(data.removeIndex);
                        if (index >= 0 && index < workingCommands.size()) {
                            preserveFormState(data);
                            workingCommands.remove(index);

                            UICommandBuilder cmd = new UICommandBuilder();
                            cmd.set("#IdInput.Value", data.id != null ? data.id : "");
                            cmd.set("#PeriodDropdown.Value", workingPeriod);
                            cmd.set("#TimeInput.Value", String.valueOf(workingTime));
                            cmd.set("#BroadcastInput.Value", workingBroadcast);
                            cmd.set("#PrivateInput.Value", workingPrivateMessage);
                            cmd.set("#GroupInput.Value", workingGroup);
                            cmd.set("#RepeatableCheck #CheckBox.Value", workingRepeatable);
                            buildPermsList(cmd, null);
                            buildCommandsList(cmd, null);
                            this.sendUpdate(cmd);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }
}

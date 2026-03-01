package com.hypersystems.hyperrewards.config;

import java.util.ArrayList;
import java.util.List;

public class Milestone {
    public String id;                       // e.g. "10h_veteran"
    public long timeRequirement;            // milliseconds
    public String period;                   // "all" (total), "daily", "weekly", "monthly"

    // Actions (all optional)
    public List<String> grantPermissions;   // permissions to grant via HyperPerms
    public String addToGroup;               // HyperPerms group to add player to
    public String broadcastMessage;         // public chat announcement (null = disabled)
    public String privateMessage;           // private message to player (null = disabled)
    public List<String> commands;           // console commands to execute
    public boolean repeatable;              // false = one-time, true = each period reset

    public Milestone() {
        this.grantPermissions = new ArrayList<>();
        this.commands = new ArrayList<>();
    }

    public Milestone(String id, long timeRequirement, String period) {
        this();
        this.id = id;
        this.timeRequirement = timeRequirement;
        this.period = period;
    }
}

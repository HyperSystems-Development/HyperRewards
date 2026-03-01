package com.hypersystems.hyperrewards.config;

import java.util.ArrayList;
import java.util.List;

public class Reward {
    public String id;
    public String period;
    public long timeRequirement;
    public List<String> commands;
    public String broadcastMessage;
    public boolean repeatable;
    public String world; // null or empty = all worlds

    public Reward() {
        this.commands = new ArrayList<>();
        this.repeatable = false;
        this.world = null;
    }

    public Reward(String id, String period, long timeRequirement, List<String> commands, String broadcastMessage) {
        this(id, period, timeRequirement, commands, broadcastMessage, false);
    }

    public Reward(String id, String period, long timeRequirement, List<String> commands, String broadcastMessage, boolean repeatable) {
        this.id = id;
        this.period = period;
        this.timeRequirement = timeRequirement;
        this.commands = commands;
        this.broadcastMessage = broadcastMessage;
        this.repeatable = repeatable;
    }
}

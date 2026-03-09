package com.hypersystems.hyperrewards.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.hypersystems.hyperrewards.HyperRewards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.List;

public class ConfigManager {

    private final File configFile;
    private volatile HyperRewardsConfig config;
    private final Gson gson;
    private final Logger logger = LoggerFactory.getLogger("HyperRewards-Config");

    public ConfigManager(File dataFolder) {
        this.configFile = new File(dataFolder, "config.json");
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public void init() {
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        load();
    }

    private void saveDefaultConfig() {
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        try (InputStream in = HyperRewards.class.getResourceAsStream("/config.json")) {
            if (in != null) {
                Files.copy(in, configFile.toPath());
                logger.info("Copied default config.json from JAR.");
            } else {
                config = new HyperRewardsConfig();
                save();
                logger.warn("Could not find config.json in JAR, created blank default.");
            }
        } catch (IOException e) {
            logger.error("Failed to create config.json", e);
        }
    }

    public void load() {
        try (Reader reader = new FileReader(configFile)) {
            config = gson.fromJson(reader, HyperRewardsConfig.class);
            if (config == null) config = new HyperRewardsConfig();
            config.setDefaults();
            migrateConfig();
            save();
            logger.info("Configuration loaded!");
        } catch (IOException e) {
            logger.error("Failed to load config.json", e);
            config = new HyperRewardsConfig();
            config.setDefaults();
            save();
        }
    }

    /**
     * Migrates old config versions to the current format.
     * Detects missing sections and adds defaults without losing existing values.
     */
    private void migrateConfig() {
        boolean migrated = false;

        // v1 -> v2: Add milestones, rest reminders, integrations
        if (config.configVersion < 2) {
            logger.info("Migrating config from v{} to v2...", config.configVersion);

            if (config.milestones == null) {
                config.milestones = new HyperRewardsConfig.MilestoneSettings();
            }
            if (config.milestones.list == null) {
                config.milestones.list = new java.util.ArrayList<>();
            }
            if (config.restReminder == null) {
                config.restReminder = new HyperRewardsConfig.RestReminderSettings();
            }
            if (config.integrations == null) {
                config.integrations = new HyperRewardsConfig.IntegrationSettings();
            }

            // Add new message defaults if missing
            if (config.messages.milestoneAnnouncement == null) {
                config.messages.milestoneAnnouncement = "&6%player% &ehas reached the &6%milestone% &emilestone! (&6%time%&e played)";
            }
            if (config.messages.milestonePrivate == null) {
                config.messages.milestonePrivate = "&a[Milestone] &7Congratulations! You reached the &e%milestone% &7milestone!";
            }
            if (config.messages.permissionGranted == null) {
                config.messages.permissionGranted = "&a[HyperRewards] &7You've been granted permission: &e%permission%";
            }
            if (config.messages.groupAssigned == null) {
                config.messages.groupAssigned = "&a[HyperRewards] &7You've been added to the &e%group% &7group!";
            }
            if (config.messages.restReminderMsg == null) {
                config.messages.restReminderMsg = "&e[HyperRewards] &7You've been playing for &e%session_time%&7. Consider taking a break!";
            }

            config.configVersion = 2;
            migrated = true;
        }

        if (migrated) {
            logger.info("Config migration complete.");
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(configFile)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            logger.error("Failed to save config.json", e);
        }
    }

    /**
     * Applies a template by name, loading rewards and milestones from the templates/ resource folder.
     * Replaces existing rewards and milestones with the template data.
     *
     * @param templateName the template name (e.g. "default")
     * @return a result message describing what was applied, or an error message
     */
    public String applyTemplate(String templateName) {
        String resourcePath = "/templates/" + templateName + ".json";
        try (InputStream in = HyperRewards.class.getResourceAsStream(resourcePath);
             Reader reader = new InputStreamReader(in)) {

            JsonObject template = gson.fromJson(reader, JsonObject.class);
            if (template == null) {
                return "Template '" + templateName + "' is empty.";
            }

            int rewardsCount = 0;
            int milestonesCount = 0;

            if (template.has("rewards")) {
                JsonArray rewardsArray = template.getAsJsonArray("rewards");
                Type rewardListType = new TypeToken<List<Reward>>(){}.getType();
                List<Reward> templateRewards = gson.fromJson(rewardsArray, rewardListType);
                config.rewards = templateRewards;
                rewardsCount = templateRewards.size();
            }

            if (template.has("milestones")) {
                JsonArray milestonesArray = template.getAsJsonArray("milestones");
                Type milestoneListType = new TypeToken<List<Milestone>>(){}.getType();
                List<Milestone> templateMilestones = gson.fromJson(milestonesArray, milestoneListType);
                config.milestones.list = templateMilestones;
                milestonesCount = templateMilestones.size();
            }

            save();
            return "Applied '" + templateName + "' template: " + rewardsCount + " rewards and " + milestonesCount + " milestones loaded.";

        } catch (NullPointerException e) {
            return "Template '" + templateName + "' not found.";
        } catch (Exception e) {
            logger.error("Failed to apply template: " + templateName, e);
            return "Failed to apply template. Check console for errors.";
        }
    }

    public HyperRewardsConfig getConfig() {
        if (config == null) load();
        return config;
    }
}

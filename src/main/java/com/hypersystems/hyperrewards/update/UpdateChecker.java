package com.hypersystems.hyperrewards.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypersystems.hyperrewards.HyperRewards;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks for plugin updates from the GitHub Releases API.
 * <p>
 * Uses the GitHub API endpoint for the latest release, parsing
 * {@code tag_name} for the version, {@code body} for the changelog,
 * and the first JAR asset's {@code browser_download_url} for the download.
 */
public final class UpdateChecker {

    private static final Logger logger = LoggerFactory.getLogger("HyperRewards-Update");
    private static final String USER_AGENT = "HyperRewards-UpdateChecker";
    private static final int CHECK_TIMEOUT_MS = 10000;
    private static final int DOWNLOAD_TIMEOUT_MS = 60000;
    private static final Gson GSON = new Gson();

    private final String currentVersion;
    private final String checkUrl;
    private final Path modsFolder;

    private final AtomicReference<UpdateInfo> cachedUpdate = new AtomicReference<>();
    private volatile long lastCheckTime = 0;
    private static final long CACHE_DURATION_MS = 300000; // 5 minutes

    public UpdateChecker(@NotNull String currentVersion, @NotNull String checkUrl, @NotNull Path modsFolder) {
        this.currentVersion = currentVersion;
        this.checkUrl = checkUrl;
        this.modsFolder = modsFolder;
    }

    @NotNull
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Checks for updates asynchronously.
     */
    public CompletableFuture<UpdateInfo> checkForUpdates() {
        return checkForUpdates(false);
    }

    /**
     * Checks for updates asynchronously.
     *
     * @param forceRefresh if true, ignores cache
     */
    public CompletableFuture<UpdateInfo> checkForUpdates(boolean forceRefresh) {
        if (!forceRefresh && System.currentTimeMillis() - lastCheckTime < CACHE_DURATION_MS) {
            return CompletableFuture.completedFuture(cachedUpdate.get());
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("[Update] Checking for updates from {}", checkUrl);

                URL url = URI.create(checkUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setConnectTimeout(CHECK_TIMEOUT_MS);
                conn.setReadTimeout(CHECK_TIMEOUT_MS);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    logger.warn("[Update] Failed to check for updates: HTTP {}", responseCode);
                    return null;
                }

                String json;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    json = sb.toString();
                }

                JsonObject obj = GSON.fromJson(json, JsonObject.class);
                String tagName = obj.get("tag_name").getAsString();
                String latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                String changelog = obj.has("body") && !obj.get("body").isJsonNull()
                        ? obj.get("body").getAsString() : null;

                // Find the first .jar asset download URL
                String downloadUrl = null;
                if (obj.has("assets") && obj.get("assets").isJsonArray()) {
                    JsonArray assets = obj.getAsJsonArray("assets");
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject asset = assets.get(i).getAsJsonObject();
                        String assetName = asset.get("name").getAsString();
                        if (assetName.endsWith(".jar")) {
                            downloadUrl = asset.get("browser_download_url").getAsString();
                            break;
                        }
                    }
                }

                lastCheckTime = System.currentTimeMillis();

                if (isNewerVersion(latestVersion, currentVersion)) {
                    UpdateInfo info = new UpdateInfo(latestVersion, downloadUrl, changelog);
                    cachedUpdate.set(info);
                    logger.info("[Update] New version available: {} (current: {})", latestVersion, currentVersion);
                    return info;
                } else {
                    cachedUpdate.set(null);
                    logger.info("[Update] Plugin is up-to-date (v{})", currentVersion);
                    return null;
                }

            } catch (Exception e) {
                logger.warn("[Update] Failed to check for updates: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * Downloads the update to the mods folder.
     */
    public CompletableFuture<Path> downloadUpdate(@NotNull UpdateInfo info) {
        if (info.downloadUrl() == null || info.downloadUrl().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("[Update] Downloading update v{} from {}", info.version(), info.downloadUrl());

                URL url = URI.create(info.downloadUrl()).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(CHECK_TIMEOUT_MS);
                conn.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
                conn.setInstanceFollowRedirects(true);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    logger.warn("[Update] Failed to download update: HTTP {}", responseCode);
                    return null;
                }

                Path updateFile = modsFolder.resolve("HyperRewards-" + info.version() + ".jar");
                Path currentJar = modsFolder.resolve("HyperRewards-" + currentVersion + ".jar");
                Path tempFile = modsFolder.resolve("HyperRewards-" + info.version() + ".jar.tmp");

                try (InputStream in = conn.getInputStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // Verify download
                if (Files.size(tempFile) < 1000) {
                    logger.warn("[Update] Downloaded file seems too small, aborting");
                    Files.deleteIfExists(tempFile);
                    return null;
                }

                // Rename temp to final
                Files.move(tempFile, updateFile, StandardCopyOption.REPLACE_EXISTING);

                // Backup current JAR if it exists (may fail on Windows due to lock)
                if (Files.exists(currentJar) && !currentJar.equals(updateFile)) {
                    Path backupFile = modsFolder.resolve("HyperRewards-" + currentVersion + ".jar.backup");
                    try {
                        Files.move(currentJar, backupFile, StandardCopyOption.REPLACE_EXISTING);
                        logger.info("[Update] Backed up current JAR to {}", backupFile.getFileName());
                    } catch (java.nio.file.FileSystemException e) {
                        logger.warn("[Update] Could not backup old JAR (file in use). Please delete {} manually after restart.", currentJar.getFileName());
                    }
                }

                logger.info("[Update] Successfully downloaded update to {}", updateFile.getFileName());
                return updateFile;

            } catch (Exception e) {
                logger.warn("[Update] Failed to download update: {}", e.getMessage());
                return null;
            }
        });
    }

    @Nullable
    public UpdateInfo getCachedUpdate() {
        return cachedUpdate.get();
    }

    public boolean hasUpdateAvailable() {
        return cachedUpdate.get() != null;
    }

    /**
     * Compares two version strings.
     *
     * @return true if newVersion is newer than oldVersion
     */
    private boolean isNewerVersion(@NotNull String newVersion, @NotNull String oldVersion) {
        try {
            String v1 = newVersion.toLowerCase().startsWith("v") ? newVersion.substring(1) : newVersion;
            String v2 = oldVersion.toLowerCase().startsWith("v") ? oldVersion.substring(1) : oldVersion;

            String[] parts1 = v1.split("\\.");
            String[] parts2 = v2.split("\\.");

            int maxLen = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < maxLen; i++) {
                int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
                int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

                if (num1 > num2) return true;
                if (num1 < num2) return false;
            }

            return false;
        } catch (Exception e) {
            return newVersion.compareTo(oldVersion) > 0;
        }
    }

    private int parseVersionPart(@NotNull String part) {
        String numPart = part.split("-")[0];
        return Integer.parseInt(numPart);
    }

    /**
     * Record containing update information.
     */
    public record UpdateInfo(
            @NotNull String version,
            @Nullable String downloadUrl,
            @Nullable String changelog
    ) {}
}

package com.hypersystems.hyperrewards.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Looks up Hytale player information via the PlayerDB API.
 * Used as a fallback when players are not found online or in local storage.
 *
 * <p>API: <a href="https://playerdb.co/api/player/hytale/">playerdb.co</a>
 */
public final class PlayerDBService {

    private static final String API_URL = "https://playerdb.co/api/player/hytale/";
    private static final Logger logger = LoggerFactory.getLogger("HyperRewards-PlayerDB");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);

    private PlayerDBService() {}

    public record PlayerInfo(@NotNull UUID uuid, @NotNull String username) {}

    /**
     * Looks up a player by username via the PlayerDB API.
     *
     * @param name the player username (case-insensitive)
     * @return future containing player info, or null if not found
     */
    @NotNull
    public static CompletableFuture<@Nullable PlayerInfo> lookup(@NotNull String name) {
        String key = name.toLowerCase();

        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.info);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + name))
                        .timeout(Duration.ofSeconds(5))
                        .header("User-Agent", "HyperRewards-Hytale-Plugin")
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

                    if (json.has("success") && json.get("success").getAsBoolean()
                            && json.has("code") && "player.found".equals(json.get("code").getAsString())) {

                        JsonObject player = json.getAsJsonObject("data").getAsJsonObject("player");
                        UUID uuid = UUID.fromString(player.get("id").getAsString());
                        String username = player.get("username").getAsString();

                        PlayerInfo info = new PlayerInfo(uuid, username);
                        cache.put(key, new CacheEntry(info));
                        return info;
                    }
                } else if (response.statusCode() != 404) {
                    logger.warn("PlayerDB API returned {} for '{}'", response.statusCode(), name);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.warn("PlayerDB lookup failed for '{}': {}", name, e.getMessage());
            }
            return null;
        });
    }

    private static final class CacheEntry {
        final PlayerInfo info;
        final long timestamp;

        CacheEntry(PlayerInfo info) {
            this.info = info;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}

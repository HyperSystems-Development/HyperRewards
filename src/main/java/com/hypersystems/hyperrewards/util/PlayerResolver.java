package com.hypersystems.hyperrewards.util;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypersystems.hyperrewards.HyperRewardsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Centralized player name/UUID resolution for HyperRewards.
 *
 * <p>Resolution chain (in order):
 * <ol>
 *   <li>Online players — case-insensitive match against connected players</li>
 *   <li>Database records — lookup from playtime session history</li>
 *   <li>PlayerDB API — external API lookup as a last resort</li>
 * </ol>
 */
public final class PlayerResolver {

    public record ResolvedPlayer(@NotNull UUID uuid, @NotNull String username) {}

    private PlayerResolver() {}

    /**
     * Resolves a player by username. Checks online players first,
     * then database records, then the PlayerDB external API.
     *
     * @param service  the HyperRewardsService for DB lookups
     * @param username the player username (case-insensitive)
     * @return resolved player, or null if not found anywhere
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static ResolvedPlayer resolve(@NotNull HyperRewardsService service,
                                         @NotNull String username) {
        // 1. Check online players
        for (PlayerRef ref : Universe.get().getPlayers()) {
            String name = ref.getUsername();
            if (name != null && name.equalsIgnoreCase(username)) {
                return new ResolvedPlayer(ref.getUuid(), name);
            }
        }

        // 2. Check database records (previously connected players)
        String storedUuid = service.getUuidByUsername(username);
        if (storedUuid != null) {
            return new ResolvedPlayer(UUID.fromString(storedUuid), username);
        }

        // 3. Fall back to PlayerDB API (any Hytale player)
        PlayerDBService.PlayerInfo info = PlayerDBService.lookup(username).join();
        if (info != null) {
            return new ResolvedPlayer(info.uuid(), info.username());
        }

        return null;
    }
}

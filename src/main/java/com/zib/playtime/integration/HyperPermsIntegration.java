package com.zib.playtime.integration;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Reflection-based integration with HyperPerms for permission checking and granting.
 * Uses reflection to avoid hard dependency on HyperPerms.
 *
 * <p>When HyperPerms is not available, permission checks fail-open (return true)
 * so the plugin works standalone.
 */
public final class HyperPermsIntegration {

    private static final Logger logger = LoggerFactory.getLogger("Playtime-HyperPerms");

    private static boolean available = false;
    private static Object hyperPermsInstance = null;
    private static String initError = null;

    // Cached reflection methods
    private static Method hasPermissionMethod = null;
    private static Method getUserManagerMethod = null;
    private static Method getGroupManagerMethod = null;

    // UserManager methods
    private static Method getUserMethod = null;
    private static Method modifyUserMethod = null;

    // Node builder methods (for adding permissions)
    private static Class<?> nodeClass = null;
    private static Class<?> nodeBuilderClass = null;
    private static Method nodeBuilderFactory = null;
    private static Method nodeBuilderExpiry = null;
    private static Method nodeBuilderBuild = null;

    // User methods
    private static Method addNodeMethod = null;
    private static Method addGroupMethod = null;
    private static Method addGroupWithDurationMethod = null;
    private static Method setPermissionWithDurationMethod = null;

    private HyperPermsIntegration() {}

    /**
     * Initializes the HyperPerms integration via reflection.
     */
    public static void init() {
        try {
            // Load HyperPerms via bootstrap
            Class<?> bootstrapClass = Class.forName("com.hyperperms.HyperPermsBootstrap");
            Method getInstanceMethod = bootstrapClass.getMethod("getInstance");
            hyperPermsInstance = getInstanceMethod.invoke(null);

            if (hyperPermsInstance == null) {
                initError = "HyperPermsBootstrap.getInstance() returned null";
                available = false;
                logger.warn("HyperPerms bootstrap returned null - integration disabled");
                return;
            }

            Class<?> instanceClass = hyperPermsInstance.getClass();

            // Core methods on HyperPerms instance
            hasPermissionMethod = instanceClass.getMethod("hasPermission", UUID.class, String.class);
            getUserManagerMethod = instanceClass.getMethod("getUserManager");
            getGroupManagerMethod = instanceClass.getMethod("getGroupManager");

            // Resolve UserManager methods
            Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
            if (userManager != null) {
                Class<?> umClass = userManager.getClass();
                getUserMethod = umClass.getMethod("getUser", UUID.class);
                modifyUserMethod = umClass.getMethod("modifyUser", UUID.class, java.util.function.Consumer.class);
            }

            // Resolve Node builder for adding permissions
            nodeClass = Class.forName("com.hyperperms.model.Node");
            nodeBuilderClass = Class.forName("com.hyperperms.model.NodeBuilder");
            nodeBuilderFactory = nodeClass.getMethod("builder", String.class);
            nodeBuilderExpiry = nodeBuilderClass.getMethod("expiry", Duration.class);
            nodeBuilderBuild = nodeBuilderClass.getMethod("build");

            // Resolve User methods (addNode, addGroup, setPermission)
            Class<?> userClass = Class.forName("com.hyperperms.model.User");
            addNodeMethod = findMethod(userClass, "addNode", nodeClass);
            addGroupMethod = findMethod(userClass, "addGroup", String.class);
            addGroupWithDurationMethod = findMethod(userClass, "addGroup", String.class, Duration.class);
            setPermissionWithDurationMethod = findMethod(userClass, "setPermission", String.class, boolean.class, Duration.class);

            available = true;
            logger.info("[Integration] HyperPerms integration enabled");

        } catch (ClassNotFoundException e) {
            available = false;
            initError = "HyperPerms not found";
            logger.info("[Integration] HyperPerms not found - running standalone");
        } catch (NoSuchMethodException e) {
            available = false;
            initError = "Method not found: " + e.getMessage();
            logger.warn("HyperPerms API mismatch: {} - integration disabled", e.getMessage());
        } catch (Exception e) {
            available = false;
            initError = e.getClass().getSimpleName() + ": " + e.getMessage();
            logger.warn("Failed to initialize HyperPerms integration: {} - running standalone", e.getMessage());
        }
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            logger.debug("Optional method not found: {}.{}", clazz.getSimpleName(), name);
            return null;
        }
    }

    /**
     * @return true if HyperPerms is loaded and available
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Checks if a player has a permission.
     * Returns true (fail-open) if HyperPerms is not available.
     */
    public static boolean hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        if (!available || hasPermissionMethod == null) {
            return true; // fail-open
        }

        try {
            Object result = hasPermissionMethod.invoke(hyperPermsInstance, playerUuid, permission);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return true;
        } catch (Exception e) {
            logger.warn("Exception checking permission {} for {}: {}, ALLOWING", permission, playerUuid, e.getMessage());
            return true;
        }
    }

    /**
     * Grants a permanent permission to a player via HyperPerms.
     *
     * @return true if the permission was granted successfully
     */
    public static boolean setPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        if (!available || modifyUserMethod == null || nodeBuilderFactory == null) {
            logger.debug("Cannot grant permission {} - HyperPerms not available", permission);
            return false;
        }

        try {
            Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
            if (userManager == null) return false;

            // Build a Node for the permission
            Object nodeBuilder = nodeBuilderFactory.invoke(null, permission);
            Object node = nodeBuilderBuild.invoke(nodeBuilder);

            // Use modifyUser for thread-safe modification
            @SuppressWarnings("unchecked")
            CompletableFuture<Void> future = (CompletableFuture<Void>) modifyUserMethod.invoke(
                    userManager, playerUuid,
                    (java.util.function.Consumer<Object>) user -> {
                        try {
                            addNodeMethod.invoke(user, node);
                        } catch (Exception e) {
                            logger.error("Failed to add node to user: {}", e.getMessage());
                        }
                    }
            );
            future.join();
            logger.info("Granted permission '{}' to {}", permission, playerUuid);
            return true;

        } catch (Exception e) {
            logger.error("Failed to grant permission '{}' to {}: {}", permission, playerUuid, e.getMessage());
            return false;
        }
    }

    /**
     * Grants a temporary permission to a player via HyperPerms.
     *
     * @return true if the permission was granted successfully
     */
    public static boolean setTemporaryPermission(@NotNull UUID playerUuid, @NotNull String permission, @NotNull Duration duration) {
        if (!available || modifyUserMethod == null) {
            logger.debug("Cannot grant temporary permission {} - HyperPerms not available", permission);
            return false;
        }

        try {
            Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
            if (userManager == null) return false;

            // Try setPermission(String, boolean, Duration) first
            if (setPermissionWithDurationMethod != null) {
                @SuppressWarnings("unchecked")
                CompletableFuture<Void> future = (CompletableFuture<Void>) modifyUserMethod.invoke(
                        userManager, playerUuid,
                        (java.util.function.Consumer<Object>) user -> {
                            try {
                                setPermissionWithDurationMethod.invoke(user, permission, true, duration);
                            } catch (Exception e) {
                                logger.error("Failed to set temporary permission: {}", e.getMessage());
                            }
                        }
                );
                future.join();
            } else if (nodeBuilderFactory != null && nodeBuilderExpiry != null) {
                // Fall back to Node builder with expiry
                Object nodeBuilder = nodeBuilderFactory.invoke(null, permission);
                nodeBuilder = nodeBuilderExpiry.invoke(nodeBuilder, duration);
                Object node = nodeBuilderBuild.invoke(nodeBuilder);

                @SuppressWarnings("unchecked")
                CompletableFuture<Void> future = (CompletableFuture<Void>) modifyUserMethod.invoke(
                        userManager, playerUuid,
                        (java.util.function.Consumer<Object>) user -> {
                            try {
                                addNodeMethod.invoke(user, node);
                            } catch (Exception e) {
                                logger.error("Failed to add temporary node: {}", e.getMessage());
                            }
                        }
                );
                future.join();
            } else {
                logger.warn("No method available for temporary permissions");
                return false;
            }

            logger.info("Granted temporary permission '{}' ({}) to {}", permission, duration, playerUuid);
            return true;

        } catch (Exception e) {
            logger.error("Failed to grant temporary permission '{}' to {}: {}", permission, playerUuid, e.getMessage());
            return false;
        }
    }

    /**
     * Adds a player to a HyperPerms group.
     *
     * @return true if the player was added to the group
     */
    public static boolean addToGroup(@NotNull UUID playerUuid, @NotNull String groupName) {
        if (!available || modifyUserMethod == null || addGroupMethod == null) {
            logger.debug("Cannot add to group {} - HyperPerms not available", groupName);
            return false;
        }

        try {
            Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
            if (userManager == null) return false;

            @SuppressWarnings("unchecked")
            CompletableFuture<Void> future = (CompletableFuture<Void>) modifyUserMethod.invoke(
                    userManager, playerUuid,
                    (java.util.function.Consumer<Object>) user -> {
                        try {
                            addGroupMethod.invoke(user, groupName);
                        } catch (Exception e) {
                            logger.error("Failed to add user to group: {}", e.getMessage());
                        }
                    }
            );
            future.join();
            logger.info("Added {} to group '{}'", playerUuid, groupName);
            return true;

        } catch (Exception e) {
            logger.error("Failed to add {} to group '{}': {}", playerUuid, groupName, e.getMessage());
            return false;
        }
    }

    /**
     * Gets detailed status of the integration for debugging.
     */
    public static String getDetailedStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== HyperPerms Integration Status ===\n");
        sb.append("Available: ").append(available).append("\n");
        sb.append("Instance: ").append(hyperPermsInstance != null ? hyperPermsInstance.getClass().getName() : "null").append("\n");
        sb.append("hasPermission: ").append(hasPermissionMethod != null ? "found" : "null").append("\n");
        sb.append("getUserManager: ").append(getUserManagerMethod != null ? "found" : "null").append("\n");
        sb.append("getGroupManager: ").append(getGroupManagerMethod != null ? "found" : "null").append("\n");
        sb.append("modifyUser: ").append(modifyUserMethod != null ? "found" : "null").append("\n");
        sb.append("addNode: ").append(addNodeMethod != null ? "found" : "null").append("\n");
        sb.append("addGroup: ").append(addGroupMethod != null ? "found" : "null").append("\n");
        sb.append("setPermission(temp): ").append(setPermissionWithDurationMethod != null ? "found" : "null").append("\n");
        if (initError != null) {
            sb.append("Init error: ").append(initError).append("\n");
        }
        return sb.toString();
    }

    /**
     * Gets initialization error message if any.
     */
    public static String getInitError() {
        return initError;
    }
}

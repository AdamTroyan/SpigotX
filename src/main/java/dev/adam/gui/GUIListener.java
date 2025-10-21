package dev.adam.gui;

import dev.adam.SpigotX;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for GUI inventory interactions in Bukkit/Spigot plugins.
 * 
 * This listener handles all inventory-related events for custom GUIs, providing
 * click protection, spam prevention, and proper event management. It automatically
 * manages player data cleanup and provides comprehensive event handling for all
 * GUI implementations that extend the GUIBase interface.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic registration and management</li>
 *   <li>Click spam protection with configurable cooldowns</li>
 *   <li>Rate limiting to prevent excessive clicking</li>
 *   <li>Comprehensive inventory event blocking (drag, move, creative)</li>
 *   <li>Automatic player data cleanup on disconnect</li>
 *   <li>Error handling with user-friendly messages</li>
 *   <li>Debug logging for troubleshooting</li>
 *   <li>Statistics tracking for monitoring</li>
 * </ul>
 * 
 * <p>The listener automatically prevents:</p>
 * <ul>
 *   <li>Item dragging in GUI inventories</li>
 *   <li>Item movement between inventories</li>
 *   <li>Creative mode inventory interactions</li>
 *   <li>Excessive clicking (spam protection)</li>
 * </ul>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class GUIListener implements Listener {
    
    /** Whether the listener has been registered with Bukkit */
    private static boolean registered = false;
    
    /** Map of player UUIDs to their last click timestamp for cooldown tracking */
    private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    
    /** Map of player UUIDs to their current click count for rate limiting */
    private static final Map<UUID, Integer> clickCounts = new ConcurrentHashMap<>();
    
    /** Minimum time between clicks in milliseconds */
    private static long clickCooldown = 50;
    
    /** Maximum clicks allowed per second per player */
    private static int maxClicksPerSecond = 20;
    
    /** Whether debug logging is enabled */
    private static boolean debugMode = false;

    // === LISTENER REGISTRATION ===

    /**
     * Ensures the GUI listener is registered with the plugin manager.
     * This method is idempotent - calling it multiple times has no additional effect.
     * Should be called during plugin initialization.
     */
    public static void ensureRegistered() {
        if (!registered) {
            Plugin plugin = SpigotX.getPlugin();
            plugin.getServer().getPluginManager().registerEvents(new GUIListener(), plugin);
            registered = true;
            logDebug("GUIListener registered successfully");
        }
    }

    /**
     * Checks if the listener is currently registered.
     * 
     * @return true if registered, false otherwise
     */
    public static boolean isRegistered() {
        return registered;
    }

    // === CONFIGURATION ===

    /**
     * Sets the minimum time between clicks for spam protection.
     * 
     * @param cooldownMs the cooldown time in milliseconds (minimum 10ms)
     */
    public static void setClickCooldown(long cooldownMs) {
        clickCooldown = Math.max(10, cooldownMs);
        logDebug("Click cooldown set to: " + clickCooldown + "ms");
    }

    /**
     * Sets the maximum number of clicks allowed per second per player.
     * 
     * @param maxClicks the maximum clicks per second (minimum 1, maximum 100)
     */
    public static void setMaxClicksPerSecond(int maxClicks) {
        maxClicksPerSecond = Math.max(1, Math.min(100, maxClicks));
        logDebug("Max clicks per second set to: " + maxClicksPerSecond);
    }

    /**
     * Enables or disables debug logging for the listener.
     * Debug logs provide detailed information about event processing.
     * 
     * @param debug true to enable debug logging, false to disable
     */
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
        logDebug("Debug mode " + (debug ? "enabled" : "disabled"));
    }

    /**
     * Gets the current click cooldown setting.
     * 
     * @return the cooldown time in milliseconds
     */
    public static long getClickCooldown() {
        return clickCooldown;
    }

    /**
     * Gets the current maximum clicks per second setting.
     * 
     * @return the maximum clicks per second
     */
    public static int getMaxClicksPerSecond() {
        return maxClicksPerSecond;
    }

    /**
     * Checks if debug mode is currently enabled.
     * 
     * @return true if debug mode is enabled, false otherwise
     */
    public static boolean isDebugMode() {
        return debugMode;
    }

    // === EVENT HANDLERS ===

    /**
     * Handles inventory click events for GUI inventories.
     * Processes click spam protection, validates the click, and delegates to the appropriate GUI handler.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isOurGui(event.getInventory())) return;

        Player player = (Player) event.getWhoClicked();

        if (!isClickAllowed(player)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null) {
            logDebug("Clicked inventory is null for player: " + player.getName());
            return;
        }

        if (!(event.getClickedInventory().getHolder() instanceof GUIBase)) {
            logDebug("Clicked inventory holder is not GUIBase for player: " + player.getName());
            return;
        }

        int rawSlot = event.getRawSlot();
        GUIBase gui = (GUIBase) event.getInventory().getHolder();

        logDebug("Processing click for " + player.getName() +
                " in " + gui.getClass().getSimpleName() +
                " at slot " + rawSlot);

        if (gui.hasHandler(rawSlot)) {
            try {
                gui.handleClick(event);
                logDebug("Successfully handled click for " + player.getName());
            } catch (Exception e) {
                logError("Error handling click for " + player.getName() +
                        " in " + gui.getClass().getSimpleName(), e);
                player.sendMessage("§cAn error occurred while processing your click. Please try again.");
            }
        } else {
            logDebug("No handler found for slot " + rawSlot +
                    " in " + gui.getClass().getSimpleName());
        }
    }

    /**
     * Handles inventory drag events to prevent item dragging in GUI inventories.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isOurGui(event.getInventory())) {
            event.setCancelled(true);
            logDebug("Blocked drag event for " + event.getWhoClicked().getName());
        }
    }

    /**
     * Handles inventory move events to prevent item movement between inventories.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isOurGui(event.getSource()) || isOurGui(event.getDestination())) {
            event.setCancelled(true);
            logDebug("Blocked move event between inventories");
        }
    }

    /**
     * Handles inventory close events to clean up player data and notify the GUI.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!isOurGui(event.getInventory())) return;

        Player player = (Player) event.getPlayer();
        GUIBase gui = (GUIBase) event.getInventory().getHolder();

        logDebug("Processing close event for " + player.getName() +
                " in " + gui.getClass().getSimpleName());

        try {
            if (gui instanceof GUI) {
                ((GUI) gui).handleClose(event);
            }

            UUID uuid = player.getUniqueId();
            lastClickTime.remove(uuid);
            clickCounts.remove(uuid);

            logDebug("Successfully handled close for " + player.getName());
        } catch (Exception e) {
            logError("Error handling close for " + player.getName() +
                    " in " + gui.getClass().getSimpleName(), e);
        }
    }

    /**
     * Handles inventory open events to reset player click tracking.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!isOurGui(event.getInventory())) return;

        Player player = (Player) event.getPlayer();
        GUIBase gui = (GUIBase) event.getInventory().getHolder();

        logDebug("GUI opened: " + gui.getClass().getSimpleName() +
                " by " + player.getName());

        UUID uuid = player.getUniqueId();
        lastClickTime.remove(uuid);
        clickCounts.remove(uuid);
    }

    /**
     * Handles creative mode inventory events to prevent creative interactions with GUIs.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (isOurGui(event.getInventory())) {
            event.setCancelled(true);
            logDebug("Blocked creative event for " + event.getWhoClicked().getName());
        }
    }

    /**
     * Handles player quit events to clean up tracking data.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        lastClickTime.remove(uuid);
        clickCounts.remove(uuid);

        logDebug("Cleaned up data for disconnected player: " + event.getPlayer().getName());
    }

    /**
     * Handles plugin disable events to clean up all data when the plugin shuts down.
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(SpigotX.getPlugin())) {
            lastClickTime.clear();
            clickCounts.clear();
            
            GUIUpdater.cancelAll();

            logDebug("Plugin disabled - cleaned up all GUI data");
        }
    }

    // === UTILITY METHODS ===

    /**
     * Checks if an inventory belongs to one of our custom GUIs.
     * 
     * @param inv the inventory to check
     * @return true if the inventory is a custom GUI, false otherwise
     */
    private boolean isOurGui(Inventory inv) {
        return inv != null && inv.getHolder() instanceof GUIBase;
    }

    /**
     * Checks if a player is allowed to click based on spam protection rules.
     * Applies both cooldown and rate limiting checks.
     * 
     * @param player the player attempting to click
     * @return true if the click is allowed, false if blocked
     */
    private boolean isClickAllowed(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        Long lastClick = lastClickTime.get(uuid);
        if (lastClick != null && (currentTime - lastClick) < clickCooldown) {
            logDebug("Click blocked for " + player.getName() + " - cooldown not expired");
            return false;
        }

        Integer clicks = clickCounts.get(uuid);
        if (clicks == null) clicks = 0;

        if (clicks >= maxClicksPerSecond) {
            logDebug("Click blocked for " + player.getName() + " - too many clicks per second");
            return false;
        }

        lastClickTime.put(uuid, currentTime);
        clickCounts.put(uuid, clicks + 1);

        SpigotX.getPlugin().getServer().getScheduler().runTaskLater(
                SpigotX.getPlugin(),
                () -> clickCounts.put(uuid, Math.max(0, clickCounts.getOrDefault(uuid, 0) - 1)),
                20L
        );

        return true;
    }

    // === MONITORING AND STATISTICS ===

    /**
     * Gets the number of players currently being tracked for click protection.
     * 
     * @return the number of tracked players
     */
    public static int getTrackedPlayerCount() {
        return lastClickTime.size();
    }

    /**
     * Gets the number of players with active click counts.
     * 
     * @return the number of players with recent clicks
     */
    public static int getActiveClickerCount() {
        return clickCounts.size();
    }

    /**
     * Prints comprehensive statistics about the GUI listener.
     * Useful for monitoring performance and debugging issues.
     */
    public static void printStatistics() {
        System.out.println("=== GUI Listener Statistics ===");
        System.out.println("Listener registered: " + registered);
        System.out.println("Debug mode: " + debugMode);
        System.out.println("Click cooldown: " + clickCooldown + "ms");
        System.out.println("Max clicks per second: " + maxClicksPerSecond);
        System.out.println("Tracked players: " + getTrackedPlayerCount());
        System.out.println("Active clickers: " + getActiveClickerCount());
        System.out.println("==============================");
    }

    /**
     * Manually clears all player tracking data.
     * This can be useful for resetting spam protection or debugging.
     */
    public static void clearTrackingData() {
        lastClickTime.clear();
        clickCounts.clear();
        logDebug("Cleared all tracking data");
    }

    /**
     * Gets the last click time for a specific player.
     * 
     * @param playerId the player's UUID
     * @return the last click timestamp, or null if not found
     */
    public static Long getLastClickTime(UUID playerId) {
        return lastClickTime.get(playerId);
    }

    /**
     * Gets the current click count for a specific player.
     * 
     * @param playerId the player's UUID
     * @return the current click count, or 0 if not found
     */
    public static int getCurrentClickCount(UUID playerId) {
        return clickCounts.getOrDefault(playerId, 0);
    }

    /**
     * Checks if a player is currently being tracked for spam protection.
     * 
     * @param playerId the player's UUID
     * @return true if the player is being tracked, false otherwise
     */
    public static boolean isPlayerTracked(UUID playerId) {
        return lastClickTime.containsKey(playerId) || clickCounts.containsKey(playerId);
    }

    // === LOGGING METHODS ===

    /**
     * Logs debug messages when debug mode is enabled.
     * 
     * @param message the message to log
     */
    private static void logDebug(String message) {
        if (debugMode) {
            System.out.println("[GUIListener DEBUG] " + message);
        }
    }

    /**
     * Logs error messages to the console with optional exception details.
     * 
     * @param message the error message
     * @param e the exception that occurred (can be null)
     */
    private static void logError(String message, Exception e) {
        System.err.println("[GUIListener ERROR] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}
package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * GUI update management system for Bukkit/Spigot plugins.
 * 
 * This utility class provides a comprehensive solution for scheduling and managing
 * periodic updates to GUI inventories. It includes features for controlling update
 * timing, handling errors gracefully, and optimizing performance by automatically
 * stopping updates when GUIs are no longer being viewed.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic scheduling with configurable periods</li>
 *   <li>Pause/resume functionality for temporary stops</li>
 *   <li>Error handling with retry mechanisms</li>
 *   <li>Automatic cleanup when GUIs are not being viewed</li>
 *   <li>Async and sync update options</li>
 *   <li>Conditional updates based on custom predicates</li>
 *   <li>Bulk management for multiple GUIs</li>
 *   <li>Performance monitoring and statistics</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Schedule a GUI to update every 20 ticks (1 second)
 * GUIUpdater.scheduleRepeating(plugin, myGUI, 20L, gui -> {
 *     // Update GUI content here
 *     gui.refresh();
 * });
 * 
 * // Pause updates temporarily
 * GUIUpdater.pauseUpdates(myGUI);
 * 
 * // Resume updates
 * GUIUpdater.resumeUpdates(myGUI);
 * }</pre>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class GUIUpdater {
    
    /** Map of GUIs to their scheduled update tasks */
    private static final Map<GUI, BukkitTask> tasks = new ConcurrentHashMap<>();
    
    /** Map of GUIs to their update configurations */
    private static final Map<GUI, GUIUpdateConfig> configs = new ConcurrentHashMap<>();
    
    /** Whether debug logging is enabled */
    private static boolean debugMode = false;

    /**
     * Configuration class for GUI update behavior.
     * Contains settings for update period, retry logic, and execution options.
     */
    public static class GUIUpdateConfig {
        /** Update period in ticks */
        private final long period;
        /** Whether to stop updates when no players are viewing the GUI */
        private final boolean stopWhenEmpty;
        /** Whether updates should run asynchronously */
        private final boolean asyncUpdate;
        /** Maximum number of retry attempts on failure */
        private final int maxRetries;
        
        /** Current number of failed retry attempts */
        private int currentRetries = 0;
        /** Timestamp of the last successful update */
        private long lastUpdate = 0;
        /** Whether updates are currently paused */
        private boolean paused = false;

        /**
         * Creates a basic update configuration.
         * Uses default settings: stop when empty, sync updates, 3 max retries.
         * 
         * @param period the update period in ticks (20 ticks = 1 second)
         */
        public GUIUpdateConfig(long period) {
            this(period, true, false, 3);
        }

        /**
         * Creates a fully customized update configuration.
         * 
         * @param period the update period in ticks
         * @param stopWhenEmpty whether to stop updates when GUI has no viewers
         * @param asyncUpdate whether to run updates asynchronously
         * @param maxRetries maximum number of retry attempts on failure
         */
        public GUIUpdateConfig(long period, boolean stopWhenEmpty, boolean asyncUpdate, int maxRetries) {
            this.period = Math.max(1, period); // Ensure minimum 1 tick period
            this.stopWhenEmpty = stopWhenEmpty;
            this.asyncUpdate = asyncUpdate;
            this.maxRetries = Math.max(0, maxRetries);
        }

        /**
         * Gets the update period in ticks.
         * 
         * @return the update period
         */
        public long getPeriod() {
            return period;
        }

        /**
         * Checks if updates should stop when no players are viewing the GUI.
         * 
         * @return true if updates stop when empty, false otherwise
         */
        public boolean shouldStopWhenEmpty() {
            return stopWhenEmpty;
        }

        /**
         * Checks if updates run asynchronously.
         * 
         * @return true if async updates, false for sync
         */
        public boolean isAsyncUpdate() {
            return asyncUpdate;
        }

        /**
         * Gets the maximum number of retry attempts.
         * 
         * @return maximum retry attempts
         */
        public int getMaxRetries() {
            return maxRetries;
        }

        /**
         * Gets the current number of failed retry attempts.
         * 
         * @return current retry count
         */
        public int getCurrentRetries() {
            return currentRetries;
        }

        /**
         * Gets the timestamp of the last successful update.
         * 
         * @return last update timestamp in milliseconds
         */
        public long getLastUpdate() {
            return lastUpdate;
        }

        /**
         * Checks if updates are currently paused.
         * 
         * @return true if paused, false otherwise
         */
        public boolean isPaused() {
            return paused;
        }

        /** Increments the retry counter for failed updates */
        void incrementRetries() {
            currentRetries++;
        }

        /** Resets the retry counter after a successful update */
        void resetRetries() {
            currentRetries = 0;
        }

        /** Updates the last successful update timestamp */
        void updateLastUpdate() {
            lastUpdate = System.currentTimeMillis();
        }

        /** Sets the paused state for this configuration */
        void setPaused(boolean paused) {
            this.paused = paused;
        }
    }

    // === BASIC SCHEDULING ===

    /**
     * Schedules a GUI to update repeatedly at the specified interval.
     * Uses default configuration (stops when empty, sync updates, 3 retries).
     * 
     * @param plugin the plugin scheduling the updates
     * @param gui the GUI to update
     * @param period the update period in ticks (20 ticks = 1 second)
     * @param update the function to execute on each update
     */
    public static void scheduleRepeating(Plugin plugin, GUI gui, long period, Consumer<GUI> update) {
        scheduleRepeating(plugin, gui, new GUIUpdateConfig(period), update);
    }

    /**
     * Schedules a GUI to update repeatedly with custom configuration.
     * Cancels any existing updates for the GUI before scheduling new ones.
     * 
     * @param plugin the plugin scheduling the updates
     * @param gui the GUI to update
     * @param config the update configuration to use
     * @param update the function to execute on each update
     */
    public static void scheduleRepeating(Plugin plugin, GUI gui, GUIUpdateConfig config, Consumer<GUI> update) {
        if (plugin == null || gui == null || update == null || config == null) {
            logDebug("Invalid parameters for scheduleRepeating");
            return;
        }

        // Cancel any existing updates for this GUI
        cancel(gui);
        configs.put(gui, config);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                // Skip if updates are paused
                if (config.isPaused()) {
                    logDebug("Update paused for GUI: " + gui.getClass().getSimpleName());
                    return;
                }

                // Auto-stop if GUI has no viewers and configured to do so
                if (config.shouldStopWhenEmpty() && gui.getInventory().getViewers().isEmpty()) {
                    logDebug("Stopping update for empty GUI: " + gui.getClass().getSimpleName());
                    cancel(gui);
                    return;
                }

                // Execute update based on configuration
                if (config.isAsyncUpdate()) {
                    runUpdateAsync(plugin, gui, update, config);
                } else {
                    runUpdateSync(gui, update, config);
                }

            } catch (Exception e) {
                handleUpdateError(gui, config, e);
            }
        }, config.getPeriod(), config.getPeriod());

        tasks.put(gui, task);
        logDebug("Scheduled repeating update for GUI: " + gui.getClass().getSimpleName() +
                " with period: " + config.getPeriod());
    }

    /**
     * Schedules a single delayed update for a GUI.
     * The update will only execute if the GUI still has viewers when the delay expires.
     * 
     * @param plugin the plugin scheduling the update
     * @param gui the GUI to update
     * @param delay the delay in ticks before executing the update
     * @param update the function to execute
     */
    public static void scheduleDelayed(Plugin plugin, GUI gui, long delay, Consumer<GUI> update) {
        if (plugin == null || gui == null || update == null) {
            logDebug("Invalid parameters for scheduleDelayed");
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                if (!gui.getInventory().getViewers().isEmpty()) {
                    update.accept(gui);
                    logDebug("Executed delayed update for GUI: " + gui.getClass().getSimpleName());
                } else {
                    logDebug("Skipped delayed update for empty GUI: " + gui.getClass().getSimpleName());
                }
            } catch (Exception e) {
                logError("Error in delayed update for " + gui.getClass().getSimpleName(), e);
            }
        }, Math.max(1, delay));
    }

    /**
     * Schedules an immediate asynchronous update for a GUI.
     * The update runs on a separate thread to avoid blocking the main server thread.
     * 
     * @param plugin the plugin scheduling the update
     * @param gui the GUI to update
     * @param update the function to execute asynchronously
     */
    public static void scheduleAsync(Plugin plugin, GUI gui, Consumer<GUI> update) {
        if (plugin == null || gui == null || update == null) {
            logDebug("Invalid parameters for scheduleAsync");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                update.accept(gui);
                logDebug("Executed async update for GUI: " + gui.getClass().getSimpleName());
            } catch (Exception e) {
                logError("Error in async update for " + gui.getClass().getSimpleName(), e);
            }
        });
    }

    // === UPDATE CONTROL ===

    /**
     * Pauses updates for a specific GUI without canceling the scheduled task.
     * Updates can be resumed later with {@link #resumeUpdates(GUI)}.
     * 
     * @param gui the GUI to pause updates for
     */
    public static void pauseUpdates(GUI gui) {
        GUIUpdateConfig config = configs.get(gui);
        if (config != null) {
            config.setPaused(true);
            logDebug("Paused updates for GUI: " + gui.getClass().getSimpleName());
        } else {
            logDebug("No config found to pause for GUI: " + gui.getClass().getSimpleName());
        }
    }

    /**
     * Resumes previously paused updates for a specific GUI.
     * 
     * @param gui the GUI to resume updates for
     */
    public static void resumeUpdates(GUI gui) {
        GUIUpdateConfig config = configs.get(gui);
        if (config != null) {
            config.setPaused(false);
            logDebug("Resumed updates for GUI: " + gui.getClass().getSimpleName());
        } else {
            logDebug("No config found to resume for GUI: " + gui.getClass().getSimpleName());
        }
    }

    /**
     * Checks if updates are currently paused for a GUI.
     * 
     * @param gui the GUI to check
     * @return true if updates are paused, false otherwise
     */
    public static boolean isPaused(GUI gui) {
        GUIUpdateConfig config = configs.get(gui);
        return config != null && config.isPaused();
    }

    /**
     * Changes the update period for an existing scheduled GUI.
     * This will cancel and reschedule the GUI with the new period.
     * 
     * @param plugin the plugin that owns the updates
     * @param gui the GUI to modify
     * @param newPeriod the new update period in ticks
     * @param update the update function (must be the same as originally scheduled)
     */
    public static void changePeriod(Plugin plugin, GUI gui, long newPeriod, Consumer<GUI> update) {
        GUIUpdateConfig oldConfig = configs.get(gui);
        if (oldConfig != null) {
            GUIUpdateConfig newConfig = new GUIUpdateConfig(
                    newPeriod,
                    oldConfig.shouldStopWhenEmpty(),
                    oldConfig.isAsyncUpdate(),
                    oldConfig.getMaxRetries()
            );
            scheduleRepeating(plugin, gui, newConfig, update);
            logDebug("Changed update period for GUI: " + gui.getClass().getSimpleName() +
                    " to: " + newPeriod);
        } else {
            logDebug("No existing config found to change period for GUI: " + gui.getClass().getSimpleName());
        }
    }

    // === CONDITIONAL AND BULK OPERATIONS ===

    /**
     * Schedules conditional updates that only execute when a predicate is true.
     * The condition is evaluated on each update cycle before executing the update.
     * 
     * @param plugin the plugin scheduling the updates
     * @param gui the GUI to update
     * @param period the update period in ticks
     * @param condition the condition that must be true for updates to execute
     * @param update the function to execute when condition is true
     */
    public static void scheduleConditional(Plugin plugin, GUI gui, long period,
                                           Predicate<GUI> condition, Consumer<GUI> update) {
        if (condition == null) {
            logDebug("Null condition provided for conditional update");
            return;
        }

        scheduleRepeating(plugin, gui, period, g -> {
            try {
                if (condition.test(g)) {
                    update.accept(g);
                } else {
                    logDebug("Condition failed for GUI: " + g.getClass().getSimpleName());
                }
            } catch (Exception e) {
                logError("Error in condition evaluation for " + g.getClass().getSimpleName(), e);
            }
        });
    }

    /**
     * Schedules the same update function for multiple GUIs.
     * Each GUI will have its own independent update schedule.
     * 
     * @param plugin the plugin scheduling the updates
     * @param period the update period in ticks for all GUIs
     * @param update the function to execute for each GUI
     * @param guis the GUIs to schedule updates for
     */
    public static void scheduleMultiple(Plugin plugin, long period, Consumer<GUI> update, GUI... guis) {
        if (guis == null || guis.length == 0) {
            logDebug("No GUIs provided for multiple scheduling");
            return;
        }

        for (GUI gui : guis) {
            if (gui != null) {
                scheduleRepeating(plugin, gui, period, update);
            }
        }
        logDebug("Scheduled updates for " + guis.length + " GUIs");
    }

    /**
     * Cancels updates for multiple GUIs at once.
     * 
     * @param guis the GUIs to cancel updates for
     */
    public static void cancelMultiple(GUI... guis) {
        if (guis == null || guis.length == 0) {
            logDebug("No GUIs provided for multiple cancellation");
            return;
        }

        int cancelledCount = 0;
        for (GUI gui : guis) {
            if (gui != null && isScheduled(gui)) {
                cancel(gui);
                cancelledCount++;
            }
        }
        logDebug("Cancelled updates for " + cancelledCount + " GUIs");
    }

    // === TASK CANCELLATION ===

    /**
     * Cancels all updates for a specific GUI.
     * Removes the GUI from both the task and configuration maps.
     * 
     * @param gui the GUI to cancel updates for
     */
    public static void cancel(GUI gui) {
        if (gui == null) {
            logDebug("Null GUI provided for cancellation");
            return;
        }

        BukkitTask task = tasks.remove(gui);
        if (task != null && !task.isCancelled()) {
            task.cancel();
            logDebug("Cancelled update for GUI: " + gui.getClass().getSimpleName());
        }
        configs.remove(gui);
    }

    /**
     * Cancels all scheduled GUI updates.
     * This should be called when the plugin is being disabled to clean up resources.
     */
    public static void cancelAll() {
        int taskCount = tasks.size();
        logDebug("Cancelling all GUI updates (" + taskCount + " tasks)");

        for (BukkitTask task : tasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        tasks.clear();
        configs.clear();

        logDebug("Cancelled " + taskCount + " GUI update tasks");
    }

    // === STATUS AND MONITORING ===

    /**
     * Checks if a GUI has scheduled updates.
     * 
     * @param gui the GUI to check
     * @return true if the GUI has active updates scheduled, false otherwise
     */
    public static boolean isScheduled(GUI gui) {
        return gui != null && tasks.containsKey(gui);
    }

    /**
     * Gets the number of GUIs with active updates.
     * 
     * @return the count of active update tasks
     */
    public static int getActiveUpdateCount() {
        return tasks.size();
    }

    /**
     * Gets the update configuration for a specific GUI.
     * 
     * @param gui the GUI to get configuration for
     * @return the GUIUpdateConfig, or null if not found
     */
    public static GUIUpdateConfig getConfig(GUI gui) {
        return gui != null ? configs.get(gui) : null;
    }

    /**
     * Gets the timestamp of the last successful update for a GUI.
     * 
     * @param gui the GUI to check
     * @return the last update timestamp in milliseconds, or 0 if never updated
     */
    public static long getLastUpdateTime(GUI gui) {
        GUIUpdateConfig config = getConfig(gui);
        return config != null ? config.getLastUpdate() : 0;
    }

    /**
     * Gets the time elapsed since the last successful update for a GUI.
     * 
     * @param gui the GUI to check
     * @return milliseconds since last update, or -1 if never updated
     */
    public static long getTimeSinceLastUpdate(GUI gui) {
        long lastUpdate = getLastUpdateTime(gui);
        return lastUpdate > 0 ? System.currentTimeMillis() - lastUpdate : -1;
    }

    /**
     * Enables or disables debug logging for the updater.
     * Debug logs provide detailed information about update scheduling and execution.
     * 
     * @param debug true to enable debug logging, false to disable
     */
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
        logDebug("Debug mode " + (debug ? "enabled" : "disabled"));
    }

    /**
     * Checks if debug mode is currently enabled.
     * 
     * @return true if debug mode is enabled, false otherwise
     */
    public static boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Prints comprehensive status information about all scheduled updates.
     * Useful for monitoring and debugging GUI update performance.
     */
    public static void printStatus() {
        System.out.println("=== GUI Updater Status ===");
        System.out.println("Active updates: " + getActiveUpdateCount());
        System.out.println("Debug mode: " + debugMode);

        if (configs.isEmpty()) {
            System.out.println("No active GUI updates");
        } else {
            for (Map.Entry<GUI, GUIUpdateConfig> entry : configs.entrySet()) {
                GUI gui = entry.getKey();
                GUIUpdateConfig config = entry.getValue();
                long timeSinceUpdate = getTimeSinceLastUpdate(gui);

                System.out.println("- " + gui.getClass().getSimpleName() + ":");
                System.out.println("  Period: " + config.getPeriod() + " ticks");
                System.out.println("  Paused: " + config.isPaused());
                System.out.println("  Viewers: " + gui.getInventory().getViewers().size());
                System.out.println("  Retries: " + config.getCurrentRetries() + "/" + config.getMaxRetries());
                System.out.println("  Last update: " + (timeSinceUpdate >= 0 ? timeSinceUpdate + "ms ago" : "Never"));
                System.out.println("  Async: " + config.isAsyncUpdate());
                System.out.println("  Stop when empty: " + config.shouldStopWhenEmpty());
            }
        }
        System.out.println("========================");
    }

    // === PRIVATE HELPER METHODS ===

    /**
     * Executes a synchronous update for a GUI.
     */
    private static void runUpdateSync(GUI gui, Consumer<GUI> update, GUIUpdateConfig config) {
        update.accept(gui);
        config.updateLastUpdate();
        config.resetRetries();
        logDebug("Executed sync update for GUI: " + gui.getClass().getSimpleName());
    }

    /**
     * Executes an asynchronous update for a GUI.
     */
    private static void runUpdateAsync(Plugin plugin, GUI gui, Consumer<GUI> update, GUIUpdateConfig config) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                update.accept(gui);
                config.updateLastUpdate();
                config.resetRetries();
                logDebug("Executed async update for GUI: " + gui.getClass().getSimpleName());
            } catch (Exception e) {
                logError("Error in async update execution for " + gui.getClass().getSimpleName(), e);
            }
        });
    }

    /**
     * Handles errors that occur during GUI updates.
     */
    private static void handleUpdateError(GUI gui, GUIUpdateConfig config, Exception e) {
        config.incrementRetries();
        logError("Error updating GUI " + gui.getClass().getSimpleName() +
                " (retry " + config.getCurrentRetries() + "/" + config.getMaxRetries() + ")", e);

        if (config.getCurrentRetries() >= config.getMaxRetries()) {
            logError("Max retries reached for GUI " + gui.getClass().getSimpleName() + ", cancelling updates", null);
            cancel(gui);
        }
    }

    /**
     * Logs debug messages when debug mode is enabled.
     */
    private static void logDebug(String message) {
        if (debugMode) {
            System.out.println("[GUIUpdater DEBUG] " + message);
        }
    }

    /**
     * Logs error messages to the console.
     */
    private static void logError(String message, Exception e) {
        System.err.println("[GUIUpdater ERROR] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}
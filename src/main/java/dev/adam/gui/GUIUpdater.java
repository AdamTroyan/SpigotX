package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

public class GUIUpdater {
    private static final Map<GUI, BukkitTask> tasks = new ConcurrentHashMap<>();
    private static final Map<GUI, GUIUpdateConfig> configs = new ConcurrentHashMap<>();
    private static boolean debugMode = false;

    /**
     * Configuration for GUI updates
     */
    public static class GUIUpdateConfig {
        private final long period;
        private final boolean stopWhenEmpty;
        private final boolean asyncUpdate;
        private final int maxRetries;
        private int currentRetries = 0;
        private long lastUpdate = 0;
        private boolean paused = false;

        public GUIUpdateConfig(long period) {
            this(period, true, false, 3);
        }

        public GUIUpdateConfig(long period, boolean stopWhenEmpty, boolean asyncUpdate, int maxRetries) {
            this.period = period;
            this.stopWhenEmpty = stopWhenEmpty;
            this.asyncUpdate = asyncUpdate;
            this.maxRetries = maxRetries;
        }

        // Getters
        public long getPeriod() { return period; }
        public boolean shouldStopWhenEmpty() { return stopWhenEmpty; }
        public boolean isAsyncUpdate() { return asyncUpdate; }
        public int getMaxRetries() { return maxRetries; }
        public int getCurrentRetries() { return currentRetries; }
        public long getLastUpdate() { return lastUpdate; }
        public boolean isPaused() { return paused; }

        // Internal methods
        void incrementRetries() { currentRetries++; }
        void resetRetries() { currentRetries = 0; }
        void updateLastUpdate() { lastUpdate = System.currentTimeMillis(); }
        void setPaused(boolean paused) { this.paused = paused; }
    }

    // **EXISTING METHODS** (enhanced)
    public static void scheduleRepeating(Plugin plugin, GUI gui, long period, Consumer<GUI> update) {
        scheduleRepeating(plugin, gui, new GUIUpdateConfig(period), update);
    }

    public static void scheduleRepeating(Plugin plugin, GUI gui, GUIUpdateConfig config, Consumer<GUI> update) {
        if (plugin == null || gui == null || update == null || config == null) {
            logDebug("Invalid parameters for scheduleRepeating");
            return;
        }

        cancel(gui);
        configs.put(gui, config);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                // Check if paused
                if (config.isPaused()) {
                    logDebug("Update paused for GUI: " + gui.getClass().getSimpleName());
                    return;
                }

                // Check if should stop when empty
                if (config.shouldStopWhenEmpty() && gui.getInventory().getViewers().isEmpty()) {
                    logDebug("Stopping update for empty GUI: " + gui.getClass().getSimpleName());
                    cancel(gui);
                    return;
                }

                // Perform update
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

    // **NEW ADVANCED METHODS**

    /**
     * Schedule a one-time delayed update
     */
    public static void scheduleDelayed(Plugin plugin, GUI gui, long delay, Consumer<GUI> update) {
        if (plugin == null || gui == null || update == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                if (!gui.getInventory().getViewers().isEmpty()) {
                    update.accept(gui);
                    logDebug("Executed delayed update for GUI: " + gui.getClass().getSimpleName());
                }
            } catch (Exception e) {
                logError("Error in delayed update", e);
            }
        }, delay);
    }

    /**
     * Schedule async update
     */
    public static void scheduleAsync(Plugin plugin, GUI gui, Consumer<GUI> update) {
        if (plugin == null || gui == null || update == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                update.accept(gui);
                logDebug("Executed async update for GUI: " + gui.getClass().getSimpleName());
            } catch (Exception e) {
                logError("Error in async update", e);
            }
        });
    }

    /**
     * Pause/Resume updates
     */
    public static void pauseUpdates(GUI gui) {
        GUIUpdateConfig config = configs.get(gui);
        if (config != null) {
            config.setPaused(true);
            logDebug("Paused updates for GUI: " + gui.getClass().getSimpleName());
        }
    }

    public static void resumeUpdates(GUI gui) {
        GUIUpdateConfig config = configs.get(gui);
        if (config != null) {
            config.setPaused(false);
            logDebug("Resumed updates for GUI: " + gui.getClass().getSimpleName());
        }
    }

    public static boolean isPaused(GUI gui) {
        GUIUpdateConfig config = configs.get(gui);
        return config != null && config.isPaused();
    }

    /**
     * Update period modification
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
        }
    }

    /**
     * Conditional updates
     */
    public static void scheduleConditional(Plugin plugin, GUI gui, long period, 
                                         java.util.function.Predicate<GUI> condition, 
                                         Consumer<GUI> update) {
        scheduleRepeating(plugin, gui, period, g -> {
            if (condition.test(g)) {
                update.accept(g);
            }
        });
    }

    /**
     * Batch updates for multiple GUIs
     */
    public static void scheduleMultiple(Plugin plugin, long period, Consumer<GUI> update, GUI... guis) {
        for (GUI gui : guis) {
            scheduleRepeating(plugin, gui, period, update);
        }
    }

    public static void cancelMultiple(GUI... guis) {
        for (GUI gui : guis) {
            cancel(gui);
        }
    }

    /**
     * Enhanced cancel with cleanup
     */
    public static void cancel(GUI gui) {
        BukkitTask task = tasks.remove(gui);
        if (task != null) {
            task.cancel();
            logDebug("Cancelled update for GUI: " + gui.getClass().getSimpleName());
        }
        configs.remove(gui);
    }

    public static void cancelAll() {
        logDebug("Cancelling all GUI updates (" + tasks.size() + " tasks)");
        for (BukkitTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        configs.clear();
    }

    /**
     * Status and monitoring
     */
    public static boolean isScheduled(GUI gui) {
        return tasks.containsKey(gui);
    }

    public static int getActiveUpdateCount() {
        return tasks.size();
    }

    public static GUIUpdateConfig getConfig(GUI gui) {
        return configs.get(gui);
    }

    public static long getLastUpdateTime(GUI gui) {
        GUIUpdateConfig config = configs.get(gui);
        return config != null ? config.getLastUpdate() : 0;
    }

    public static long getTimeSinceLastUpdate(GUI gui) {
        long lastUpdate = getLastUpdateTime(gui);
        return lastUpdate > 0 ? System.currentTimeMillis() - lastUpdate : -1;
    }

    /**
     * Debug and logging
     */
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
        logDebug("Debug mode " + (debug ? "enabled" : "disabled"));
    }

    public static void printStatus() {
        System.out.println("=== GUI Updater Status ===");
        System.out.println("Active updates: " + getActiveUpdateCount());
        for (Map.Entry<GUI, GUIUpdateConfig> entry : configs.entrySet()) {
            GUI gui = entry.getKey();
            GUIUpdateConfig config = entry.getValue();
            System.out.println("- " + gui.getClass().getSimpleName() + 
                             ": period=" + config.getPeriod() + 
                             ", paused=" + config.isPaused() + 
                             ", viewers=" + gui.getInventory().getViewers().size());
        }
        System.out.println("========================");
    }

    // **PRIVATE HELPER METHODS**

    private static void runUpdateSync(GUI gui, Consumer<GUI> update, GUIUpdateConfig config) {
        update.accept(gui);
        config.updateLastUpdate();
        config.resetRetries();
        logDebug("Executed sync update for GUI: " + gui.getClass().getSimpleName());
    }

    private static void runUpdateAsync(Plugin plugin, GUI gui, Consumer<GUI> update, GUIUpdateConfig config) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                update.accept(gui);
                config.updateLastUpdate();
                config.resetRetries();
                logDebug("Executed async update for GUI: " + gui.getClass().getSimpleName());
            } catch (Exception e) {
                logError("Error in async update execution", e);
            }
        });
    }

    private static void handleUpdateError(GUI gui, GUIUpdateConfig config, Exception e) {
        config.incrementRetries();
        logError("Error updating GUI " + gui.getClass().getSimpleName() + 
                " (retry " + config.getCurrentRetries() + "/" + config.getMaxRetries() + ")", e);

        if (config.getCurrentRetries() >= config.getMaxRetries()) {
            logError("Max retries reached for GUI " + gui.getClass().getSimpleName() + ", cancelling updates", null);
            cancel(gui);
        }
    }

    private static void logDebug(String message) {
        if (debugMode) {
            System.out.println("[GUIUpdater DEBUG] " + message);
        }
    }

    private static void logError(String message, Exception e) {
        System.err.println("[GUIUpdater ERROR] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}
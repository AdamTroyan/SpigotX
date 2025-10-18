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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIListener implements Listener {
    private static boolean registered = false;
    private static final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> clickCounts = new ConcurrentHashMap<>();
    private static long clickCooldown = 50; // milliseconds
    private static int maxClicksPerSecond = 20;
    private static boolean debugMode = false;
    private static final Map<UUID, String> lastGUITypes = new HashMap<>();

    public static void ensureRegistered() {
        if (!registered) {
            Plugin plugin = SpigotX.getPlugin();
            plugin.getServer().getPluginManager().registerEvents(new GUIListener(), plugin);
            registered = true;
            logDebug("GUIListener registered successfully");
        }
    }

    public static void setClickCooldown(long cooldownMs) {
        clickCooldown = cooldownMs;
        logDebug("Click cooldown set to: " + cooldownMs + "ms");
    }

    public static void setMaxClicksPerSecond(int maxClicks) {
        maxClicksPerSecond = maxClicks;
        logDebug("Max clicks per second set to: " + maxClicks);
    }

    public static void setDebugMode(boolean debug) {
        debugMode = debug;
        logDebug("Debug mode " + (debug ? "enabled" : "disabled"));
    }

    private boolean isOurGui(Inventory inv) {
        return inv != null && inv.getHolder() instanceof GUIBase;
    }

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
        
        lastGUITypes.put(player.getUniqueId(), gui.getClass().getSimpleName());
        
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isOurGui(event.getInventory())) {
            event.setCancelled(true);
            logDebug("Blocked drag event for " + event.getWhoClicked().getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isOurGui(event.getSource()) || isOurGui(event.getDestination())) {
            event.setCancelled(true);
            logDebug("Blocked move event between inventories");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!isOurGui(event.getInventory())) return;
        
        Player player = (Player) event.getPlayer();
        GUIBase gui = (GUIBase) event.getInventory().getHolder();
        
        logDebug("Processing close event for " + player.getName() + 
                " in " + gui.getClass().getSimpleName());
        
        try {
            gui.handleClose(event);
            
            UUID uuid = player.getUniqueId();
            lastClickTime.remove(uuid);
            clickCounts.remove(uuid);
            lastGUITypes.remove(uuid);
            
            logDebug("Successfully handled close for " + player.getName());
        } catch (Exception e) {
            logError("Error handling close for " + player.getName() + 
                    " in " + gui.getClass().getSimpleName(), e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!isOurGui(event.getInventory())) return;
        
        Player player = (Player) event.getPlayer();
        GUIBase gui = (GUIBase) event.getInventory().getHolder();
        
        logDebug("GUI opened: " + gui.getClass().getSimpleName() + 
                " by " + player.getName());
        
        lastGUITypes.put(player.getUniqueId(), gui.getClass().getSimpleName());
        
        UUID uuid = player.getUniqueId();
        lastClickTime.remove(uuid);
        clickCounts.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        lastClickTime.remove(uuid);
        clickCounts.remove(uuid);
        lastGUITypes.remove(uuid);
        
        logDebug("Cleaned up data for disconnected player: " + event.getPlayer().getName());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(SpigotX.getPlugin())) {
            lastClickTime.clear();
            clickCounts.clear();
            lastGUITypes.clear();
            GUIUpdater.cancelAll();
            
            logDebug("Plugin disabled - cleaned up all GUI data");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (isOurGui(event.getInventory())) {
            event.setCancelled(true);
            logDebug("Blocked creative event for " + event.getWhoClicked().getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryInteract(InventoryInteractEvent event) {
        if (isOurGui(event.getInventory())) {
            event.setCancelled(true);
            logDebug("Blocked interact event for " + event.getWhoClicked().getName());
        }
    }

    public static Map<String, Integer> getClickStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<UUID, String> entry : lastGUITypes.entrySet()) {
            String guiType = entry.getValue();
            stats.put(guiType, stats.getOrDefault(guiType, 0) + 1);
        }
        return stats;
    }

    public static void printStatistics() {
        System.out.println("=== GUI Listener Statistics ===");
        System.out.println("Active GUI sessions: " + lastGUITypes.size());
        System.out.println("Players with click tracking: " + lastClickTime.size());
        
        Map<String, Integer> guiStats = getClickStatistics();
        if (!guiStats.isEmpty()) {
            System.out.println("GUI types in use:");
            for (Map.Entry<String, Integer> entry : guiStats.entrySet()) {
                System.out.println("- " + entry.getKey() + ": " + entry.getValue() + " players");
            }
        }
        System.out.println("==============================");
    }

    public static void clearTrackingData() {
        lastClickTime.clear();
        clickCounts.clear();
        lastGUITypes.clear();
        logDebug("Cleared all tracking data");
    }

    private static void logDebug(String message) {
        if (debugMode) {
            System.out.println("[GUIListener DEBUG] " + message);
        }
    }

    private static void logError(String message, Exception e) {
        System.err.println("[GUIListener ERROR] " + message);
        if (e != null) {
            e.printStackTrace();
        }
    }
}
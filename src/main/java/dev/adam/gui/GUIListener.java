package dev.adam.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import dev.adam.SpigotX;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class GUIListener implements Listener {
    private static final Map<Player, GUI> openGuis = new ConcurrentHashMap<>();
    private static boolean registered = false;

    public static void openGui(Player player, GUI gui) {
        openGuis.put(player, gui);

        gui.open(player);
    }

    private boolean isOurGui(Inventory inv){
        return inv.getHolder() instanceof GUIBase;
    }

    public static void closeGui(Player player) {
        openGuis.remove(player);
    }

    public static GUI getOpenGui(Player player) {
        return openGuis.get(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        GUI gui = openGuis.get(player);

        if (gui != null && event.getInventory().equals(gui.getInventory())) {
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        GUI gui = openGuis.get(player);

        if (gui != null && event.getInventory().equals(gui.getInventory())) {
            gui.handleClose(event);

            openGuis.remove(player);

            GUIUpdater.cancel(gui);
        }
    }

    public static void ensureRegistered() {
        if (!registered) {
            Plugin plugin = SpigotX.getPlugin();
            plugin.getServer().getPluginManager().registerEvents(new GUIListener(), plugin);
            registered = true;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isOurGui(event.getInventory())) return;

        event.setCancelled(true);

        if (event.getRawSlot() >= 0 && event.getRawSlot() < event.getInventory().getSize()) {
            GUIBase gui = (GUIBase) event.getInventory().getHolder();
            if (gui != null && gui.hasHandler(event.getSlot())) {
                gui.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isOurGui(event.getInventory())) {
            event.setCancelled(true);
        }
    }
}

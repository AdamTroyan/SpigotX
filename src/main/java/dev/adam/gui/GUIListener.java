package dev.adam.gui;

import dev.adam.SpigotX;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class GUIListener implements Listener {
    private static boolean registered = false;

    public static void ensureRegistered() {
        if (!registered) {
            Plugin plugin = SpigotX.getPlugin();
            plugin.getServer().getPluginManager().registerEvents(new GUIListener(), plugin);
            registered = true;
        }
    }

    private boolean isOurGui(Inventory inv) {
        return inv.getHolder() instanceof GUIBase;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isOurGui(event.getInventory())) return;
        event.setCancelled(true);

        GUIBase gui = (GUIBase) event.getInventory().getHolder();
        if (gui.hasHandler(event.getSlot())) {
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isOurGui(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!isOurGui(event.getInventory())) return;
        GUIBase gui = (GUIBase) event.getInventory().getHolder();
        gui.handleClose(event);
    }
}
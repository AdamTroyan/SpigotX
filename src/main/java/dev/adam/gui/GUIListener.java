package dev.adam.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class GUIListener implements Listener {
    private static final Map<Player, GUI> openGuis = new ConcurrentHashMap<>();

    public static void openGui(Player player, GUI gui) {
        openGuis.put(player, gui);
        
        gui.open(player);
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
}
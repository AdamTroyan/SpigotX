package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GUI {
    private final Inventory inventory;
    private final Map<Integer, GUIButton> buttons = new HashMap<>();
    private final Plugin plugin;

    public GUI(Plugin plugin, String title, int rows) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(null, rows * 9, title);
        registerListeners();
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onClick(InventoryClickEvent e) {
                if (e.getInventory().equals(inventory)) {
                    e.setCancelled(true);
                    GUIButton button = buttons.get(e.getRawSlot());
                    if (button != null && e.getWhoClicked() instanceof Player player) {
                        button.click(player);
                    }
                }
            }

            @EventHandler
            public void onClose(InventoryCloseEvent e) {

            }
        }, plugin);
    }

    public void setItem(int slot, ItemStack item, Consumer<Player> action) {
        inventory.setItem(slot, item);
        buttons.put(slot, new GUIButton(slot, action));
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public void clear() {
        inventory.clear();
        buttons.clear();
    }
}

package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import dev.adam.gui.context.GUIClickContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GUI implements GUIBase {
    private final String title;
    private final int rows;
    private final Inventory inventory;
    private final Map<Integer, Consumer<GUIClickContext>> clickHandlers = new HashMap<>();
    private Consumer<Player> onOpen, onClose;

    public GUI(String title, int rows) {
        this.title = title;
        this.rows = rows;
        this.inventory = Bukkit.createInventory(null, rows * 9, title);
    }

    public void setItem(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        inventory.setItem(slot, item);
        
        if (onClick != null) clickHandlers.put(slot, onClick);
    }

    public void fillBorder(ItemStack item, Consumer<GUIClickContext> onClick) {
        int size = rows * 9;

        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                setItem(i, item, onClick);
            }
        }
    }

    public void setOnOpen(Consumer<Player> onOpen) { this.onOpen = onOpen; }
    public void setOnClose(Consumer<Player> onClose) { this.onClose = onClose; }

    public void open(Player player) {
        player.openInventory(inventory);

        if (onOpen != null) onOpen.accept(player);
    }

    public void close(Player player) {
        player.closeInventory();

        if (onClose != null) onClose.accept(player);
    }

    public Inventory getInventory() { return inventory; }

    public void handleClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        int slot = event.getRawSlot();

        if (clickHandlers.containsKey(slot)) {
            event.setCancelled(true);

            clickHandlers.get(slot).accept(new GUIClickContext(event));
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        if (onClose != null && event.getInventory().equals(inventory)) {
            onClose.accept((Player) event.getPlayer());
        }
    }

        public boolean hasHandler(int slot) {
        return clickHandlers.containsKey(slot);
    }

    public Consumer<GUIClickContext> getHandler(int slot) {
        return clickHandlers.get(slot);
    }

    public void addHandler(int slot, Consumer<GUIClickContext> handler) {
        if (handler != null) clickHandlers.put(slot, handler);
    }
}
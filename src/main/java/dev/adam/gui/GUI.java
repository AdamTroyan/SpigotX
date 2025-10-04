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
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    public void setItem(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        if (slot < 0 || slot >= inventory.getSize() || item == null) return;
        inventory.setItem(slot, item);
        if (onClick != null) clickHandlers.put(slot, onClick);
        else clickHandlers.remove(slot);
    }

    public void removeItem(int slot) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        inventory.setItem(slot, null);
        clickHandlers.remove(slot);
    }

    public Consumer<GUIClickContext> getHandler(int slot) {
        return clickHandlers.get(slot);
    }

    public void removeHandler(int slot) {
        clickHandlers.remove(slot);
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
        if (player == null) return;
        player.openInventory(inventory);
        if (onOpen != null) onOpen.accept(player);
    }

    public void close(Player player) {
        if (player == null) return;
        player.closeInventory();
        if (onClose != null) onClose.accept(player);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    @Override
    public boolean hasHandler(int slot) {
        return clickHandlers.containsKey(slot);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        int slot = event.getRawSlot();
        Consumer<GUIClickContext> handler = clickHandlers.get(slot);
        if (handler != null) {
            event.setCancelled(true);
            try {
                handler.accept(new GUIClickContext(event));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (onClose != null && event.getInventory().equals(inventory)) {
            Player player = (Player) event.getPlayer();
            onClose.accept(player);
        }
    }
}
package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    public void fillRowIfEmpty(int row, ItemStack item, Consumer<GUIClickContext> onClick) {
        int start = (row - 1) * 9;
        int end = start + 9;
        for (int i = start; i < end; i++) {
            if (inventory.getItem(i) == null) setItem(i, item, onClick);
        }
    }

    public void fillColumnIfEmpty(int col, ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int i = col; i < inventory.getSize(); i += 9) {
            if (inventory.getItem(i) == null) setItem(i, item, onClick);
        }
    }

    public void clearRow(int row) {
        int start = (row - 1) * 9;
        for (int i = start; i < start + 9; i++) removeItem(i);
    }

    public void clearColumn(int col) {
        for (int i = col; i < inventory.getSize(); i += 9) removeItem(i);
    }

    public void setItemsBulk(int[] slots, ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int slot : slots) setItem(slot, item, onClick);
    }

    public void fillBorderIfEmpty(ItemStack item, Consumer<GUIClickContext> onClick) {
        int size = inventory.getSize();
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                if (inventory.getItem(i) == null) setItem(i, item, onClick);
            }
        }
    }

    public void replaceItem(Material from, ItemStack to, Consumer<GUIClickContext> onClick) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack current = inventory.getItem(i);
            if (current != null && current.getType() == from) setItem(i, to, onClick);
        }
    }

    public int getFirstEmptySlotInRow(int row) {
        int start = (row - 1) * 9;
        for (int i = start; i < start + 9; i++) {
            if (inventory.getItem(i) == null) return i;
        }
        return -1;
    }

    public void setItemIfEmpty(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        if (inventory.getItem(slot) == null) setItem(slot, item, onClick);
    }

    public void setRow(int row, ItemStack[] items, Consumer<GUIClickContext>[] handlers) {
        int start = (row - 1) * 9;
        for (int i = 0; i < 9 && i < items.length; i++) {
            setItem(start + i, items[i], handlers != null && i < handlers.length ? handlers[i] : null);
        }
    }

    public void setBackground(ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) setItem(i, item, onClick);
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
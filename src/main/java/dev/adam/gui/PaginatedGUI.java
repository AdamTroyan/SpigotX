package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import dev.adam.gui.context.GUIClickContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PaginatedGUI implements GUIBase {
    private final Inventory inventory;
    private final Map<Integer, Consumer<GUIClickContext>> handlers = new HashMap<>();

    static {
        GUIListener.ensureRegistered();
    }

    public PaginatedGUI(String title, int rows) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    public void setContent(List<ItemStack> items) {
        for (int i = 0; i < items.size() && i < inventory.getSize(); i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    public void setItemHandler(int slot, Consumer<GUIClickContext> handler) {
        if (handler != null) handlers.put(slot, handler);
    }

    public void setPrevItem(ItemStack item) {
        inventory.setItem(45, item);
    }

    public void setNextItem(ItemStack item) {
        inventory.setItem(53, item);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasHandler(int slot) {
        return handlers.containsKey(slot);
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        Consumer<GUIClickContext> handler = handlers.get(event.getSlot());
        if (handler != null) {
            handler.accept(new GUIClickContext(event));
        }
    }
}
package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import dev.adam.gui.context.GUIClickContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GUIBuilder implements GUIBase {
    private final Inventory inventory;
    private final Map<Integer, Consumer<GUIClickContext>> handlers = new HashMap<>();

    static {
        GUIListener.ensureRegistered();
    }

    public GUIBuilder(String title, int rows) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    public GUIBuilder setItem(int slot, ItemStack item, Consumer<GUIClickContext> handler) {
        inventory.setItem(slot, item);
        if (handler != null) handlers.put(slot, handler);
        return this;
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
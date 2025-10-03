package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import dev.adam.gui.context.GUIClickContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GUIBuilder implements GUIBase {
    private final GUI gui;
    private final Map<Integer, String> slotPermissions = new HashMap<>();

    static {
        GUIListener.ensureRegistered();
    }

    public GUIBuilder(String title, int rows) {
        this.gui = new GUI(title, rows);
    }

    public GUIBuilder setItem(int slot, ItemStack item, Consumer<GUIClickContext> handler) {
        return setItem(slot, item, null, handler);
    }

    public GUIBuilder setItem(int slot, ItemStack item, String permission, Consumer<GUIClickContext> handler) {
        gui.setItem(slot, item, handler);
        if (permission != null && !permission.isEmpty()) slotPermissions.put(slot, permission);
        return this;
    }

    public void open(Player player) {
        gui.open(player);
    }

    public GUI build() {
        return gui;
    }

    @Override
    public Inventory getInventory() {
        return gui.getInventory();
    }

    @Override
    public boolean hasHandler(int slot) {
        return gui.hasHandler(slot);
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();
        
        if (slotPermissions.containsKey(slot) && !player.hasPermission(slotPermissions.get(slot))) {
            return;
        }

        gui.handleClick(event);
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        gui.handleClose(event);
    }
}
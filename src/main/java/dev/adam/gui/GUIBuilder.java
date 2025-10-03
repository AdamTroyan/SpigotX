package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.units.qual.g;

import dev.adam.gui.context.GUIClickContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GUIBuilder implements GUIBase {
    private final GUI gui;
    private final Map<Integer, String> slotPermissions = new HashMap<>();
    private final Map<Integer, Consumer<GUIClickContext>> handlers = new HashMap<>();
    private final Inventory inventory;

    static {
        GUIListener.ensureRegistered();
    }
    
    public GUIBuilder(String title, int rows) {
        this.gui = new GUI(title, rows);
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    public GUIBuilder setItem(int slot, ItemStack item, Consumer<GUIClickContext> handler) {
        return setItem(slot, item, null, handler);
    }

    public GUIBuilder setItem(int slot, ItemStack item, String permission, Consumer<GUIClickContext> handler) {
        if (item.getItemMeta() == null || item.getItemMeta().getDisplayName() == null || item.getItemMeta().getDisplayName().isEmpty()) {
            throw new IllegalArgumentException("Every GUI item must have a display name!");
        }
        gui.getInventory().setItem(slot, item);
        if (handler != null) handlers.put(slot, handler);
        if (permission != null && !permission.isEmpty()) slotPermissions.put(slot, permission);
        return this;
    }

    public GUIBuilder fillBorder(ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.fillBorder(item, onClick);
        return this;
    }

    public GUIBuilder onOpen(Consumer<Player> onOpen) {
        gui.setOnOpen(onOpen);
        return this;
    }

    public GUIBuilder onClose(Consumer<Player> onClose) {
        gui.setOnClose(onClose);
        return this;
    }

    public GUI build() {
        return gui;
    }

    public void open(Player player) {
        GUIListener.openGui(player, gui);
    }

    @Override
    public Inventory getInventory() {
        return gui.getInventory();
    }

    @Override
    public boolean hasHandler(int slot) {
        return handlers.containsKey(slot);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();
        if (slotPermissions.containsKey(slot) && !player.hasPermission(slotPermissions.get(slot))) {
            return;
        }

        Consumer<GUIClickContext> handler = handlers.get(slot);
        if (handler != null) {
            handler.accept(new GUIClickContext(event));
        }
    }
}
package dev.adam.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import dev.adam.gui.context.GUIClickContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GUIBuilder implements GUIBase {
    private final GUI gui;
    private final Map<Integer, String> slotPermissions = new HashMap<>();

    public GUIBuilder(String title, int rows) {
        this.gui = new GUI(title, rows);
    }

    public GUIBuilder setItem(int slot, ItemStack item, Consumer<GUIClickContext> handler) {
        return setItem(slot, item, null, handler);
    }

    public GUIBuilder setItem(int slot, ItemStack item, String permission, Consumer<GUIClickContext> handler) {
        if (item == null || item.getItemMeta() == null || item.getItemMeta().getDisplayName() == null) {
            throw new IllegalArgumentException("Every GUI item must have a display name!");
        }
        gui.setItem(slot, item, handler);
        if (permission != null && !permission.isEmpty()) slotPermissions.put(slot, permission);
        return this;
    }

    public GUIBuilder fillRowIfEmpty(int row, ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.fillRowIfEmpty(row, item, onClick);
        return this;
    }

    public GUIBuilder fillColumnIfEmpty(int col, ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.fillColumnIfEmpty(col, item, onClick);
        return this;
    }

    public GUIBuilder clearRow(int row) {
        gui.clearRow(row);
        return this;
    }

    public GUIBuilder clearColumn(int col) {
        gui.clearColumn(col);
        return this;
    }

    public GUIBuilder setItemsBulk(int[] slots, ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.setItemsBulk(slots, item, onClick);
        return this;
    }

    public GUIBuilder fillBorderIfEmpty(ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.fillBorderIfEmpty(item, onClick);
        return this;
    }

    public GUIBuilder replaceItem(Material from, ItemStack to, Consumer<GUIClickContext> onClick) {
        gui.replaceItem(from, to, onClick);
        return this;
    }

    public int getFirstEmptySlotInRow(int row) {
        return gui.getFirstEmptySlotInRow(row);
    }

    public GUIBuilder setItemIfEmpty(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.setItemIfEmpty(slot, item, onClick);
        return this;
    }

    public GUIBuilder setRow(int row, ItemStack[] items, Consumer<GUIClickContext>[] handlers) {
        gui.setRow(row, items, handlers);
        return this;
    }

    public GUIBuilder setBackground(ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.setBackground(item, onClick);
        return this;
    }

    public GUIBuilder removeItem(int slot) {
        gui.removeItem(slot);
        slotPermissions.remove(slot);
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
            player.sendMessage("§cאין לך הרשאה ללחוץ על כפתור זה.");
            return;
        }
        gui.handleClick(event);
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        gui.handleClose(event);
    }
}
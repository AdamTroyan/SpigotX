package dev.adam.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface GUIBase extends InventoryHolder {
    default boolean hasHandler(int slot) { return false; }
    default void handleClick(InventoryClickEvent event) {}
    default void handleClose(InventoryCloseEvent event) {}
    @Override
    Inventory getInventory();
}
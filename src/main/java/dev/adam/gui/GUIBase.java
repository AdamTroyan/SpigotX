package dev.adam.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public abstract class GUIBase implements InventoryHolder {
    public abstract boolean hasHandler(int slot);
    
    public abstract void handleClick(InventoryClickEvent event);

    @Override
    public abstract Inventory getInventory();
}
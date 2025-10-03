package dev.adam.gui.context;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;

public class GUIClickContext {
    private final InventoryClickEvent event;

    public GUIClickContext(InventoryClickEvent event) {
        this.event = event;
    }

    public Player getPlayer() { return (Player) event.getWhoClicked(); }
    public int getSlot() { return event.getRawSlot(); }
    public InventoryClickEvent getEvent() { return event; }
}
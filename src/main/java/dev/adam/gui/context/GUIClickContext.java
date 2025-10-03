package dev.adam.gui.context;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GUIClickContext {
    private final InventoryClickEvent event;

    public GUIClickContext(InventoryClickEvent event) {
        this.event = event;
    }

    public Player getPlayer() {
        return (Player) event.getWhoClicked();
    }

    public int getSlot() {
        return event.getRawSlot();
    }

    public ItemStack getClickedItem() {
        return event.getCurrentItem();
    }

    public InventoryClickEvent getEvent() {
        return event;
    }

    public void cancel() {
        event.setCancelled(true);
    }

    public void sendMessage(String msg) {
        getPlayer().sendMessage(msg);
    }
}
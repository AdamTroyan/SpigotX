package dev.adam.gui.context;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        return event.getSlot();
    }

    public ItemStack getClickedItem() {
        return event.getCurrentItem();
    }

    public void cancel() {
        event.setCancelled(true);
    }

    public void sendMessage(String msg) {
        getPlayer().sendMessage(msg);
    }

    public int getRawSlot() {
        return event.getRawSlot();
    }

    public ItemStack getCursor() {
        return event.getCursor();
    }

    public boolean isLeftClick() {
        return event.isLeftClick();
    }

    public boolean isRightClick() {
        return event.isRightClick();
    }

    public boolean isShiftClick() {
        return event.isShiftClick();
    }

    public void closeInventory() {
        getPlayer().closeInventory();
    }

    public InventoryClickEvent getEvent() {
        return event;
    }
}
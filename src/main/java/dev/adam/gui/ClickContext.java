package dev.adam.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class ClickContext {
    private final Player player;
    private final ItemStack clicked;
    private final ItemStack cursor;
    private final int slot;
    private final int rawSlot;
    private final boolean top;
    private final ClickType clickType;
    private final InventoryAction action;
    private final int hotbarButton;
    private final boolean shift;
    private final InventoryClickEvent event;
    private boolean cancelled;

    public ClickContext(Player player,
                        ItemStack clicked,
                        ItemStack cursor,
                        int slot,
                        int rawSlot,
                        boolean top,
                        ClickType clickType,
                        InventoryAction action,
                        int hotbarButton,
                        boolean shift,
                        InventoryClickEvent event,
                        boolean defaultCancel) {
        this.player = player;
        this.clicked = clicked;
        this.cursor = cursor;
        this.slot = slot;
        this.rawSlot = rawSlot;
        this.top = top;
        this.clickType = clickType;
        this.action = action;
        this.hotbarButton = hotbarButton;
        this.shift = shift;
        this.event = event;
        this.cancelled = defaultCancel;
    }

    public Player getPlayer() { return player; }
    public ItemStack getClicked() { return clicked; }
    public ItemStack getCursor() { return cursor; }
    public int getSlot() { return slot; }
    public int getRawSlot() { return rawSlot; }
    public boolean isTop() { return top; }
    public ClickType getClickType() { return clickType; }
    public InventoryAction getAction() { return action; }
    public int getHotbarButton() { return hotbarButton; }
    public boolean isShiftClick() { return shift; }
    public InventoryClickEvent getEvent() { return event; }

    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public boolean isCancelled() { return cancelled; }
}

package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import dev.adam.gui.context.GUIClickContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GUIBuilder implements GUIBase {
    private final Inventory inventory;
    private final Map<Integer, Consumer<GUIClickContext>> handlers = new HashMap<>();
    private final Map<Integer, String> slotPermissions = new HashMap<>();

    static {
        GUIListener.ensureRegistered();
    }

    public GUIBuilder(String title, int rows) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    public GUIBuilder setItem(int slot, ItemStack item, Consumer<GUIClickContext> handler) {
        return setItem(slot, item, null, handler);
    }

    public GUIBuilder setItem(int slot, ItemStack item, String permission, Consumer<GUIClickContext> handler) {
        inventory.setItem(slot, item);
        if (handler != null) handlers.put(slot, handler);
        if (permission != null && !permission.isEmpty()) slotPermissions.put(slot, permission);
        return this;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public GUIBase build() {
        return this;
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
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();
        if (slotPermissions.containsKey(slot) && !player.hasPermission(slotPermissions.get(slot))) {
            player.sendMessage("§cאין לך הרשאה ללחוץ על כפתור זה.");
            return;
        }
        Consumer<GUIClickContext> handler = handlers.get(slot);
        if (handler != null) {
            handler.accept(new GUIClickContext(event));
        }
    }
}
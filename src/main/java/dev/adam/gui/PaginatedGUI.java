package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import dev.adam.gui.context.GUIClickContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PaginatedGUI implements GUIBase {
    private final Inventory inventory;
    private final Map<Integer, Consumer<GUIClickContext>> handlers = new HashMap<>();

    private List<ItemStack> items;
    private int currentPage = 0;
    private int itemsPerPage;
    private ItemStack prevButton;
    private ItemStack nextButton;

    static {
        GUIListener.ensureRegistered();
    }

    public PaginatedGUI(String title, int rows) {
        if (rows < 2) rows = 2;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        this.itemsPerPage = (rows - 1) * 9;
    }

    public void setContent(List<ItemStack> items) {
        this.items = items;
        openPage(0);
    }

    public void setPrevButton(ItemStack prev) {
        this.prevButton = prev;
    }

    public void setNextButton(ItemStack next) {
        this.nextButton = next;
    }

    public void openPage(int page) {
        if (items == null || items.isEmpty()) return;

        currentPage = page;
        int lastRowStart = inventory.getSize() - 9;

        for (int i = 0; i < lastRowStart; i++) {
            inventory.setItem(i, null);
        }

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());

        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, items.get(i));
        }

        if (page > 0 && prevButton != null) {
            inventory.setItem(lastRowStart + 3, prevButton);
        } else {
            inventory.setItem(lastRowStart + 3, null);
        }

        if (end < items.size() && nextButton != null) {
            inventory.setItem(lastRowStart + 5, nextButton);
        } else {
            inventory.setItem(lastRowStart + 5, null);
        }
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        int slot = event.getSlot();
        int lastRowStart = inventory.getSize() - 9;

        if (slot == lastRowStart + 3 && currentPage > 0) {
            openPage(currentPage - 1);
            event.setCancelled(true);
            return;
        }

        if (slot == lastRowStart + 5 && items != null && (currentPage + 1) * itemsPerPage < items.size()) {
            openPage(currentPage + 1);
            event.setCancelled(true);
            return;
        }

        Consumer<GUIClickContext> handler = handlers.get(slot);
        if (handler != null) {
            handler.accept(new GUIClickContext(event));
        }
    }

    public void setItemHandler(int slot, Consumer<GUIClickContext> handler) {
        if (handler != null) handlers.put(slot, handler);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasHandler(int slot) {
        return handlers.containsKey(slot);
    }
}

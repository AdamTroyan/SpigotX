package dev.adam.gui;

import dev.adam.gui.context.GUIClickContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
    private Consumer<GUIClickContext> mainItemAction;
    private ItemStack glassPane;

    public PaginatedGUI(String title, int rows) {
        if (rows < 2) rows = 2;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        this.itemsPerPage = (rows - 1) * 9;
        
        this.glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glassPane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glassPane.setItemMeta(meta);
        }
    }

    public void setContent(List<ItemStack> items) {
        this.items = items;
        openPage(0);
    }

    public void setPrevButton(ItemStack prev) {
        this.prevButton = prev;
        updateNavigationButtons();
    }

    public void setNextButton(ItemStack next) {
        this.nextButton = next;
        updateNavigationButtons();
    }

    public void setGlassPane(ItemStack glassPane) {
        this.glassPane = glassPane;
        updateNavigationButtons();
    }

    public void setMainItemAction(Consumer<GUIClickContext> action) {
        this.mainItemAction = action;
    }

    private void updateNavigationButtons() {
        if (items == null || items.isEmpty()) return;
        
        int lastRowStart = inventory.getSize() - 9;
        int maxPage = (items.size() - 1) / itemsPerPage;
        
        removeHandler(lastRowStart + 3);
        removeHandler(lastRowStart + 5);
        
        if (currentPage > 0 && prevButton != null) {
            inventory.setItem(lastRowStart + 3, prevButton);
            setItemHandler(lastRowStart + 3, ctx -> {
                if (currentPage > 0) {
                    openPage(currentPage - 1);
                    updateNavigationButtons();
                }
            });
        } else {
            inventory.setItem(lastRowStart + 3, glassPane);
            setItemHandler(lastRowStart + 3, ctx -> {});
        }
        
        if (currentPage < maxPage && nextButton != null) {
            inventory.setItem(lastRowStart + 5, nextButton);
            setItemHandler(lastRowStart + 5, ctx -> {
                if (items != null && (currentPage + 1) * itemsPerPage < items.size()) {
                    openPage(currentPage + 1);
                    updateNavigationButtons();
                }
            });
        } else {
            inventory.setItem(lastRowStart + 5, glassPane);
            setItemHandler(lastRowStart + 5, ctx -> {});
        }
    }

    public void openPage(int page) {
        if (items == null || items.isEmpty()) return;
        if (page < 0) page = 0;
        int maxPage = (items.size() - 1) / itemsPerPage;
        if (page > maxPage) page = maxPage;

        currentPage = page;
        int lastRowStart = inventory.getSize() - 9;

        for (int i = 0; i < lastRowStart; i++) {
            inventory.setItem(i, null);
            removeHandler(i);
        }

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());

        for (int i = start; i < end; i++) {
            int slot = i - start;
            inventory.setItem(slot, items.get(i));

            if (mainItemAction != null) {
                final int itemIndex = i;
                setItemHandler(slot, ctx -> {
                    Consumer<GUIClickContext> action = mainItemAction;
                    if (action != null) {
                        action.accept(ctx);
                    }
                });
            }
        }

        updateNavigationButtons();
    }

    public int getTotalPages() {
        if (items == null || items.isEmpty()) return 0;
        return (items.size() - 1) / itemsPerPage + 1;
    }

    public boolean hasNextPage() {
        return items != null && (currentPage + 1) * itemsPerPage < items.size();
    }

    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    public void fillRowIfEmpty(int row, ItemStack item, Consumer<GUIClickContext> onClick) {
        int start = (row - 1) * 9;
        int end = start + 9;
        for (int i = start; i < end && i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
                setItemHandler(i, onClick);
            }
        }
    }

    public void fillColumnIfEmpty(int col, ItemStack item, Consumer<GUIClickContext> onClick) {
        if (col < 0 || col >= 9) return;
        for (int i = col; i < inventory.getSize(); i += 9) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
                setItemHandler(i, onClick);
            }
        }
    }

    public void clearRow(int row) {
        int start = (row - 1) * 9;
        for (int i = start; i < start + 9 && i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
            removeHandler(i);
        }
    }

    public void clearColumn(int col) {
        if (col < 0 || col >= 9) return;
        for (int i = col; i < inventory.getSize(); i += 9) {
            inventory.setItem(i, null);
            removeHandler(i);
        }
    }

    public void setItemsBulk(int[] slots, ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);
                setItemHandler(slot, onClick);
            }
        }
    }

    public void fillBorderIfEmpty(ItemStack item, Consumer<GUIClickContext> onClick) {
        int size = inventory.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, item);
                    setItemHandler(i, onClick);
                }
            }
        }
    }

    public void replaceItem(Material from, ItemStack to, Consumer<GUIClickContext> onClick) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack current = inventory.getItem(i);
            if (current != null && current.getType() == from) {
                inventory.setItem(i, to);
                setItemHandler(i, onClick);
            }
        }
    }

    public int getFirstEmptySlotInRow(int row) {
        int start = (row - 1) * 9;
        for (int i = start; i < start + 9 && i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) return i;
        }
        return -1;
    }

    public void setItemIfEmpty(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        if (slot >= 0 && slot < inventory.getSize() && inventory.getItem(slot) == null) {
            inventory.setItem(slot, item);
            setItemHandler(slot, onClick);
        }
    }

    public void setRow(int row, ItemStack[] items, Consumer<GUIClickContext>[] handlersArr) {
        int start = (row - 1) * 9;
        for (int i = 0; i < 9 && i < items.length && (start + i) < inventory.getSize(); i++) {
            inventory.setItem(start + i, items[i]);
            setItemHandler(start + i, handlersArr != null && i < handlersArr.length ? handlersArr[i] : null);
        }
    }

    public void setBackground(ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
                setItemHandler(i, onClick);
            }
        }
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        
        event.setCancelled(true);
        
        if (rawSlot < 0 || rawSlot >= inventory.getSize()) return;
        
        Consumer<GUIClickContext> handler = handlers.get(rawSlot);
        if (handler != null) {
            try {
                handler.accept(new GUIClickContext(event));
            } catch (Exception e) {
                dev.adam.logging.Logger.error("Error handling GUI click at slot " + rawSlot + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void setItemHandler(int slot, Consumer<GUIClickContext> handler) {
        if (slot >= 0 && slot < inventory.getSize()) {
            if (handler != null) {
                handlers.put(slot, handler);
            } else {
                handlers.remove(slot);
            }
        }
    }

    public void removeHandler(int slot) {
        handlers.remove(slot);
    }

    public Consumer<GUIClickContext> getHandler(int slot) {
        return handlers.get(slot);
    }

    public void open(Player player) {
        if (player != null && player.isOnline()) {
            player.openInventory(inventory);
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
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
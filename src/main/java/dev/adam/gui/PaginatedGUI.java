package dev.adam.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PaginatedGUI extends GUI {
    private List<ItemStack> content;
    private int page = 0;
    private final int itemsPerPage;

    public PaginatedGUI(String title, int rows) {
        super(title, rows);
        this.itemsPerPage = (rows * 9) - 2;
    }

    public void setContent(List<ItemStack> content) {
        this.content = content;
        updatePage();
    }

    public void setPrevItem(ItemStack item) {
        setItem(0, item, ctx -> {
            if (page > 0) {
                page--;
                updatePage();
            }
        });
    }

    public void setNextItem(ItemStack item) {
        setItem(getInventory().getSize() - 1, item, ctx -> {
            if (content != null && (page + 1) * itemsPerPage < content.size()) {
                page++;
                updatePage();
            }
        });
    }

    private void updatePage() {
        Inventory inv = getInventory();
        for (int i = 1; i < inv.getSize() - 1; i++) {
            inv.setItem(i, null);
        }
        if (content == null) return;
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, content.size());
        for (int i = start, slot = 1; i < end; i++, slot++) {
            inv.setItem(slot, content.get(i));
        }
    }
}
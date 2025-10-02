package dev.adam.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PaginatedGUI {

    private final Plugin plugin;
    private final int rows;
    private final String title;
    private final int pageSize;
    private final List<ItemStack> content = new ArrayList<>();
    private final int prevSlot;
    private final int nextSlot;

    private ItemStack prevItem;
    private ItemStack nextItem;
    private Consumer<Integer> onPageChange;

    private final GUI gui;

    public PaginatedGUI(Plugin plugin, String title, int rows) {
        this.plugin = plugin;
        this.title = title;
        this.rows = rows;
        this.pageSize = rows * 9 - 9;
        this.prevSlot = rows * 9 - 9;
        this.nextSlot = rows * 9 - 1;
        this.gui = new GUI(plugin, title, rows);
    }

    public void setContent(List<ItemStack> items) {
        content.clear();
        if (items != null) content.addAll(items);
    }

    public void addItem(ItemStack item) { if (item != null) content.add(item); }

    public void setPrevItem(ItemStack item) { this.prevItem = item; }
    public void setNextItem(ItemStack item) { this.nextItem = item; }
    public void setOnPageChange(Consumer<Integer> onPageChange) { this.onPageChange = onPageChange; }

    public void openPage(Player player, int page) {
        if (player == null) return;

        int pages = Math.max(1, (int) Math.ceil(content.size() / (double) pageSize));
        int p = Math.max(0, Math.min(page, pages - 1));

        gui.clear();
        int start = p * pageSize;
        int end = Math.min(start + pageSize, content.size());

        int slot = 0;
        for (int i = start; i < end; i++) {
            final ItemStack it = content.get(i);
            final int idx = i;
            gui.setItem(slot++, it, ctx -> {});
        }

        if (prevItem != null && p > 0)
            gui.setItem(prevSlot, prevItem, ctx -> openPage(player, p - 1));
        if (nextItem != null && p < pages - 1)
            gui.setItem(nextSlot, nextItem, ctx -> openPage(player, p + 1));

        if (onPageChange != null) onPageChange.accept(p);

        gui.open(player);
    }

    public void openFirst(Player player) { openPage(player, 0); }
}

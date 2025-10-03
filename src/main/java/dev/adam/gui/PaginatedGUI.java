package dev.adam.gui;

import org.bukkit.inventory.ItemStack;
import dev.adam.gui.context.GUIClickContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PaginatedGUI extends GUI {
    private List<ItemStack> content = new ArrayList<>();
    private final List<Consumer<GUIClickContext>> itemHandlers = new ArrayList<>();
    private int page = 0;
    private int itemsPerPage;
    private int prevSlot;
    private int nextSlot;
    private int bottomRowStart;
    private int bottomRowEnd;

    private Consumer<GUIClickContext> prevHandler = ctx -> {};
    private Consumer<GUIClickContext> nextHandler = ctx -> {};

    public PaginatedGUI(String title, int rows) {
        super(title, rows);
        int size = rows * 9;
        bottomRowStart = size - 9;
        bottomRowEnd = size - 1;
        prevSlot = bottomRowStart;
        nextSlot = bottomRowEnd;
        itemsPerPage = size - 9; 
    }

    public void setContent(List<ItemStack> items) {
        this.content = items != null ? items : new ArrayList<>();
        this.itemHandlers.clear();

        for (int i = 0; i < content.size(); i++) {
            this.itemHandlers.add(null);
        }

        updatePage();
    }

    public void setItemHandler(int index, Consumer<GUIClickContext> handler) {
        if (index >= 0 && index < itemHandlers.size()) {
            itemHandlers.set(index, handler);
        }
    }

    public void setPrevItem(ItemStack item) {
        super.setItem(prevSlot, item, ctx -> {
            ctx.getEvent().setCancelled(true);

            if (page > 0) {
                page--;
                updatePage();
            }

            prevHandler.accept(ctx);
        });
    }

    public void setNextItem(ItemStack item) {
        super.setItem(nextSlot, item, ctx -> {
            ctx.getEvent().setCancelled(true);

            if ((page + 1) * itemsPerPage < content.size()) {
                page++;
                updatePage();
            }

            nextHandler.accept(ctx);
        });
    }

    public void setPrevHandler(Consumer<GUIClickContext> handler) {
        this.prevHandler = handler;
    }

    public void setNextHandler(Consumer<GUIClickContext> handler) {
        this.nextHandler = handler;
    }

    private void updatePage() {
        for (int i = 0; i < getInventory().getSize(); i++) {
            getInventory().setItem(i, null);
        }

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, content.size());
        int slot = 0;

        for (int i = start; i < end && slot < itemsPerPage; i++, slot++) {
            getInventory().setItem(slot, content.get(i));
        }
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == prevSlot || slot == nextSlot) {
            super.handleClick(event);

            return;
        }

        if (slot >= 0 && slot < itemsPerPage) {
            int itemIndex = (page * itemsPerPage) + slot;

            if (itemIndex < content.size()) {
                event.setCancelled(true);
                Consumer<GUIClickContext> handler = itemHandlers.get(itemIndex);
                
                if (handler != null) {
                    handler.accept(new GUIClickContext(event));
                }
            }
        }
    }
}
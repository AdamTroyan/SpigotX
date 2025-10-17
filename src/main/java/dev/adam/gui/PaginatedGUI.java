package dev.adam.gui;

import dev.adam.gui.context.GUIClickContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private boolean isInitialized = false;

    public interface ItemFilter {
        boolean test(ItemStack item, int index);
    }

    private final List<ItemFilter> filters = new ArrayList<>();

    private String searchQuery = "";

    public enum SortType {
        ALPHABETICAL, AMOUNT, MATERIAL, CUSTOM
    }

    public interface ItemComparator {
        int compare(ItemStack a, ItemStack b);
    }

    private SortType sortType = SortType.ALPHABETICAL;
    private boolean sortAscending = true;
    private ItemComparator customComparator;

    public enum LayoutPattern {
        GRID, LIST, CENTERED, BORDER, SPIRAL, CHECKERBOARD
    }

    private LayoutPattern layoutPattern = LayoutPattern.GRID;

    public interface GUIAnimation {
        void tick(PaginatedGUI gui, long tickCount);

        boolean isFinished();
    }

    private final List<GUIAnimation> animations = new ArrayList<>();
    private long animationTick = 0;

    public interface GUIEventListener {
        void onPageChange(int oldPage, int newPage);

        void onItemClick(ItemStack item, int slot, ClickType clickType);

        void onGUIOpen(Player player);

        void onGUIClose(Player player);

        void onContentChange(List<ItemStack> oldItems, List<ItemStack> newItems);
    }

    private final List<GUIEventListener> eventListeners = new ArrayList<>();

    private final Map<String, List<ItemStack>> contentCache = new HashMap<>();
    private boolean cacheEnabled = true;

    public static class GUITheme {
        public ItemStack glassPane;
        public ItemStack prevButton;
        public ItemStack nextButton;
        public ItemStack searchButton;
        public ItemStack sortButton;
        public ItemStack filterButton;
        public String titleFormat = "%s - Page %d/%d";
        public ChatColor primaryColor = ChatColor.GRAY;
        public ChatColor secondaryColor = ChatColor.DARK_GRAY;
    }

    private GUITheme theme;

    public static class GUITemplate {
        private final Map<Integer, ItemStack> fixedItems = new HashMap<>();
        private final Map<Integer, Consumer<GUIClickContext>> fixedHandlers = new HashMap<>();

        public void setFixedItem(int slot, ItemStack item, Consumer<GUIClickContext> handler) {
            fixedItems.put(slot, item);
            fixedHandlers.put(slot, handler);
        }
    }

    private GUITemplate template;

    public static class GUIStatistics {
        private int totalClicks = 0;
        private int pageChanges = 0;
        private long totalTimeOpen = 0;
        private long openTime = 0;
        private final Map<Material, Integer> itemClickCounts = new HashMap<>();

        public void recordClick() {
            totalClicks++;
        }

        public void recordPageChange() {
            pageChanges++;
        }

        public void recordOpen() {
            openTime = System.currentTimeMillis();
        }

        public void recordClose() {
            if (openTime > 0) {
                totalTimeOpen += System.currentTimeMillis() - openTime;
            }
        }

        public int getTotalClicks() {
            return totalClicks;
        }

        public int getPageChanges() {
            return pageChanges;
        }

        public long getTotalTimeOpen() {
            return totalTimeOpen;
        }
    }

    private final GUIStatistics statistics = new GUIStatistics();

    public PaginatedGUI(String title, int rows) {
        if (rows < 2) rows = 2;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        this.itemsPerPage = (rows - 1) * 9;

        this.theme = createDefaultTheme();
        this.glassPane = theme.glassPane;

        initializeBottomRow();
    }

    private void initializeBottomRow() {
        int lastRowStart = inventory.getSize() - 9;
        for (int i = lastRowStart; i < inventory.getSize(); i++) {
            inventory.setItem(i, glassPane);
            setItemHandler(i, ctx -> {
            });
        }
    }

    public void setContent(List<ItemStack> items) {
        List<ItemStack> oldItems = this.items;
        this.items = items;
        this.currentPage = 0;

        fireContentChangeEvent(oldItems, items);

        refreshContent();
        this.isInitialized = true;
    }

    public void setPrevButton(ItemStack prev) {
        this.prevButton = prev;
        if (isInitialized) {
            updateNavigationButtons();
        }
    }

    public void setNextButton(ItemStack next) {
        this.nextButton = next;
        if (isInitialized) {
            updateNavigationButtons();
        }
    }

    public void setGlassPane(ItemStack glassPane) {
        if (glassPane != null) {
            this.glassPane = glassPane;
            if (isInitialized) {
                updateNavigationButtons();
            }
        }
    }

    public void setMainItemAction(Consumer<GUIClickContext> action) {
        this.mainItemAction = action;

        if (isInitialized && items != null && !items.isEmpty()) {
            refreshContentHandlers();
        }
    }

    private void refreshContent() {
        List<ItemStack> processedItems = getProcessedItems();

        openPageWithItems(currentPage, processedItems);
        updateNavigationButtons();
    }

    private void refreshContentHandlers() {
        if (items == null || items.isEmpty()) return;

        int lastRowStart = inventory.getSize() - 9;
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());

        for (int i = start; i < end; i++) {
            int slot = i - start;
            if (slot < lastRowStart && inventory.getItem(slot) != null) {
                final int itemIndex = i;
                setItemHandler(slot, ctx -> {
                    if (mainItemAction != null) {
                        mainItemAction.accept(ctx);
                    }
                });
            }
        }
    }

    private void updateNavigationButtons() {
        int lastRowStart = inventory.getSize() - 9;
        int maxPage = (items == null || items.isEmpty()) ? 0 : (items.size() - 1) / itemsPerPage;

        removeHandler(lastRowStart + 3);
        removeHandler(lastRowStart + 5);

        if (currentPage > 0 && prevButton != null) {
            inventory.setItem(lastRowStart + 3, prevButton);
            setItemHandler(lastRowStart + 3, ctx -> {
                if (currentPage > 0) {
                    openPage(currentPage - 1);
                }
            });
        } else {
            inventory.setItem(lastRowStart + 3, glassPane);
            setItemHandler(lastRowStart + 3, ctx -> {
            });
        }

        if (currentPage < maxPage && nextButton != null) {
            inventory.setItem(lastRowStart + 5, nextButton);
            setItemHandler(lastRowStart + 5, ctx -> {
                if (items != null && (currentPage + 1) * itemsPerPage < items.size()) {
                    openPage(currentPage + 1);
                }
            });
        } else {
            inventory.setItem(lastRowStart + 5, glassPane);
            setItemHandler(lastRowStart + 5, ctx -> {
            });
        }
    }

    public void openPage(int page) {
        List<ItemStack> processedItems = getProcessedItems();
        openPageWithItems(page, processedItems);
    }

    private void clearContentArea() {
        int lastRowStart = inventory.getSize() - 9;
        for (int i = 0; i < lastRowStart; i++) {
            inventory.setItem(i, null);
            removeHandler(i);
        }
    }

    public int getTotalPages() {
        if (items == null || items.isEmpty()) return 1;
        return (items.size() - 1) / itemsPerPage + 1;
    }

    public boolean hasNextPage() {
        return items != null && !items.isEmpty() && (currentPage + 1) * itemsPerPage < items.size();
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

    private GUITheme createDefaultTheme() {
        GUITheme theme = new GUITheme();
        theme.glassPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = theme.glassPane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            theme.glassPane.setItemMeta(meta);
        }
        return theme;
    }

    public void addFilter(ItemFilter filter) {
        if (filter != null) {
            filters.add(filter);
            refreshContent();
        }
    }

    public void removeFilter(ItemFilter filter) {
        filters.remove(filter);
        refreshContent();
    }

    public void clearFilters() {
        filters.clear();
        refreshContent();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase() : "";
        refreshContent();
    }

    public void clearSearch() {
        this.searchQuery = "";
        refreshContent();
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSorting(SortType type, boolean ascending) {
        this.sortType = type != null ? type : SortType.ALPHABETICAL;
        this.sortAscending = ascending;
        refreshContent();
    }

    public void setCustomComparator(ItemComparator comparator) {
        this.customComparator = comparator;
        this.sortType = SortType.CUSTOM;
        refreshContent();
    }

    public SortType getSortType() {
        return sortType;
    }

    public boolean isSortAscending() {
        return sortAscending;
    }

    public void setLayout(LayoutPattern pattern) {
        this.layoutPattern = pattern != null ? pattern : LayoutPattern.GRID;
        refreshContent();
    }

    public LayoutPattern getLayoutPattern() {
        return layoutPattern;
    }

    public void addAnimation(GUIAnimation animation) {
        if (animation != null) {
            animations.add(animation);
        }
    }

    public void tickAnimations() {
        animationTick++;
        animations.removeIf(animation -> {
            try {
                animation.tick(this, animationTick);
                return animation.isFinished();
            } catch (Exception e) {
                e.printStackTrace();
                return true;
            }
        });
    }

    public void addEventListener(GUIEventListener listener) {
        if (listener != null) {
            eventListeners.add(listener);
        }
    }

    public void removeEventListener(GUIEventListener listener) {
        eventListeners.remove(listener);
    }

    public void setTheme(GUITheme theme) {
        this.theme = theme != null ? theme : createDefaultTheme();
        this.glassPane = this.theme.glassPane;
        applyTheme();
    }

    public GUITheme getTheme() {
        return theme;
    }

    public static GUITheme createDarkTheme() {
        GUITheme theme = new GUITheme();
        theme.glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = theme.glassPane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            theme.glassPane.setItemMeta(meta);
        }
        theme.primaryColor = ChatColor.DARK_GRAY;
        theme.secondaryColor = ChatColor.BLACK;
        return theme;
    }

    public void setTemplate(GUITemplate template) {
        this.template = template;
        if (template != null && isInitialized) {
            applyTemplate();
        }
    }

    public GUITemplate getTemplate() {
        return template;
    }

    public void enableCache(boolean enable) {
        this.cacheEnabled = enable;
        if (!enable) {
            contentCache.clear();
        }
    }

    public void clearCache() {
        contentCache.clear();
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public GUIStatistics getStatistics() {
        return statistics;
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();

        event.setCancelled(true);

        if (rawSlot < 0 || rawSlot >= inventory.getSize()) {
            return;
        }

        Consumer<GUIClickContext> handler = handlers.get(rawSlot);
        if (handler != null) {
            try {
                handler.accept(new GUIClickContext(event));
            } catch (Exception e) {

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
            statistics.recordOpen();
            fireGUIOpenEvent(player);
            player.openInventory(inventory);
        }
    }

    public void close(Player player) {
        if (player != null && player.isOnline()) {
            statistics.recordClose();
            fireGUICloseEvent(player);
            player.closeInventory();
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

    public boolean isInitialized() {
        return isInitialized;
    }

    public void reset() {
        handlers.clear();
        items = null;
        currentPage = 0;
        isInitialized = false;

        filters.clear();
        searchQuery = "";
        contentCache.clear();
        animations.clear();
        animationTick = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        initializeBottomRow();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasHandler(int slot) {
        return handlers.containsKey(slot);
    }

    private List<ItemStack> getProcessedItems() {
        if (items == null) return new ArrayList<>();

        List<ItemStack> cached = getCachedContent();
        if (cached != null) {
            return cached;
        }

        List<ItemStack> processed = new ArrayList<>(items);

        processed = getFilteredItems(processed);

        processed = getSearchedItems(processed);

        processed = getSortedItems(processed);

        setCachedContent(processed);

        return processed;
    }

    private List<ItemStack> getFilteredItems(List<ItemStack> items) {
        if (filters.isEmpty()) return items;

        List<ItemStack> filtered = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            boolean passesAllFilters = true;

            for (ItemFilter filter : filters) {
                if (!filter.test(item, i)) {
                    passesAllFilters = false;
                    break;
                }
            }

            if (passesAllFilters) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private List<ItemStack> getSearchedItems(List<ItemStack> items) {
        if (searchQuery.isEmpty()) return items;

        return items.stream()
                .filter(this::matchesSearch)
                .collect(Collectors.toList());
    }

    private boolean matchesSearch(ItemStack item) {
        if (searchQuery.isEmpty()) return true;

        String itemName = item.getType().name().toLowerCase();
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            itemName = item.getItemMeta().getDisplayName().toLowerCase();
        }

        return itemName.contains(searchQuery);
    }

    private List<ItemStack> getSortedItems(List<ItemStack> items) {
        List<ItemStack> sorted = new ArrayList<>(items);

        switch (sortType) {
            case ALPHABETICAL:
                sorted.sort((a, b) -> a.getType().name().compareTo(b.getType().name()));
                break;
            case AMOUNT:
                sorted.sort((a, b) -> Integer.compare(a.getAmount(), b.getAmount()));
                break;
            case MATERIAL:
                sorted.sort((a, b) -> a.getType().compareTo(b.getType()));
                break;
            case CUSTOM:
                if (customComparator != null) {
                    sorted.sort(customComparator::compare);
                }
                break;
        }

        if (!sortAscending) {
            Collections.reverse(sorted);
        }

        return sorted;
    }

    private String getCacheKey() {
        return searchQuery + "|" + sortType + "|" + sortAscending + "|" + filters.size();
    }

    private List<ItemStack> getCachedContent() {
        if (!cacheEnabled) return null;
        return contentCache.get(getCacheKey());
    }

    private void setCachedContent(List<ItemStack> content) {
        if (cacheEnabled && content != null) {
            contentCache.put(getCacheKey(), new ArrayList<>(content));
        }
    }

    private void applyTheme() {
        if (theme == null || !isInitialized) return;

        this.glassPane = theme.glassPane;

        if (prevButton != null && theme.prevButton != null) {
            this.prevButton = theme.prevButton;
        }
        if (nextButton != null && theme.nextButton != null) {
            this.nextButton = theme.nextButton;
        }

        refreshContent();
    }

    private void applyTemplate() {
        if (template == null) return;

        for (Map.Entry<Integer, ItemStack> entry : template.fixedItems.entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, entry.getValue());
                setItemHandler(slot, template.fixedHandlers.get(slot));
            }
        }
    }

    private void openPageWithItems(int page, List<ItemStack> itemsList) {
        if (itemsList == null || itemsList.isEmpty()) {
            clearContentArea();
            currentPage = 0;
            updateNavigationButtons();
            return;
        }

        if (page < 0) page = 0;
        int maxPage = (itemsList.size() - 1) / itemsPerPage;
        if (page > maxPage) page = maxPage;

        int oldPage = currentPage;
        currentPage = page;

        clearContentArea();

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, itemsList.size());

        int[] slots = getSlotsByPattern(layoutPattern, end - start);

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex < slots.length) {
                int slot = slots[slotIndex];
                ItemStack item = itemsList.get(i);
                inventory.setItem(slot, item);

                final int itemIndex = i;
                setItemHandler(slot, ctx -> {
                    if (mainItemAction != null) {
                        statistics.recordClick();
                        fireItemClickEvent(item, slot, ctx.getEvent().getClick());

                        mainItemAction.accept(ctx);
                    }
                });
            }
        }

        if (template != null) {
            applyTemplate();
        }

        if (oldPage != currentPage) {
            statistics.recordPageChange();
            firePageChangeEvent(oldPage, currentPage);
        }

        updateNavigationButtons();
    }

    private int[] getSlotsByPattern(LayoutPattern pattern, int itemCount) {
        int lastRowStart = inventory.getSize() - 9;

        switch (pattern) {
            case GRID:
                return getGridSlots(itemCount, lastRowStart);
            case LIST:
                return getListSlots(itemCount, lastRowStart);
            case CENTERED:
                return getCenteredSlots(itemCount, lastRowStart);
            case BORDER:
                return getBorderSlots(itemCount, lastRowStart);
            case SPIRAL:
                return getSpiralSlots(itemCount, lastRowStart);
            case CHECKERBOARD:
                return getCheckerboardSlots(itemCount, lastRowStart);
            default:
                return getGridSlots(itemCount, lastRowStart);
        }
    }

    private int[] getGridSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < Math.min(itemCount, maxSlot); i++) {
            slots.add(i);
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    private int[] getListSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();
        int col = 4;
        for (int row = 0; row < 6 && slots.size() < itemCount; row++) {
            int slot = row * 9 + col;
            if (slot < maxSlot) {
                slots.add(slot);
            }
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    private int[] getCenteredSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();
        int[] centers = {4, 3, 5, 2, 6, 1, 7, 0, 8};

        for (int row = 0; row < 6 && slots.size() < itemCount; row++) {
            for (int colIndex = 0; colIndex < centers.length && slots.size() < itemCount; colIndex++) {
                int slot = row * 9 + centers[colIndex];
                if (slot < maxSlot) {
                    slots.add(slot);
                }
            }
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    private int[] getBorderSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < maxSlot && slots.size() < itemCount; i++) {
            int row = i / 9;
            int col = i % 9;

            if (row == 0 || row == 5 || col == 0 || col == 8) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    private int[] getSpiralSlots(int itemCount, int maxSlot) {
        return getGridSlots(itemCount, maxSlot);
    }

    private int[] getCheckerboardSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < maxSlot && slots.size() < itemCount; i++) {
            int row = i / 9;
            int col = i % 9;

            if ((row + col) % 2 == 0) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    private void firePageChangeEvent(int oldPage, int newPage) {
        for (GUIEventListener listener : eventListeners) {
            try {
                listener.onPageChange(oldPage, newPage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fireItemClickEvent(ItemStack item, int slot, ClickType clickType) {
        for (GUIEventListener listener : eventListeners) {
            try {
                listener.onItemClick(item, slot, clickType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fireContentChangeEvent(List<ItemStack> oldItems, List<ItemStack> newItems) {
        for (GUIEventListener listener : eventListeners) {
            try {
                listener.onContentChange(oldItems, newItems);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fireGUIOpenEvent(Player player) {
        for (GUIEventListener listener : eventListeners) {
            try {
                listener.onGUIOpen(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fireGUICloseEvent(Player player) {
        for (GUIEventListener listener : eventListeners) {
            try {
                listener.onGUIClose(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
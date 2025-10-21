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

/**
 * Advanced paginated GUI system for Bukkit/Spigot plugins.
 * 
 * This class provides a comprehensive solution for creating paginated inventories with
 * advanced features like filtering, searching, sorting, and custom layouts. It handles
 * navigation buttons, content management, and provides extensive customization options
 * for creating professional-looking inventory interfaces.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic pagination with configurable items per page</li>
 *   <li>Built-in navigation buttons (previous/next)</li>
 *   <li>Content filtering with custom filter functions</li>
 *   <li>Search functionality for finding specific items</li>
 *   <li>Multiple sorting options (alphabetical, amount, material, custom)</li>
 *   <li>Various layout patterns (grid, list, centered, border, checkerboard)</li>
 *   <li>Comprehensive event system for GUI interactions</li>
 *   <li>Statistics tracking for monitoring usage</li>
 *   <li>Customizable themes and templates</li>
 *   <li>Performance optimizations with caching</li>
 * </ul>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class PaginatedGUI implements GUIBase {
    /** The main inventory that holds all GUI items */
    private final Inventory inventory;
    /** Map of slot handlers for click events */
    private final Map<Integer, Consumer<GUIClickContext>> handlers = new HashMap<>();

    /** The main content items to paginate */
    private List<ItemStack> items;
    /** Current page number (0-based) */
    private int currentPage = 0;
    /** Number of content items per page */
    private int itemsPerPage;
    /** Navigation button for previous page */
    private ItemStack prevButton;
    /** Navigation button for next page */
    private ItemStack nextButton;
    /** Action to execute when content items are clicked */
    private Consumer<GUIClickContext> mainItemAction;
    /** Background item for empty slots */
    private ItemStack glassPane;
    /** Whether the GUI has been properly initialized */
    private boolean isInitialized = false;

    // === FILTERING SYSTEM ===

    /**
     * Interface for filtering items in the GUI.
     * Filters determine which items should be displayed based on custom criteria.
     */
    public interface ItemFilter {
        /**
         * Tests whether an item should be included in the display.
         * 
         * @param item the ItemStack to test
         * @param index the original index of the item in the list
         * @return true if the item should be shown, false otherwise
         */
        boolean test(ItemStack item, int index);
    }

    /** List of active filters applied to the content */
    private final List<ItemFilter> filters = new ArrayList<>();

    /** Current search query for filtering by name */
    private String searchQuery = "";

    // === SORTING SYSTEM ===

    /**
     * Enumeration of available sorting types for organizing displayed items.
     */
    public enum SortType {
        /** Sort items alphabetically by name */
        ALPHABETICAL,
        /** Sort items by stack amount */
        AMOUNT,
        /** Sort items by material type */
        MATERIAL,
        /** Use custom comparator for sorting */
        CUSTOM
    }

    /**
     * Interface for custom item comparison logic.
     */
    public interface ItemComparator {
        /**
         * Compares two ItemStacks for sorting purposes.
         * 
         * @param a first ItemStack to compare
         * @param b second ItemStack to compare
         * @return negative if a < b, zero if a == b, positive if a > b
         */
        int compare(ItemStack a, ItemStack b);
    }

    /** Current sort type being used */
    private SortType sortType = SortType.ALPHABETICAL;
    /** Whether sorting is in ascending order */
    private boolean sortAscending = true;
    /** Custom comparator for CUSTOM sort type */
    private ItemComparator customComparator;

    // === LAYOUT SYSTEM ===

    /**
     * Enumeration of available layout patterns for item placement.
     */
    public enum LayoutPattern {
        /** Standard grid layout filling rows left to right */
        GRID,
        /** Single column centered layout */
        LIST,
        /** Items placed from center outward */
        CENTERED,
        /** Items placed around the border first */
        BORDER,
        /** Checkerboard pattern placement */
        CHECKERBOARD
    }

    /** Current layout pattern being used */
    private LayoutPattern layoutPattern = LayoutPattern.GRID;

    // === EVENT SYSTEM ===

    /**
     * Interface for listening to GUI events and state changes.
     */
    public interface GUIEventListener {
        /**
         * Called when the page changes.
         * 
         * @param oldPage the previous page number
         * @param newPage the new current page number
         */
        void onPageChange(int oldPage, int newPage);

        /**
         * Called when an item is clicked.
         * 
         * @param item the ItemStack that was clicked
         * @param slot the slot number where the click occurred
         * @param clickType the type of click performed
         */
        void onItemClick(ItemStack item, int slot, ClickType clickType);

        /**
         * Called when the GUI is opened by a player.
         * 
         * @param player the player who opened the GUI
         */
        void onGUIOpen(Player player);

        /**
         * Called when the GUI is closed by a player.
         * 
         * @param player the player who closed the GUI
         */
        void onGUIClose(Player player);

        /**
         * Called when the content of the GUI changes.
         * 
         * @param oldItems the previous item list
         * @param newItems the new item list
         */
        void onContentChange(List<ItemStack> oldItems, List<ItemStack> newItems);
    }

    /** List of registered event listeners */
    private final List<GUIEventListener> eventListeners = new ArrayList<>();

    // === CACHING SYSTEM ===

    /** Cache for processed content to improve performance */
    private final Map<String, List<ItemStack>> contentCache = new HashMap<>();
    /** Whether content caching is enabled */
    private boolean cacheEnabled = true;

    // === THEME SYSTEM ===

    /**
     * Theme configuration for customizing GUI appearance.
     */
    public static class GUITheme {
        /** Background glass pane item */
        public ItemStack glassPane;
        /** Previous page navigation button */
        public ItemStack prevButton;
        /** Next page navigation button */
        public ItemStack nextButton;
        /** Format string for GUI titles */
        public String titleFormat = "%s - Page %d/%d";
        /** Primary color for text elements */
        public ChatColor primaryColor = ChatColor.GRAY;
        /** Secondary color for text elements */
        public ChatColor secondaryColor = ChatColor.DARK_GRAY;
    }

    /** Current theme configuration */
    private GUITheme theme;

    // === STATISTICS SYSTEM ===

    /**
     * Statistics tracking for GUI usage and performance monitoring.
     */
    public static class GUIStatistics {
        private int totalClicks = 0;
        private int pageChanges = 0;
        private long totalTimeOpen = 0;
        private long openTime = 0;
        private final Map<Material, Integer> itemClickCounts = new HashMap<>();

        /**
         * Records a click event.
         */
        public void recordClick() {
            totalClicks++;
        }

        /**
         * Records a page change event.
         */
        public void recordPageChange() {
            pageChanges++;
        }

        /**
         * Records when the GUI is opened.
         */
        public void recordOpen() {
            openTime = System.currentTimeMillis();
        }

        /**
         * Records when the GUI is closed.
         */
        public void recordClose() {
            if (openTime > 0) {
                totalTimeOpen += System.currentTimeMillis() - openTime;
            }
        }

        /**
         * Gets the total number of clicks recorded.
         * 
         * @return total click count
         */
        public int getTotalClicks() {
            return totalClicks;
        }

        /**
         * Gets the total number of page changes recorded.
         * 
         * @return total page change count
         */
        public int getPageChanges() {
            return pageChanges;
        }

        /**
         * Gets the total time the GUI has been open in milliseconds.
         * 
         * @return total open time in milliseconds
         */
        public long getTotalTimeOpen() {
            return totalTimeOpen;
        }
    }

    /** Statistics instance for this GUI */
    private final GUIStatistics statistics = new GUIStatistics();

    // === CONSTRUCTOR ===

    /**
     * Creates a new paginated GUI with the specified title and number of rows.
     * 
     * @param title the title displayed at the top of the inventory
     * @param rows the number of rows in the inventory (minimum 2, maximum 6)
     */
    public PaginatedGUI(String title, int rows) {
        if (rows < 2) rows = 2;
        if (rows > 6) rows = 6;
        
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        this.itemsPerPage = (rows - 1) * 9; // Reserve last row for navigation

        this.theme = createDefaultTheme();
        this.glassPane = theme.glassPane;

        initializeBottomRow();
    }

    /**
     * Initializes the bottom row with glass pane background.
     */
    private void initializeBottomRow() {
        int lastRowStart = inventory.getSize() - 9;
        for (int i = lastRowStart; i < inventory.getSize(); i++) {
            inventory.setItem(i, glassPane);
            setItemHandler(i, ctx -> {}); // Empty handler to prevent null clicks
        }
    }

    // === CONTENT MANAGEMENT ===

    /**
     * Sets the main content items to be paginated.
     * This will reset the current page to 0 and refresh the display.
     * 
     * @param items the list of ItemStacks to paginate
     */
    public void setContent(List<ItemStack> items) {
        List<ItemStack> oldItems = this.items;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.currentPage = 0;

        fireContentChangeEvent(oldItems, this.items);
        refreshContent();
        this.isInitialized = true;
    }

    /**
     * Sets the previous page navigation button.
     * 
     * @param prev the ItemStack to use as the previous button
     */
    public void setPrevButton(ItemStack prev) {
        this.prevButton = prev;
        if (isInitialized) {
            updateNavigationButtons();
        }
    }

    /**
     * Sets the next page navigation button.
     * 
     * @param next the ItemStack to use as the next button
     */
    public void setNextButton(ItemStack next) {
        this.nextButton = next;
        if (isInitialized) {
            updateNavigationButtons();
        }
    }

    /**
     * Sets the background glass pane item for empty slots.
     * 
     * @param glassPane the ItemStack to use as background (null to use theme default)
     */
    public void setGlassPane(ItemStack glassPane) {
        if (glassPane != null) {
            this.glassPane = glassPane;
            if (isInitialized) {
                updateNavigationButtons();
            }
        }
    }

    /**
     * Sets the action to execute when content items are clicked.
     * 
     * @param action the Consumer to handle content item clicks
     */
    public void setMainItemAction(Consumer<GUIClickContext> action) {
        this.mainItemAction = action;
        if (isInitialized && items != null && !items.isEmpty()) {
            refreshContentHandlers();
        }
    }

    // === NAVIGATION METHODS ===

    /**
     * Opens a specific page of the pagination.
     * 
     * @param page the page number to open (0-based)
     */
    public void openPage(int page) {
        List<ItemStack> processedItems = getProcessedItems();
        openPageWithItems(page, processedItems);
    }

    /**
     * Gets the total number of pages available.
     * 
     * @return the total page count (minimum 1)
     */
    public int getTotalPages() {
        if (items == null || items.isEmpty()) return 1;
        return (items.size() - 1) / itemsPerPage + 1;
    }

    /**
     * Checks if there is a next page available.
     * 
     * @return true if there is a next page, false otherwise
     */
    public boolean hasNextPage() {
        return items != null && !items.isEmpty() && (currentPage + 1) * itemsPerPage < items.size();
    }

    /**
     * Checks if there is a previous page available.
     * 
     * @return true if there is a previous page, false otherwise
     */
    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    /**
     * Gets the current page number.
     * 
     * @return the current page (0-based)
     */
    public int getCurrentPage() {
        return currentPage;
    }

    // === FILTERING METHODS ===

    /**
     * Adds a filter to the content display.
     * Filters are applied in the order they are added.
     * 
     * @param filter the ItemFilter to add
     */
    public void addFilter(ItemFilter filter) {
        if (filter != null) {
            filters.add(filter);
            refreshContent();
        }
    }

    /**
     * Removes a specific filter from the content display.
     * 
     * @param filter the ItemFilter to remove
     */
    public void removeFilter(ItemFilter filter) {
        if (filters.remove(filter)) {
            refreshContent();
        }
    }

    /**
     * Clears all filters from the content display.
     */
    public void clearFilters() {
        if (!filters.isEmpty()) {
            filters.clear();
            refreshContent();
        }
    }

    /**
     * Sets the search query for filtering items by name.
     * 
     * @param query the search string (case-insensitive)
     */
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase() : "";
        refreshContent();
    }

    /**
     * Clears the current search query.
     */
    public void clearSearch() {
        if (!searchQuery.isEmpty()) {
            this.searchQuery = "";
            refreshContent();
        }
    }

    /**
     * Gets the current search query.
     * 
     * @return the current search query
     */
    public String getSearchQuery() {
        return searchQuery;
    }

    // === SORTING METHODS ===

    /**
     * Sets the sorting type and order for displayed items.
     * 
     * @param type the SortType to use
     * @param ascending true for ascending order, false for descending
     */
    public void setSorting(SortType type, boolean ascending) {
        this.sortType = type != null ? type : SortType.ALPHABETICAL;
        this.sortAscending = ascending;
        refreshContent();
    }

    /**
     * Sets a custom comparator for sorting items.
     * This automatically sets the sort type to CUSTOM.
     * 
     * @param comparator the ItemComparator to use for sorting
     */
    public void setCustomComparator(ItemComparator comparator) {
        this.customComparator = comparator;
        this.sortType = SortType.CUSTOM;
        refreshContent();
    }

    /**
     * Gets the current sort type.
     * 
     * @return the current SortType
     */
    public SortType getSortType() {
        return sortType;
    }

    /**
     * Checks if sorting is in ascending order.
     * 
     * @return true if ascending, false if descending
     */
    public boolean isSortAscending() {
        return sortAscending;
    }

    // === LAYOUT METHODS ===

    /**
     * Sets the layout pattern for item placement.
     * 
     * @param pattern the LayoutPattern to use
     */
    public void setLayout(LayoutPattern pattern) {
        this.layoutPattern = pattern != null ? pattern : LayoutPattern.GRID;
        refreshContent();
    }

    /**
     * Gets the current layout pattern.
     * 
     * @return the current LayoutPattern
     */
    public LayoutPattern getLayoutPattern() {
        return layoutPattern;
    }

    // === EVENT SYSTEM ===

    /**
     * Adds an event listener for GUI events.
     * 
     * @param listener the GUIEventListener to add
     */
    public void addEventListener(GUIEventListener listener) {
        if (listener != null) {
            eventListeners.add(listener);
        }
    }

    /**
     * Removes an event listener from the GUI.
     * 
     * @param listener the GUIEventListener to remove
     */
    public void removeEventListener(GUIEventListener listener) {
        eventListeners.remove(listener);
    }

    // === THEME SYSTEM ===

    /**
     * Sets the theme for the GUI appearance.
     * 
     * @param theme the GUITheme to apply
     */
    public void setTheme(GUITheme theme) {
        this.theme = theme != null ? theme : createDefaultTheme();
        this.glassPane = this.theme.glassPane;
        applyTheme();
    }

    /**
     * Gets the current theme.
     * 
     * @return the current GUITheme
     */
    public GUITheme getTheme() {
        return theme;
    }

    /**
     * Creates a dark theme with black glass panes.
     * 
     * @return a new dark GUITheme
     */
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

    // === CACHE MANAGEMENT ===

    /**
     * Enables or disables content caching for performance optimization.
     * 
     * @param enable true to enable caching, false to disable
     */
    public void enableCache(boolean enable) {
        this.cacheEnabled = enable;
        if (!enable) {
            contentCache.clear();
        }
    }

    /**
     * Clears the content cache manually.
     */
    public void clearCache() {
        contentCache.clear();
    }

    /**
     * Checks if caching is currently enabled.
     * 
     * @return true if caching is enabled, false otherwise
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    // === STATISTICS ===

    /**
     * Gets the statistics object for this GUI.
     * 
     * @return the GUIStatistics instance
     */
    public GUIStatistics getStatistics() {
        return statistics;
    }

    // === UTILITY METHODS ===

    /**
     * Gets the main content items.
     * 
     * @return the list of content ItemStacks
     */
    public List<ItemStack> getItems() {
        return items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    /**
     * Gets the number of items displayed per page.
     * 
     * @return items per page count
     */
    public int getItemsPerPage() {
        return itemsPerPage;
    }

    /**
     * Checks if the GUI has been initialized with content.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Resets the GUI to its initial state.
     * Clears all content, filters, handlers, and statistics.
     */
    public void reset() {
        handlers.clear();
        items = null;
        currentPage = 0;
        isInitialized = false;

        filters.clear();
        searchQuery = "";
        contentCache.clear();

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, null);
        }

        initializeBottomRow();
    }

    // === GUIBASE IMPLEMENTATION ===

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

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasHandler(int slot) {
        return handlers.containsKey(slot);
    }

    /**
     * Opens the GUI for a player.
     * 
     * @param player the player to open the GUI for
     */
    public void open(Player player) {
        if (player != null && player.isOnline()) {
            statistics.recordOpen();
            fireGUIOpenEvent(player);
            player.openInventory(inventory);
        }
    }

    /**
     * Closes the GUI for a player.
     * 
     * @param player the player to close the GUI for
     */
    public void close(Player player) {
        if (player != null && player.isOnline()) {
            statistics.recordClose();
            fireGUICloseEvent(player);
            player.closeInventory();
        }
    }

    // === PRIVATE IMPLEMENTATION METHODS ===

    /**
     * Sets a click handler for a specific slot.
     */
    private void setItemHandler(int slot, Consumer<GUIClickContext> handler) {
        if (slot >= 0 && slot < inventory.getSize()) {
            if (handler != null) {
                handlers.put(slot, handler);
            } else {
                handlers.remove(slot);
            }
        }
    }

    /**
     * Removes a click handler for a specific slot.
     */
    private void removeHandler(int slot) {
        handlers.remove(slot);
    }

    /**
     * Refreshes the displayed content and navigation.
     */
    private void refreshContent() {
        List<ItemStack> processedItems = getProcessedItems();
        openPageWithItems(currentPage, processedItems);
        updateNavigationButtons();
    }

    /**
     * Refreshes only the click handlers for content items.
     */
    private void refreshContentHandlers() {
        if (items == null || items.isEmpty()) return;

        int lastRowStart = inventory.getSize() - 9;
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, items.size());

        for (int i = start; i < end; i++) {
            int slot = i - start;
            if (slot < lastRowStart && inventory.getItem(slot) != null) {
                setItemHandler(slot, ctx -> {
                    if (mainItemAction != null) {
                        mainItemAction.accept(ctx);
                    }
                });
            }
        }
    }

    /**
     * Updates the navigation buttons based on current page.
     */
    private void updateNavigationButtons() {
        int lastRowStart = inventory.getSize() - 9;
        int maxPage = (items == null || items.isEmpty()) ? 0 : (items.size() - 1) / itemsPerPage;

        // Clear existing navigation handlers
        removeHandler(lastRowStart + 3);
        removeHandler(lastRowStart + 5);

        // Previous button
        if (currentPage > 0 && prevButton != null) {
            inventory.setItem(lastRowStart + 3, prevButton);
            setItemHandler(lastRowStart + 3, ctx -> {
                if (currentPage > 0) {
                    openPage(currentPage - 1);
                }
            });
        } else {
            inventory.setItem(lastRowStart + 3, glassPane);
            setItemHandler(lastRowStart + 3, ctx -> {});
        }

        // Next button
        if (currentPage < maxPage && nextButton != null) {
            inventory.setItem(lastRowStart + 5, nextButton);
            setItemHandler(lastRowStart + 5, ctx -> {
                if (items != null && (currentPage + 1) * itemsPerPage < items.size()) {
                    openPage(currentPage + 1);
                }
            });
        } else {
            inventory.setItem(lastRowStart + 5, glassPane);
            setItemHandler(lastRowStart + 5, ctx -> {});
        }
    }

    /**
     * Clears all content items from the display area.
     */
    private void clearContentArea() {
        int lastRowStart = inventory.getSize() - 9;
        for (int i = 0; i < lastRowStart; i++) {
            inventory.setItem(i, null);
            removeHandler(i);
        }
    }

    /**
     * Creates the default theme with gray glass panes.
     */
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

    /**
     * Gets processed items after applying filters, search, and sorting.
     */
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

    /**
     * Applies all registered filters to the item list.
     */
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

    /**
     * Applies search query filtering to the item list.
     */
    private List<ItemStack> getSearchedItems(List<ItemStack> items) {
        if (searchQuery.isEmpty()) return items;

        return items.stream()
                .filter(this::matchesSearch)
                .collect(Collectors.toList());
    }

    /**
     * Checks if an item matches the current search query.
     */
    private boolean matchesSearch(ItemStack item) {
        if (searchQuery.isEmpty()) return true;

        String itemName = item.getType().name().toLowerCase();
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            itemName = item.getItemMeta().getDisplayName().toLowerCase();
        }

        return itemName.contains(searchQuery);
    }

    /**
     * Applies sorting to the item list based on current sort settings.
     */
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

    /**
     * Generates a cache key for the current filter/sort/search state.
     */
    private String getCacheKey() {
        return searchQuery + "|" + sortType + "|" + sortAscending + "|" + filters.size();
    }

    /**
     * Gets cached content if available and caching is enabled.
     */
    private List<ItemStack> getCachedContent() {
        if (!cacheEnabled) return null;
        return contentCache.get(getCacheKey());
    }

    /**
     * Stores processed content in the cache.
     */
    private void setCachedContent(List<ItemStack> content) {
        if (cacheEnabled && content != null) {
            contentCache.put(getCacheKey(), new ArrayList<>(content));
        }
    }

    /**
     * Applies the current theme to the GUI.
     */
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

    /**
     * Opens a specific page with the given processed items.
     */
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

                setItemHandler(slot, ctx -> {
                    if (mainItemAction != null) {
                        statistics.recordClick();
                        fireItemClickEvent(item, slot, ctx.getEvent().getClick());
                        mainItemAction.accept(ctx);
                    }
                });
            }
        }

        if (oldPage != currentPage) {
            statistics.recordPageChange();
            firePageChangeEvent(oldPage, currentPage);
        }

        updateNavigationButtons();
    }

    /**
     * Gets slot positions based on the specified layout pattern.
     */
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
            case CHECKERBOARD:
                return getCheckerboardSlots(itemCount, lastRowStart);
            default:
                return getGridSlots(itemCount, lastRowStart);
        }
    }

    /**
     * Gets slots for grid layout (standard left-to-right, top-to-bottom).
     */
    private int[] getGridSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < Math.min(itemCount, maxSlot); i++) {
            slots.add(i);
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Gets slots for list layout (single column, centered).
     */
    private int[] getListSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();
        int col = 4; // Center column
        for (int row = 0; row < 6 && slots.size() < itemCount; row++) {
            int slot = row * 9 + col;
            if (slot < maxSlot) {
                slots.add(slot);
            }
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Gets slots for centered layout (from center outward).
     */
    private int[] getCenteredSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();
        int[] centers = {4, 3, 5, 2, 6, 1, 7, 0, 8}; // Center to edges

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

    /**
     * Gets slots for border layout (edges first).
     */
    private int[] getBorderSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < maxSlot && slots.size() < itemCount; i++) {
            int row = i / 9;
            int col = i % 9;

            // Check if on border (first/last row or first/last column)
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Gets slots for checkerboard layout (alternating pattern).
     */
    private int[] getCheckerboardSlots(int itemCount, int maxSlot) {
        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < maxSlot && slots.size() < itemCount; i++) {
            int row = i / 9;
            int col = i % 9;

            // Checkerboard pattern
            if ((row + col) % 2 == 0) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(i -> i).toArray();
    }

    // === EVENT FIRING METHODS ===

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
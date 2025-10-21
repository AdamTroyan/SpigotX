package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import dev.adam.gui.context.GUIClickContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Core GUI implementation for creating interactive inventory interfaces.
 * 
 * This class provides a comprehensive foundation for building custom inventory GUIs
 * with advanced features like click handlers, permissions, properties, and lifecycle
 * management. It supports both simple and complex inventory layouts while maintaining
 * excellent performance and flexibility.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Flexible item placement with per-slot click handlers</li>
 *   <li>Permission-based access control for individual slots</li>
 *   <li>Property system for storing custom data</li>
 *   <li>Pattern-based item placement using ASCII layouts</li>
 *   <li>Geometric filling methods (rectangles, circles, borders)</li>
 *   <li>Conditional item placement with predicates</li>
 *   <li>Comprehensive item management and manipulation</li>
 *   <li>Global and per-slot click handling</li>
 *   <li>Customizable open/close conditions and callbacks</li>
 *   <li>Background item management with auto-fill</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * GUI gui = new GUI("My Shop", 6);
 * 
 * // Add items with click handlers
 * gui.setItem(10, GUI.createItem(Material.DIAMOND, "&bDiamond"), ctx -> {
 *     ctx.getPlayer().sendMessage("You clicked diamond!");
 * });
 * 
 * // Fill border with background
 * gui.fillBorder(GUI.createItem(Material.BLACK_STAINED_GLASS_PANE, " "), null);
 * 
 * // Open for a player
 * gui.open(player);
 * }</pre>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class GUI implements GUIBase {
    
    /** The title displayed at the top of the inventory */
    private final String title;
    /** The number of rows in the inventory */
    private final int rows;
    /** The underlying Bukkit inventory */
    private final Inventory inventory;
    
    /** Map of slot positions to their click handlers */
    private final Map<Integer, Consumer<GUIClickContext>> clickHandlers = new HashMap<>();
    /** Map of slot positions to required permissions */
    private final Map<Integer, String> slotPermissions = new HashMap<>();
    /** Map storing original items for reset functionality */
    private final Map<Integer, ItemStack> originalItems = new HashMap<>();
    /** Set of slot positions that are protected from modification */
    private final Set<Integer> protectedSlots = new HashSet<>();
    /** Map for storing custom properties */
    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    /** Callback executed when a player opens the GUI */
    private Consumer<Player> onOpen;
    /** Callback executed when a player closes the GUI */
    private Consumer<Player> onClose;
    /** Global click handler executed before slot-specific handlers */
    private Consumer<GUIClickContext> globalClickHandler;
    /** Condition that must be met for players to open the GUI */
    private Predicate<Player> openCondition;

    /** Whether to automatically refresh the GUI at intervals */
    private boolean autoRefresh = false;
    /** Interval in ticks between automatic refreshes */
    private long refreshInterval = 20L;
    /** Whether players can click items in their own inventory while GUI is open */
    private boolean allowPlayerInventoryClick = false;
    /** Sound to play when the GUI is closed */
    private String closeSound = "UI_BUTTON_CLICK";
    /** Default background item for empty slots */
    private ItemStack backgroundItem;

    // === CONSTRUCTOR ===

    /**
     * Creates a new GUI with the specified title and number of rows.
     * 
     * @param title the title displayed at the top of the inventory
     * @param rows the number of rows in the inventory (1-6)
     */
    public GUI(String title, int rows) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows)); // Clamp between 1-6
        this.inventory = Bukkit.createInventory(this, this.rows * 9, title);
        this.backgroundItem = createDefaultBackground();
    }

    // === BASIC ITEM MANAGEMENT ===

    /**
     * Sets an item at the specified slot with a click handler.
     * 
     * @param slot the slot position (0-based)
     * @param item the ItemStack to place (null to clear)
     * @param onClick the click handler for this item (null for no handler)
     */
    public void setItem(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        if (!isValidSlot(slot)) return;

        // Store original item for reset functionality
        if (item != null) {
            originalItems.put(slot, item.clone());
        }

        inventory.setItem(slot, item);
        
        if (onClick != null) {
            clickHandlers.put(slot, onClick);
        } else {
            clickHandlers.remove(slot);
        }
    }

    /**
     * Sets an item with permission requirement and click handler.
     * 
     * @param slot the slot position
     * @param item the ItemStack to place
     * @param permission the permission required to click this item (null for no requirement)
     * @param onClick the click handler for this item
     */
    public void setItem(int slot, ItemStack item, String permission, Consumer<GUIClickContext> onClick) {
        setItem(slot, item, onClick);
        if (permission != null && !permission.isEmpty()) {
            slotPermissions.put(slot, permission);
        }
    }

    /**
     * Removes an item and all associated data from the specified slot.
     * This includes the item, click handler, permissions, and protection status.
     * 
     * @param slot the slot to clear
     */
    public void removeItem(int slot) {
        if (!isValidSlot(slot)) return;
        
        inventory.setItem(slot, null);
        clickHandlers.remove(slot);
        slotPermissions.remove(slot);
        originalItems.remove(slot);
        protectedSlots.remove(slot);
    }

    /**
     * Sets an item as protected, preventing it from being modified by certain operations.
     * 
     * @param slot the slot position
     * @param item the ItemStack to place
     * @param onClick the click handler for this item
     */
    public void setProtectedItem(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        setItem(slot, item, onClick);
        protectedSlots.add(slot);
    }

    /**
     * Marks a slot as protected from modification.
     * 
     * @param slot the slot to protect
     */
    public void protectSlot(int slot) {
        if (isValidSlot(slot)) {
            protectedSlots.add(slot);
        }
    }

    /**
     * Removes protection from a slot.
     * 
     * @param slot the slot to unprotect
     */
    public void unprotectSlot(int slot) {
        protectedSlots.remove(slot);
    }

    /**
     * Checks if a slot is protected from modification.
     * 
     * @param slot the slot to check
     * @return true if the slot is protected, false otherwise
     */
    public boolean isProtected(int slot) {
        return protectedSlots.contains(slot);
    }

    // === BULK ITEM OPERATIONS ===

    /**
     * Sets multiple items with the same click handler.
     * 
     * @param items map of slot positions to ItemStacks
     * @param onClick the click handler for all items
     */
    public void setItems(Map<Integer, ItemStack> items, Consumer<GUIClickContext> onClick) {
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            setItem(entry.getKey(), entry.getValue(), onClick);
        }
    }

    /**
     * Sets multiple items with individual click handlers.
     * 
     * @param items map of slot positions to ItemStacks
     * @param handlers map of slot positions to click handlers
     */
    public void setItemsWithHandlers(Map<Integer, ItemStack> items, Map<Integer, Consumer<GUIClickContext>> handlers) {
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            int slot = entry.getKey();
            Consumer<GUIClickContext> handler = handlers != null ? handlers.get(slot) : null;
            setItem(slot, entry.getValue(), handler);
        }
    }

    /**
     * Sets items at multiple slots using an array.
     * 
     * @param slots array of slot positions
     * @param item the ItemStack to place in all slots
     * @param onClick the click handler for all items
     */
    public void setItemsBulk(int[] slots, ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int slot : slots) {
            setItem(slot, item, onClick);
        }
    }

    // === PATTERN-BASED PLACEMENT ===

    /**
     * Sets items based on an ASCII pattern layout.
     * Each character in the pattern corresponds to a different item type.
     * 
     * @param pattern array of strings representing rows of the inventory
     * @param mapping map of characters to ItemStacks
     * @param handlers map of characters to click handlers (can be null)
     */
    public void setPattern(String[] pattern, Map<Character, ItemStack> mapping, Map<Character, Consumer<GUIClickContext>> handlers) {
        int slot = 0;
        for (String row : pattern) {
            for (char c : row.toCharArray()) {
                if (slot >= inventory.getSize()) break;

                ItemStack item = mapping.get(c);
                Consumer<GUIClickContext> handler = handlers != null ? handlers.get(c) : null;

                if (item != null) {
                    setItem(slot, item, handler);
                } else if (c == ' ' && backgroundItem != null) {
                    setItem(slot, backgroundItem, null);
                }
                slot++;
            }
        }
    }

    // === GEOMETRIC FILLING ===

    /**
     * Fills a rectangular area with the specified item.
     * 
     * @param topLeft the top-left slot of the rectangle
     * @param bottomRight the bottom-right slot of the rectangle
     * @param item the ItemStack to place
     * @param onClick the click handler for all items
     */
    public void fillRectangle(int topLeft, int bottomRight, ItemStack item, Consumer<GUIClickContext> onClick) {
        int startRow = topLeft / 9;
        int startCol = topLeft % 9;
        int endRow = bottomRight / 9;
        int endCol = bottomRight % 9;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int slot = row * 9 + col;
                if (slot < inventory.getSize()) {
                    setItem(slot, item, onClick);
                }
            }
        }
    }

    /**
     * Fills a diamond shape around a center point using Manhattan distance.
     * 
     * @param centerSlot the center slot position
     * @param radius the radius of the diamond
     * @param item the ItemStack to place
     * @param onClick the click handler for all items
     */
    public void fillCircle(int centerSlot, int radius, ItemStack item, Consumer<GUIClickContext> onClick) {
        int centerRow = centerSlot / 9;
        int centerCol = centerSlot % 9;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int distance = Math.abs(row - centerRow) + Math.abs(col - centerCol);
                if (distance <= radius) {
                    int slot = row * 9 + col;
                    setItem(slot, item, onClick);
                }
            }
        }
    }

    /**
     * Fills the border (edges) of the inventory with the specified item.
     * 
     * @param item the ItemStack to place on the border
     * @param onClick the click handler for border items
     */
    public void fillBorder(ItemStack item, Consumer<GUIClickContext> onClick) {
        int size = rows * 9;
        for (int i = 0; i < size; i++) {
            // Check if slot is on the border
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                setItem(i, item, onClick);
            }
        }
    }

    /**
     * Fills empty slots in the inventory with a background item.
     * 
     * @param item the ItemStack to use as background
     * @param onClick the click handler for background items
     */
    public void setBackground(ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                setItem(i, item, onClick);
            }
        }
    }

    // === CONDITIONAL OPERATIONS ===

    /**
     * Conditionally sets an item based on a predicate.
     * 
     * @param slot the slot position
     * @param item the ItemStack to place
     * @param condition the condition that must be met
     * @param onClick the click handler
     */
    public void setItemIf(int slot, ItemStack item, Predicate<GUI> condition, Consumer<GUIClickContext> onClick) {
        if (condition.test(this)) {
            setItem(slot, item, onClick);
        }
    }

    /**
     * Sets an item only if the slot is currently empty.
     * 
     * @param slot the slot position
     * @param item the ItemStack to place
     * @param onClick the click handler
     */
    public void setItemIfEmpty(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        if (isValidSlot(slot) && inventory.getItem(slot) == null) {
            setItem(slot, item, onClick);
        }
    }

    // === ITEM MANIPULATION ===

    /**
     * Swaps items and handlers between two slots.
     * 
     * @param slot1 the first slot
     * @param slot2 the second slot
     */
    public void swapItems(int slot1, int slot2) {
        if (!isValidSlot(slot1) || !isValidSlot(slot2)) return;

        ItemStack item1 = inventory.getItem(slot1);
        ItemStack item2 = inventory.getItem(slot2);
        Consumer<GUIClickContext> handler1 = clickHandlers.get(slot1);
        Consumer<GUIClickContext> handler2 = clickHandlers.get(slot2);

        setItem(slot1, item2, handler2);
        setItem(slot2, item1, handler1);
    }

    /**
     * Moves an item and its handler from one slot to another.
     * 
     * @param fromSlot the source slot
     * @param toSlot the destination slot
     */
    public void moveItem(int fromSlot, int toSlot) {
        if (!isValidSlot(fromSlot) || !isValidSlot(toSlot)) return;

        ItemStack item = inventory.getItem(fromSlot);
        Consumer<GUIClickContext> handler = clickHandlers.get(fromSlot);

        removeItem(fromSlot);
        setItem(toSlot, item, handler);
    }

    /**
     * Creates a clone of the item at the specified slot.
     * 
     * @param slot the slot to clone from
     * @return a cloned ItemStack, or null if slot is empty or invalid
     */
    public ItemStack cloneItem(int slot) {
        if (!isValidSlot(slot)) return null;
        ItemStack item = inventory.getItem(slot);
        return item != null ? item.clone() : null;
    }

    /**
     * Replaces all items of a specific material with a new item.
     * 
     * @param from the material to replace
     * @param to the new ItemStack to place
     * @param onClick the click handler for replaced items
     */
    public void replaceItem(Material from, ItemStack to, Consumer<GUIClickContext> onClick) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack current = inventory.getItem(i);
            if (current != null && current.getType() == from) {
                setItem(i, to, onClick);
            }
        }
    }

    // === SEARCH AND ANALYSIS ===

    /**
     * Finds all slots containing items of the specified material.
     * 
     * @param material the material to search for
     * @return a list of slot positions containing the material
     */
    public List<Integer> findItems(Material material) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material) {
                slots.add(i);
            }
        }
        return slots;
    }

    /**
     * Finds all empty slots in the inventory.
     * 
     * @return a list of empty slot positions
     */
    public List<Integer> findEmptySlots() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) {
                slots.add(i);
            }
        }
        return slots;
    }

    /**
     * Gets the first slot containing an item of the specified material.
     * 
     * @param material the material to search for
     * @return the first slot position, or -1 if not found
     */
    public int getFirstSlotWithItem(Material material) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the first empty slot in the specified row.
     * 
     * @param row the row number (1-based)
     * @return the first empty slot in the row, or -1 if row is full or invalid
     */
    public int getFirstEmptySlotInRow(int row) {
        if (row < 1 || row > rows) return -1;

        int start = (row - 1) * 9;
        for (int i = start; i < start + 9 && i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                return i;
            }
        }
        return -1;
    }

    // === PROPERTY SYSTEM ===

    /**
     * Sets a custom property for this GUI.
     * Properties can be used to store any custom data.
     * 
     * @param key the property key
     * @param value the property value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Gets a property value cast to the specified type.
     * 
     * @param <T> the expected type of the property
     * @param key the property key
     * @param type the expected class of the property
     * @return the property value, or null if not found or wrong type
     */
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * Gets a property value with a default fallback.
     * 
     * @param <T> the type of the property
     * @param key the property key
     * @param defaultValue the default value if property is not found
     * @param type the expected class of the property
     * @return the property value, or the default value if not found
     */
    public <T> T getProperty(String key, T defaultValue, Class<T> type) {
        T value = getProperty(key, type);
        return value != null ? value : defaultValue;
    }

    /**
     * Removes a property from this GUI.
     * 
     * @param key the property key to remove
     */
    public void removeProperty(String key) {
        properties.remove(key);
    }

    /**
     * Checks if a property exists.
     * 
     * @param key the property key to check
     * @return true if the property exists, false otherwise
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    // === RESET AND CLEANUP ===

    /**
     * Resets a slot to its original item and handler.
     * 
     * @param slot the slot to reset
     */
    public void resetSlot(int slot) {
        ItemStack original = originalItems.get(slot);
        if (original != null) {
            setItem(slot, original.clone(), clickHandlers.get(slot));
        }
    }

    /**
     * Resets all slots to their original items and handlers.
     */
    public void resetAllSlots() {
        for (int slot : new HashSet<>(originalItems.keySet())) {
            resetSlot(slot);
        }
    }

    /**
     * Clears all slots, removing items and handlers.
     */
    public void clearAllSlots() {
        for (int i = 0; i < inventory.getSize(); i++) {
            removeItem(i);
        }
    }

    // === CONFIGURATION ===

    /**
     * Sets a condition that must be met for players to open this GUI.
     * 
     * @param condition the predicate to test against players
     */
    public void setOpenCondition(Predicate<Player> condition) {
        this.openCondition = condition;
    }

    /**
     * Sets a global click handler that is executed before slot-specific handlers.
     * 
     * @param handler the global click handler
     */
    public void setGlobalClickHandler(Consumer<GUIClickContext> handler) {
        this.globalClickHandler = handler;
    }

    /**
     * Configures automatic refreshing of the GUI.
     * 
     * @param autoRefresh whether to enable auto-refresh
     * @param intervalTicks the interval between refreshes in ticks
     */
    public void setAutoRefresh(boolean autoRefresh, long intervalTicks) {
        this.autoRefresh = autoRefresh;
        this.refreshInterval = Math.max(1, intervalTicks);
    }

    /**
     * Sets whether players can click items in their own inventory while this GUI is open.
     * 
     * @param allow true to allow player inventory clicks, false to block them
     */
    public void setAllowPlayerInventoryClick(boolean allow) {
        this.allowPlayerInventoryClick = allow;
    }

    /**
     * Sets the sound to play when the GUI is closed.
     * 
     * @param sound the sound name (Bukkit Sound enum name)
     */
    public void setCloseSound(String sound) {
        this.closeSound = sound;
    }

    /**
     * Sets the default background item and fills empty slots with it.
     * 
     * @param item the ItemStack to use as background
     */
    public void setBackgroundItem(ItemStack item) {
        this.backgroundItem = item;
        if (item != null) {
            setBackground(item, null);
        }
    }

    /**
     * Sets the callback to execute when a player opens this GUI.
     * 
     * @param onOpen the callback consumer
     */
    public void setOnOpen(Consumer<Player> onOpen) {
        this.onOpen = onOpen;
    }

    /**
     * Sets the callback to execute when a player closes this GUI.
     * 
     * @param onClose the callback consumer
     */
    public void setOnClose(Consumer<Player> onClose) {
        this.onClose = onClose;
    }

    // === STATISTICS ===

    /**
     * Gets the total number of slots in this inventory.
     * 
     * @return the slot count (rows * 9)
     */
    public int getSlotCount() {
        return rows * 9;
    }

    /**
     * Gets the number of slots that contain items.
     * 
     * @return the filled slot count
     */
    public int getFilledSlotCount() {
        int count = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the number of empty slots.
     * 
     * @return the empty slot count
     */
    public int getEmptySlotCount() {
        return getSlotCount() - getFilledSlotCount();
    }

    /**
     * Checks if the inventory is completely full.
     * 
     * @return true if no empty slots remain, false otherwise
     */
    public boolean isFull() {
        return getEmptySlotCount() == 0;
    }

    /**
     * Checks if the inventory is completely empty.
     * 
     * @return true if no items are present, false otherwise
     */
    public boolean isEmpty() {
        return getFilledSlotCount() == 0;
    }

    // === UTILITY METHODS ===

    /**
     * Creates a default background glass pane item.
     * 
     * @return a gray stained glass pane with empty display name
     */
    private ItemStack createDefaultBackground() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates an ItemStack with display name and lore.
     * Supports color codes using 'and' character.
     * 
     * @param material the material type
     * @param name the display name (supports color codes)
     * @param lore optional lore lines (support color codes)
     * @return the created ItemStack
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
            }
            if (lore != null && lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // === EVENT HANDLING ===

    /**
     * Handles inventory click events for this GUI.
     * Processes permission checks, global handlers, and slot-specific handlers.
     */
    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;

        event.setCancelled(true); // Cancel by default to prevent item movement

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        GUIClickContext context = new GUIClickContext(event);

        // Check slot permissions
        String permission = slotPermissions.get(slot);
        if (permission != null && !player.hasPermission(permission)) {
            context.error("You don't have permission to use this!");
            return;
        }

        // Execute global click handler first
        if (globalClickHandler != null) {
            try {
                globalClickHandler.accept(context);
                if (context.isConsumed()) return;
            } catch (Exception e) {
                context.error("An error occurred while processing your click!");
                e.printStackTrace();
            }
        }

        // Execute slot-specific handler
        Consumer<GUIClickContext> handler = clickHandlers.get(slot);
        if (handler != null) {
            try {
                handler.accept(context);
            } catch (Exception e) {
                context.error("An error occurred while processing your click!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles inventory close events for this GUI.
     * Executes the close callback and plays the close sound.
     */
    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;

        Player player = (Player) event.getPlayer();
        
        // Execute close callback
        if (onClose != null) {
            try {
                onClose.accept(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Play close sound
        if (closeSound != null) {
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(closeSound);
                player.playSound(player.getLocation(), sound, 0.5f, 1.0f);
            } catch (IllegalArgumentException ignored) {
                // Invalid sound name, ignore
            }
        }
    }

    // === PLAYER INTERACTION ===

    /**
     * Opens this GUI for the specified player.
     * Checks open conditions and executes the open callback.
     * 
     * @param player the player to open the GUI for
     */
    public void open(Player player) {
        if (player == null || !player.isOnline()) return;

        // Check open condition
        if (openCondition != null && !openCondition.test(player)) {
            player.sendMessage("§cYou cannot open this GUI right now!");
            return;
        }

        // Open the inventory
        player.openInventory(inventory);

        // Execute open callback
        if (onOpen != null) {
            try {
                onOpen.accept(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes this GUI for the specified player.
     * 
     * @param player the player to close the GUI for
     */
    public void close(Player player) {
        if (player != null && player.isOnline()) {
            player.closeInventory();
        }
    }

    // === GETTERS ===

    /**
     * Gets the title of this GUI.
     * 
     * @return the GUI title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the number of rows in this GUI.
     * 
     * @return the row count
     */
    public int getRows() {
        return rows;
    }

    /**
     * Gets the open callback.
     * 
     * @return the open callback consumer
     */
    public Consumer<Player> getOnOpen() {
        return onOpen;
    }

    /**
     * Gets the close callback.
     * 
     * @return the close callback consumer
     */
    public Consumer<Player> getOnClose() {
        return onClose;
    }

    /**
     * Checks if auto-refresh is enabled.
     * 
     * @return true if auto-refresh is enabled, false otherwise
     */
    public boolean isAutoRefresh() {
        return autoRefresh;
    }

    /**
     * Gets the auto-refresh interval.
     * 
     * @return the refresh interval in ticks
     */
    public long getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * Checks if player inventory clicks are allowed.
     * 
     * @return true if allowed, false otherwise
     */
    public boolean isAllowPlayerInventoryClick() {
        return allowPlayerInventoryClick;
    }

    /**
     * Gets the close sound name.
     * 
     * @return the close sound name
     */
    public String getCloseSound() {
        return closeSound;
    }

    /**
     * Gets the background item.
     * 
     * @return the background ItemStack
     */
    public ItemStack getBackgroundItem() {
        return backgroundItem;
    }

    /**
     * Gets the click handler for a specific slot.
     * 
     * @param slot the slot position
     * @return the click handler, or null if none exists
     */
    public Consumer<GUIClickContext> getHandler(int slot) {
        return clickHandlers.get(slot);
    }

    /**
     * Removes the click handler from a specific slot.
     * 
     * @param slot the slot position
     */
    public void removeHandler(int slot) {
        clickHandlers.remove(slot);
    }

    // === GUIBASE IMPLEMENTATION ===

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasHandler(int slot) {
        return clickHandlers.containsKey(slot);
    }
}
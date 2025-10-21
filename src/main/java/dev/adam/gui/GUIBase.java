package dev.adam.gui;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

/**
 * Base interface for all GUI implementations in the SpigotX framework.
 * 
 * This interface provides a comprehensive foundation for creating custom inventory
 * GUIs with standardized behavior, event handling, and utility methods. It extends
 * Bukkit's InventoryHolder to ensure compatibility with the inventory system while
 * adding advanced functionality for GUI management.
 * 
 * <p>Key features provided by this interface:</p>
 * <ul>
 *   <li>Standardized event handling for click, open, close, drag, and move events</li>
 *   <li>Player access control with customizable permission checks</li>
 *   <li>Comprehensive slot validation and coordinate conversion utilities</li>
 *   <li>Advanced item search and pattern matching capabilities</li>
 *   <li>Viewer management with broadcasting and notification systems</li>
 *   <li>Statistical analysis and debugging tools</li>
 *   <li>Type-safe casting and conversion methods</li>
 *   <li>Fluent API support with method chaining</li>
 * </ul>
 * 
 * <p>This interface is implemented by:</p>
 * <ul>
 *   <li>{@link GUI} - Basic single-page GUI implementation</li>
 *   <li>{@link PaginatedGUI} - Multi-page GUI with navigation</li>
 *   <li>{@link GUIBuilder} - Fluent builder for constructing GUIs</li>
 * </ul>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public interface GUIBase extends InventoryHolder {

    // === EVENT HANDLING ===

    /**
     * Checks if this GUI has a click handler for the specified slot.
     * 
     * @param slot the slot position to check
     * @return true if a handler exists for the slot, false otherwise
     */
    default boolean hasHandler(int slot) {
        return false;
    }

    /**
     * Handles inventory click events for this GUI.
     * This method is called by the GUI listener when a player clicks in the inventory.
     * 
     * @param event the InventoryClickEvent to handle
     */
    default void handleClick(InventoryClickEvent event) {
        // Default implementation does nothing
    }

    /**
     * Handles inventory close events for this GUI.
     * This method is called when a player closes the inventory.
     * 
     * @param event the InventoryCloseEvent to handle
     */
    default void handleClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        onGUIClose(player);
    }

    /**
     * Handles inventory open events for this GUI.
     * This method is called when a player opens the inventory.
     * 
     * @param event the InventoryOpenEvent to handle
     */
    default void handleOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        onGUIOpen(player);
    }

    /**
     * Handles inventory drag events to prevent unwanted item dragging.
     * By default, all drag events are cancelled to maintain GUI integrity.
     * 
     * @param event the InventoryDragEvent to handle
     */
    default void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    /**
     * Handles inventory move events to prevent automatic item movement.
     * By default, all move events are cancelled to maintain GUI control.
     * 
     * @param event the InventoryMoveItemEvent to handle
     */
    default void handleMove(InventoryMoveItemEvent event) {
        event.setCancelled(true);
    }

    // === LIFECYCLE CALLBACKS ===

    /**
     * Called when a player opens this GUI.
     * Override this method to implement custom open behavior.
     * 
     * @param player the player who opened the GUI
     */
    default void onGUIOpen(Player player) {
        // Default implementation does nothing
    }

    /**
     * Called when a player closes this GUI.
     * Override this method to implement custom close behavior.
     * 
     * @param player the player who closed the GUI
     */
    default void onGUIClose(Player player) {
        // Default implementation does nothing
    }

    /**
     * Called when a player disconnects while viewing this GUI.
     * Override this method to handle cleanup when players disconnect.
     * 
     * @param player the player who disconnected
     */
    default void onPlayerDisconnect(Player player) {
        // Default implementation does nothing
    }

    // === ACCESS CONTROL ===

    /**
     * Checks if a player is allowed to open this GUI.
     * Override this method to implement custom access control.
     * 
     * @param player the player requesting access
     * @return true if the player can open the GUI, false otherwise
     */
    default boolean canPlayerOpen(Player player) {
        return player != null && player.isOnline();
    }

    /**
     * Checks if a player is allowed to click a specific slot.
     * Override this method to implement slot-specific permissions.
     * 
     * @param player the player attempting to click
     * @param slot the slot being clicked
     * @return true if the player can click the slot, false otherwise
     */
    default boolean canPlayerClick(Player player, int slot) {
        return canPlayerOpen(player);
    }

    // === INVENTORY ACCESS ===

    /**
     * Gets the underlying Bukkit inventory for this GUI.
     * This method is required by the InventoryHolder interface.
     * 
     * @return the inventory instance
     */
    @Override
    Inventory getInventory();

    // === SIZE AND STRUCTURE ===

    /**
     * Checks if a slot number is valid for this inventory.
     * 
     * @param slot the slot number to validate
     * @return true if the slot is within bounds, false otherwise
     */
    default boolean isValidSlot(int slot) {
        return slot >= 0 && slot < getInventory().getSize();
    }

    /**
     * Gets the total number of slots in this inventory.
     * 
     * @return the inventory size
     */
    default int getSize() {
        return getInventory().getSize();
    }

    /**
     * Gets the number of rows in this inventory.
     * 
     * @return the number of rows (size divided by 9)
     */
    default int getRows() {
        return getSize() / 9;
    }

    /**
     * Gets the title of this inventory.
     * 
     * @return the inventory title, or default title if not set
     */
    default String getTitle() {
        return getInventory().getType().getDefaultTitle();
    }

    // === VIEWER MANAGEMENT ===

    /**
     * Gets a list of all players currently viewing this GUI.
     * 
     * @return a list of viewing players
     */
    default List<Player> getViewers() {
        return new ArrayList<>(getInventory().getViewers().stream()
                .filter(humanEntity -> humanEntity instanceof Player)
                .map(humanEntity -> (Player) humanEntity)
                .toList());
    }

    /**
     * Gets the number of players currently viewing this GUI.
     * 
     * @return the viewer count
     */
    default int getViewerCount() {
        return getViewers().size();
    }

    /**
     * Checks if this GUI has any viewers.
     * 
     * @return true if there are viewers, false otherwise
     */
    default boolean hasViewers() {
        return getViewerCount() > 0;
    }

    /**
     * Checks if a specific player is viewing this GUI.
     * 
     * @param player the player to check
     * @return true if the player is viewing this GUI, false otherwise
     */
    default boolean isViewing(Player player) {
        return getViewers().contains(player);
    }

    // === ITEM MANAGEMENT ===

    /**
     * Gets the item at the specified slot.
     * 
     * @param slot the slot position
     * @return the ItemStack at the slot, or null if empty or invalid
     */
    default ItemStack getItem(int slot) {
        if (!isValidSlot(slot)) return null;
        return getInventory().getItem(slot);
    }

    /**
     * Sets an item at the specified slot.
     * 
     * @param slot the slot position
     * @param item the ItemStack to place (null to clear)
     */
    default void setItem(int slot, ItemStack item) {
        if (isValidSlot(slot)) {
            getInventory().setItem(slot, item);
        }
    }

    /**
     * Removes the item from the specified slot.
     * 
     * @param slot the slot to clear
     */
    default void removeItem(int slot) {
        setItem(slot, null);
    }

    /**
     * Checks if a slot contains an item.
     * 
     * @param slot the slot to check
     * @return true if the slot has a non-null, non-air item
     */
    default boolean hasItem(int slot) {
        ItemStack item = getItem(slot);
        return item != null && !item.getType().isAir();
    }

    // === SLOT ANALYSIS ===

    /**
     * Gets a list of all empty slot positions.
     * 
     * @return a list of empty slot indices
     */
    default List<Integer> getEmptySlots() {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            if (!hasItem(i)) {
                empty.add(i);
            }
        }
        return empty;
    }

    /**
     * Gets a list of all filled slot positions.
     * 
     * @return a list of filled slot indices
     */
    default List<Integer> getFilledSlots() {
        List<Integer> filled = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            if (hasItem(i)) {
                filled.add(i);
            }
        }
        return filled;
    }

    /**
     * Gets the number of empty slots.
     * 
     * @return the count of empty slots
     */
    default int getEmptySlotCount() {
        return getEmptySlots().size();
    }

    /**
     * Gets the number of filled slots.
     * 
     * @return the count of filled slots
     */
    default int getFilledSlotCount() {
        return getFilledSlots().size();
    }

    /**
     * Checks if the inventory is completely full.
     * 
     * @return true if no empty slots remain, false otherwise
     */
    default boolean isFull() {
        return getEmptySlotCount() == 0;
    }

    /**
     * Checks if the inventory is completely empty.
     * 
     * @return true if no items are present, false otherwise
     */
    default boolean isEmpty() {
        return getFilledSlotCount() == 0;
    }

    // === COORDINATE CONVERSION ===

    /**
     * Converts a slot number to its row position.
     * 
     * @param slot the slot number
     * @return the row number (0-based)
     */
    default int slotToRow(int slot) {
        return slot / 9;
    }

    /**
     * Converts a slot number to its column position.
     * 
     * @param slot the slot number
     * @return the column number (0-based)
     */
    default int slotToColumn(int slot) {
        return slot % 9;
    }

    /**
     * Converts row and column coordinates to a slot number.
     * 
     * @param row the row position (0-based)
     * @param column the column position (0-based)
     * @return the corresponding slot number
     */
    default int coordinateToSlot(int row, int column) {
        return row * 9 + column;
    }

    /**
     * Checks if row and column coordinates are valid for this inventory.
     * 
     * @param row the row position
     * @param column the column position
     * @return true if coordinates are within bounds, false otherwise
     */
    default boolean isValidCoordinate(int row, int column) {
        return row >= 0 && row < getRows() && column >= 0 && column < 9;
    }

    /**
     * Gets the item at the specified row and column coordinates.
     * 
     * @param row the row position
     * @param column the column position
     * @return the ItemStack, or null if coordinates are invalid
     */
    default ItemStack getItemAt(int row, int column) {
        if (!isValidCoordinate(row, column)) return null;
        return getItem(coordinateToSlot(row, column));
    }

    /**
     * Sets an item at the specified row and column coordinates.
     * 
     * @param row the row position
     * @param column the column position
     * @param item the ItemStack to place
     */
    default void setItemAt(int row, int column, ItemStack item) {
        if (isValidCoordinate(row, column)) {
            setItem(coordinateToSlot(row, column), item);
        }
    }

    // === POSITIONAL ANALYSIS ===

    /**
     * Checks if a slot is on the border (edge) of the inventory.
     * 
     * @param slot the slot to check
     * @return true if the slot is on the border, false otherwise
     */
    default boolean isBorderSlot(int slot) {
        if (!isValidSlot(slot)) return false;

        int row = slotToRow(slot);
        int col = slotToColumn(slot);

        return row == 0 || row == getRows() - 1 || col == 0 || col == 8;
    }

    /**
     * Checks if a slot is in a corner of the inventory.
     * 
     * @param slot the slot to check
     * @return true if the slot is a corner, false otherwise
     */
    default boolean isCornerSlot(int slot) {
        if (!isValidSlot(slot)) return false;

        int row = slotToRow(slot);
        int col = slotToColumn(slot);

        return (row == 0 || row == getRows() - 1) && (col == 0 || col == 8);
    }

    /**
     * Checks if a slot is the center slot of the inventory.
     * 
     * @param slot the slot to check
     * @return true if the slot is the center, false otherwise
     */
    default boolean isCenterSlot(int slot) {
        if (!isValidSlot(slot)) return false;

        int row = slotToRow(slot);
        int col = slotToColumn(slot);
        int centerRow = getRows() / 2;
        int centerCol = 4;

        return row == centerRow && col == centerCol;
    }

    // === INVENTORY OPERATIONS ===

    /**
     * Forces all viewers to refresh their inventory view.
     * This updates the client-side inventory display.
     */
    default void refresh() {
        for (Player viewer : getViewers()) {
            viewer.updateInventory();
        }
    }

    /**
     * Forces all players to close this inventory.
     */
    default void forceClose() {
        List<Player> viewers = new ArrayList<>(getViewers());
        for (Player viewer : viewers) {
            viewer.closeInventory();
        }
    }

    /**
     * Forces all players except the specified one to close this inventory.
     * 
     * @param exception the player who should not be forced to close
     */
    default void forceCloseExcept(Player exception) {
        List<Player> viewers = new ArrayList<>(getViewers());
        for (Player viewer : viewers) {
            if (!viewer.equals(exception)) {
                viewer.closeInventory();
            }
        }
    }

    /**
     * Clears all items from the inventory.
     */
    default void clearAll() {
        for (int i = 0; i < getSize(); i++) {
            removeItem(i);
        }
    }

    /**
     * Fills all slots with the specified item.
     * 
     * @param item the ItemStack to place in all slots
     */
    default void fillAll(ItemStack item) {
        for (int i = 0; i < getSize(); i++) {
            setItem(i, item);
        }
    }

    /**
     * Copies all items from another GUI to this one.
     * Only copies up to the smaller inventory size.
     * 
     * @param other the GUI to copy items from
     */
    default void copyFrom(GUIBase other) {
        int maxSlots = Math.min(getSize(), other.getSize());
        for (int i = 0; i < maxSlots; i++) {
            ItemStack item = other.getItem(i);
            setItem(i, item != null ? item.clone() : null);
        }
    }

    // === ITEM SEARCH AND ANALYSIS ===

    /**
     * Finds all slots containing items of the specified material.
     * 
     * @param material the material to search for
     * @return a list of slot positions containing the material
     */
    default List<Integer> findSlots(Material material) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            ItemStack item = getItem(i);
            if (item != null && item.getType() == material) {
                slots.add(i);
            }
        }
        return slots;
    }

    /**
     * Finds the first slot containing the specified material.
     * 
     * @param material the material to search for
     * @return the first slot position, or -1 if not found
     */
    default int findFirstSlot(Material material) {
        List<Integer> slots = findSlots(material);
        return slots.isEmpty() ? -1 : slots.get(0);
    }

    /**
     * Checks if the inventory contains any items of the specified material.
     * 
     * @param material the material to check for
     * @return true if the material is found, false otherwise
     */
    default boolean containsItem(Material material) {
        return findFirstSlot(material) != -1;
    }

    /**
     * Counts the total amount of items of the specified material.
     * 
     * @param material the material to count
     * @return the total count of items of this material
     */
    default int countItems(Material material) {
        int count = 0;
        for (int i = 0; i < getSize(); i++) {
            ItemStack item = getItem(i);
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Checks if a pattern of materials matches starting at the specified slot.
     * 
     * @param startSlot the starting slot position
     * @param pattern a 2D array representing the expected material pattern
     * @return true if the pattern matches, false otherwise
     */
    default boolean matchesPattern(int startSlot, Material[][] pattern) {
        int startRow = slotToRow(startSlot);
        int startCol = slotToColumn(startSlot);

        for (int row = 0; row < pattern.length; row++) {
            for (int col = 0; col < pattern[row].length; col++) {
                int checkRow = startRow + row;
                int checkCol = startCol + col;

                if (!isValidCoordinate(checkRow, checkCol)) return false;

                ItemStack item = getItemAt(checkRow, checkCol);
                Material expected = pattern[row][col];

                if (expected == null) {
                    if (item != null && !item.getType().isAir()) return false;
                } else {
                    if (item == null || item.getType() != expected) return false;
                }
            }
        }
        return true;
    }

    // === STATISTICS AND MONITORING ===

    /**
     * Gets statistics about items in the inventory.
     * 
     * @return a map of materials to their total counts
     */
    default Map<Material, Integer> getItemStatistics() {
        Map<Material, Integer> stats = new HashMap<>();
        for (int i = 0; i < getSize(); i++) {
            ItemStack item = getItem(i);
            if (item != null && !item.getType().isAir()) {
                stats.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        return stats;
    }

    /**
     * Prints comprehensive statistics about this GUI to the console.
     * Useful for debugging and monitoring.
     */
    default void printStatistics() {
        System.out.println("=== GUI Statistics ===");
        System.out.println("Title: " + getTitle());
        System.out.println("Size: " + getSize() + " slots (" + getRows() + " rows)");
        System.out.println("Viewers: " + getViewerCount());
        System.out.println("Filled slots: " + getFilledSlotCount());
        System.out.println("Empty slots: " + getEmptySlotCount());

        Map<Material, Integer> stats = getItemStatistics();
        if (!stats.isEmpty()) {
            System.out.println("Items:");
            stats.entrySet().stream()
                    .sorted(Map.Entry.<Material, Integer>comparingByValue().reversed())
                    .forEach(entry -> System.out.println("- " + entry.getKey() + ": " + entry.getValue()));
        }
        System.out.println("======================");
    }

    // === PLAYER COMMUNICATION ===

    /**
     * Sends a message to all players viewing this GUI.
     * 
     * @param message the message to broadcast
     */
    default void broadcastToViewers(String message) {
        for (Player viewer : getViewers()) {
            viewer.sendMessage(message);
        }
    }

    /**
     * Plays a sound to all players viewing this GUI.
     * 
     * @param sound the sound to play
     */
    default void playSoundToViewers(Sound sound) {
        playSoundToViewers(sound, 1.0f, 1.0f);
    }

    /**
     * Plays a sound to all players viewing this GUI with custom volume and pitch.
     * 
     * @param sound the sound to play
     * @param volume the volume level (0.0-1.0)
     * @param pitch the pitch level (0.5-2.0)
     */
    default void playSoundToViewers(Sound sound, float volume, float pitch) {
        for (Player viewer : getViewers()) {
            viewer.playSound(viewer.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Sends a title to all players viewing this GUI.
     * 
     * @param title the main title text
     * @param subtitle the subtitle text
     */
    default void sendTitleToViewers(String title, String subtitle) {
        sendTitleToViewers(title, subtitle, 10, 70, 20);
    }

    /**
     * Sends a title to all players viewing this GUI with custom timing.
     * 
     * @param title the main title text
     * @param subtitle the subtitle text
     * @param fadeIn fade-in time in ticks
     * @param stay display time in ticks
     * @param fadeOut fade-out time in ticks
     */
    default void sendTitleToViewers(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player viewer : getViewers()) {
            viewer.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    // === VALIDATION AND DEBUGGING ===

    /**
     * Validates the integrity of this GUI.
     * Checks for common issues like null inventory or invalid size.
     * 
     * @return true if validation passes, false otherwise
     */
    default boolean validate() {
        if (getInventory() == null) {
            System.err.println("GUI validation failed: inventory is null");
            return false;
        }

        if (getSize() <= 0 || getSize() % 9 != 0) {
            System.err.println("GUI validation failed: invalid size " + getSize());
            return false;
        }

        return true;
    }

    /**
     * Prints detailed debug information about this GUI.
     * Useful for troubleshooting and development.
     */
    default void debug() {
        System.out.println("=== GUI Debug Info ===");
        System.out.println("Class: " + getClass().getSimpleName());
        System.out.println("Title: " + getTitle());
        System.out.println("Size: " + getSize());
        System.out.println("Rows: " + getRows());
        System.out.println("Viewers: " + getViewerCount());
        System.out.println("Has viewers: " + hasViewers());
        System.out.println("Is full: " + isFull());
        System.out.println("Is empty: " + isEmpty());
        System.out.println("Validation: " + (validate() ? "PASSED" : "FAILED"));
        System.out.println("======================");
    }

    // === TYPE CHECKING ===

    /**
     * Checks if this instance is a basic GUI.
     * 
     * @return true if this is a GUI instance, false otherwise
     */
    default boolean isGUI() {
        return this instanceof GUI;
    }

    /**
     * Checks if this instance is a paginated GUI.
     * 
     * @return true if this is a PaginatedGUI instance, false otherwise
     */
    default boolean isPaginatedGUI() {
        return this instanceof PaginatedGUI;
    }

    /**
     * Checks if this instance is a GUI builder.
     * 
     * @return true if this is a GUIBuilder instance, false otherwise
     */
    default boolean isGUIBuilder() {
        return this instanceof GUIBuilder;
    }

    /**
     * Safely casts this instance to a GUI if possible.
     * 
     * @return an Optional containing the GUI, or empty if not a GUI
     */
    default Optional<GUI> asGUI() {
        return this instanceof GUI ? Optional.of((GUI) this) : Optional.empty();
    }

    /**
     * Safely casts this instance to a PaginatedGUI if possible.
     * 
     * @return an Optional containing the PaginatedGUI, or empty if not a PaginatedGUI
     */
    default Optional<PaginatedGUI> asPaginatedGUI() {
        return this instanceof PaginatedGUI ? Optional.of((PaginatedGUI) this) : Optional.empty();
    }

    /**
     * Safely casts this instance to a GUIBuilder if possible.
     * 
     * @return an Optional containing the GUIBuilder, or empty if not a GUIBuilder
     */
    default Optional<GUIBuilder> asGUIBuilder() {
        return this instanceof GUIBuilder ? Optional.of((GUIBuilder) this) : Optional.empty();
    }

    // === FLUENT API SUPPORT ===

    /**
     * Applies an action to this GUI and returns itself for method chaining.
     * This enables fluent API usage patterns.
     * 
     * @param action the action to apply to this GUI
     * @return this GUI instance for method chaining
     */
    default GUIBase then(Consumer<GUIBase> action) {
        action.accept(this);
        return this;
    }

    /**
     * Casts this GUI to the specified type.
     * Throws ClassCastException if the cast is not valid.
     * 
     * @param <T> the target type
     * @param type the class to cast to
     * @return this GUI instance cast to the specified type
     * @throws ClassCastException if the cast is not valid
     */
    default <T extends GUIBase> T as(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new ClassCastException("Cannot cast " + getClass().getSimpleName() + " to " + type.getSimpleName());
    }
}
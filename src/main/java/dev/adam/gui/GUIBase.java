package dev.adam.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public interface GUIBase extends InventoryHolder {

    // **EXISTING METHODS**
    default boolean hasHandler(int slot) { return false; }
    default void handleClick(InventoryClickEvent event) {}
    default void handleClose(InventoryCloseEvent event) {}
    @Override
    Inventory getInventory();

    // **NEW ADVANCED METHODS**

    /**
     * Enhanced event handling
     */
    default void handleOpen(InventoryOpenEvent event) {
        // Default implementation - can be overridden
        Player player = (Player) event.getPlayer();
        onGUIOpen(player);
    }

    default void handleDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        // Default: cancel all drag events in custom GUIs
        event.setCancelled(true);
    }

    default void handleMove(org.bukkit.event.inventory.InventoryMoveItemEvent event) {
        // Default: cancel all move events in custom GUIs
        event.setCancelled(true);
    }

    /**
     * Lifecycle hooks
     */
    default void onGUIOpen(Player player) {
        // Called when GUI is opened
    }

    default void onGUIClose(Player player) {
        // Called when GUI is closed
    }

    default void onPlayerDisconnect(Player player) {
        // Called when player disconnects while GUI is open
    }

    /**
     * Validation methods
     */
    default boolean canPlayerOpen(Player player) {
        return player != null && player.isOnline();
    }

    default boolean canPlayerClick(Player player, int slot) {
        return canPlayerOpen(player);
    }

    default boolean isValidSlot(int slot) {
        return slot >= 0 && slot < getInventory().getSize();
    }

    /**
     * Utility methods
     */
    default int getSize() {
        return getInventory().getSize();
    }

    default int getRows() {
        return getSize() / 9;
    }

    default String getTitle() {
        return getInventory().getType().getDefaultTitle();
    }

    default List<Player> getViewers() {
        return new ArrayList<>(getInventory().getViewers().stream()
                .filter(humanEntity -> humanEntity instanceof Player)
                .map(humanEntity -> (Player) humanEntity)
                .toList());
    }

    default int getViewerCount() {
        return getViewers().size();
    }

    default boolean hasViewers() {
        return getViewerCount() > 0;
    }

    default boolean isViewing(Player player) {
        return getViewers().contains(player);
    }

    /**
     * Item management
     */
    default ItemStack getItem(int slot) {
        if (!isValidSlot(slot)) return null;
        return getInventory().getItem(slot);
    }

    default void setItem(int slot, ItemStack item) {
        if (isValidSlot(slot)) {
            getInventory().setItem(slot, item);
        }
    }

    default void removeItem(int slot) {
        setItem(slot, null);
    }

    default boolean hasItem(int slot) {
        ItemStack item = getItem(slot);
        return item != null && !item.getType().isAir();
    }

    default List<Integer> getEmptySlots() {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            if (!hasItem(i)) {
                empty.add(i);
            }
        }
        return empty;
    }

    default List<Integer> getFilledSlots() {
        List<Integer> filled = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            if (hasItem(i)) {
                filled.add(i);
            }
        }
        return filled;
    }

    default int getEmptySlotCount() {
        return getEmptySlots().size();
    }

    default int getFilledSlotCount() {
        return getFilledSlots().size();
    }

    default boolean isFull() {
        return getEmptySlotCount() == 0;
    }

    default boolean isEmpty() {
        return getFilledSlotCount() == 0;
    }

    /**
     * Coordinate system (row/column)
     */
    default int slotToRow(int slot) {
        return slot / 9;
    }

    default int slotToColumn(int slot) {
        return slot % 9;
    }

    default int coordinateToSlot(int row, int column) {
        return row * 9 + column;
    }

    default boolean isValidCoordinate(int row, int column) {
        return row >= 0 && row < getRows() && column >= 0 && column < 9;
    }

    default ItemStack getItemAt(int row, int column) {
        if (!isValidCoordinate(row, column)) return null;
        return getItem(coordinateToSlot(row, column));
    }

    default void setItemAt(int row, int column, ItemStack item) {
        if (isValidCoordinate(row, column)) {
            setItem(coordinateToSlot(row, column), item);
        }
    }

    /**
     * Border detection
     */
    default boolean isBorderSlot(int slot) {
        if (!isValidSlot(slot)) return false;
        
        int row = slotToRow(slot);
        int col = slotToColumn(slot);
        
        return row == 0 || row == getRows() - 1 || col == 0 || col == 8;
    }

    default boolean isCornerSlot(int slot) {
        if (!isValidSlot(slot)) return false;
        
        int row = slotToRow(slot);
        int col = slotToColumn(slot);
        
        return (row == 0 || row == getRows() - 1) && (col == 0 || col == 8);
    }

    default boolean isCenterSlot(int slot) {
        if (!isValidSlot(slot)) return false;
        
        int row = slotToRow(slot);
        int col = slotToColumn(slot);
        int centerRow = getRows() / 2;
        int centerCol = 4; // Middle column
        
        return row == centerRow && col == centerCol;
    }

    /**
     * Refresh and update methods
     */
    default void refresh() {
        // Default implementation - can be overridden for custom refresh logic
        for (Player viewer : getViewers()) {
            viewer.updateInventory();
        }
    }

    default void forceClose() {
        List<Player> viewers = new ArrayList<>(getViewers());
        for (Player viewer : viewers) {
            viewer.closeInventory();
        }
    }

    default void forceCloseExcept(Player exception) {
        List<Player> viewers = new ArrayList<>(getViewers());
        for (Player viewer : viewers) {
            if (!viewer.equals(exception)) {
                viewer.closeInventory();
            }
        }
    }

    /**
     * Search and find methods
     */
    default List<Integer> findSlots(org.bukkit.Material material) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < getSize(); i++) {
            ItemStack item = getItem(i);
            if (item != null && item.getType() == material) {
                slots.add(i);
            }
        }
        return slots;
    }

    default int findFirstSlot(org.bukkit.Material material) {
        List<Integer> slots = findSlots(material);
        return slots.isEmpty() ? -1 : slots.get(0);
    }

    default int findLastSlot(org.bukkit.Material material) {
        List<Integer> slots = findSlots(material);
        return slots.isEmpty() ? -1 : slots.get(slots.size() - 1);
    }

    default boolean containsItem(org.bukkit.Material material) {
        return findFirstSlot(material) != -1;
    }

    default int countItems(org.bukkit.Material material) {
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
     * Pattern matching
     */
    default boolean matchesPattern(int startSlot, org.bukkit.Material[][] pattern) {
        int startRow = slotToRow(startSlot);
        int startCol = slotToColumn(startSlot);
        
        for (int row = 0; row < pattern.length; row++) {
            for (int col = 0; col < pattern[row].length; col++) {
                int checkRow = startRow + row;
                int checkCol = startCol + col;
                
                if (!isValidCoordinate(checkRow, checkCol)) return false;
                
                ItemStack item = getItemAt(checkRow, checkCol);
                org.bukkit.Material expected = pattern[row][col];
                
                if (expected == null) {
                    if (item != null && !item.getType().isAir()) return false;
                } else {
                    if (item == null || item.getType() != expected) return false;
                }
            }
        }
        return true;
    }

    /**
     * Bulk operations
     */
    default void clearAll() {
        for (int i = 0; i < getSize(); i++) {
            removeItem(i);
        }
    }

    default void fillAll(ItemStack item) {
        for (int i = 0; i < getSize(); i++) {
            setItem(i, item);
        }
    }

    default void copyFrom(GUIBase other) {
        int maxSlots = Math.min(getSize(), other.getSize());
        for (int i = 0; i < maxSlots; i++) {
            ItemStack item = other.getItem(i);
            setItem(i, item != null ? item.clone() : null);
        }
    }

    default GUIBase createCopy() {
        // This would need to be implemented by concrete classes
        throw new UnsupportedOperationException("Copy operation not supported for this GUI type");
    }

    /**
     * Statistics and monitoring
     */
    default Map<org.bukkit.Material, Integer> getItemStatistics() {
        Map<org.bukkit.Material, Integer> stats = new HashMap<>();
        for (int i = 0; i < getSize(); i++) {
            ItemStack item = getItem(i);
            if (item != null && !item.getType().isAir()) {
                stats.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        return stats;
    }

    default void printStatistics() {
        System.out.println("=== GUI Statistics ===");
        System.out.println("Title: " + getTitle());
        System.out.println("Size: " + getSize() + " slots (" + getRows() + " rows)");
        System.out.println("Viewers: " + getViewerCount());
        System.out.println("Filled slots: " + getFilledSlotCount());
        System.out.println("Empty slots: " + getEmptySlotCount());
        
        Map<org.bukkit.Material, Integer> stats = getItemStatistics();
        if (!stats.isEmpty()) {
            System.out.println("Items:");
            stats.entrySet().stream()
                    .sorted(Map.Entry.<org.bukkit.Material, Integer>comparingByValue().reversed())
                    .forEach(entry -> System.out.println("- " + entry.getKey() + ": " + entry.getValue()));
        }
        System.out.println("===================");
    }

    /**
     * Event utilities
     */
    default void broadcastToViewers(String message) {
        for (Player viewer : getViewers()) {
            viewer.sendMessage(message);
        }
    }

    default void playSoundToViewers(org.bukkit.Sound sound) {
        playSoundToViewers(sound, 1.0f, 1.0f);
    }

    default void playSoundToViewers(org.bukkit.Sound sound, float volume, float pitch) {
        for (Player viewer : getViewers()) {
            viewer.playSound(viewer.getLocation(), sound, volume, pitch);
        }
    }

    default void sendTitleToViewers(String title, String subtitle) {
        sendTitleToViewers(title, subtitle, 10, 70, 20);
    }

    default void sendTitleToViewers(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (Player viewer : getViewers()) {
            viewer.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    default void sendActionBarToViewers(String message) {
        for (Player viewer : getViewers()) {
            viewer.sendMessage(message);
        }
    }

    /**
     * Debugging and validation
     */
    default boolean validate() {
        // Basic validation - can be overridden
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
        System.out.println("===================");
    }

    /**
     * Type checking utilities
     */
    default boolean isGUI() {
        return this instanceof GUI;
    }

    default boolean isPaginatedGUI() {
        return this instanceof PaginatedGUI;
    }

    default boolean isGUIBuilder() {
        return this instanceof GUIBuilder;
    }

    /**
     * Safe casting methods
     */
    default Optional<GUI> asGUI() {
        return this instanceof GUI ? Optional.of((GUI) this) : Optional.empty();
    }

    default Optional<PaginatedGUI> asPaginatedGUI() {
        return this instanceof PaginatedGUI ? Optional.of((PaginatedGUI) this) : Optional.empty();
    }

    default Optional<GUIBuilder> asGUIBuilder() {
        return this instanceof GUIBuilder ? Optional.of((GUIBuilder) this) : Optional.empty();
    }

    /**
     * Fluent interface helpers
     */
    default GUIBase then(java.util.function.Consumer<GUIBase> action) {
        action.accept(this);
        return this;
    }

    default <T extends GUIBase> T as(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new ClassCastException("Cannot cast " + getClass().getSimpleName() + " to " + type.getSimpleName());
    }
}
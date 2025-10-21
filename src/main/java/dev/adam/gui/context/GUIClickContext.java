package dev.adam.gui.context;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;
import org.bukkit.Material;
import org.bukkit.ChatColor;

/**
 * Context object for GUI click events that provides rich interaction capabilities.
 * 
 * This class wraps the Bukkit InventoryClickEvent and provides a comprehensive API
 * for handling GUI interactions. It offers convenient methods for player feedback,
 * inventory management, permission checking, and asynchronous task execution, making
 * it easy to create responsive and user-friendly GUI interactions.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Rich click information and type detection</li>
 *   <li>Player feedback with sounds, messages, and titles</li>
 *   <li>Permission checking with automatic error handling</li>
 *   <li>Inventory manipulation and item management</li>
 *   <li>Asynchronous task scheduling</li>
 *   <li>Event consumption to prevent further processing</li>
 *   <li>Predefined feedback methods (success, error, info, warning)</li>
 *   <li>Debug information for developers</li>
 *   <li>Inventory type detection (top vs bottom inventory)</li>
 * </ul>
 * 
 * <p>The context automatically handles:</p>
 * <ul>
 *   <li>Color code translation for messages</li>
 *   <li>Sound playback with appropriate volume and pitch</li>
 *   <li>Inventory overflow (drops items if full)</li>
 *   <li>Permission error messages with sound feedback</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * gui.setItem(10, diamondItem, ctx -> {
 *     if (!ctx.requirePermission("shop.buy.diamond")) {
 *         return; // Permission error automatically handled
 *     }
 *     
 *     if (ctx.hasItem(currencyItem)) {
 *         ctx.takeItem(currencyItem);
 *         ctx.giveItem(diamondItem);
 *         ctx.success("Diamond purchased successfully!");
 *         ctx.closeInventory();
 *     } else {
 *         ctx.error("You need more coins!");
 *     }
 * });
 * }</pre>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class GUIClickContext {
    
    /** The underlying Bukkit inventory click event */
    private final InventoryClickEvent event;
    
    /** Whether this context has been consumed (prevents further processing) */
    private boolean consumed = false;

    /**
     * Creates a new context wrapper around an inventory click event.
     * 
     * @param event the InventoryClickEvent to wrap
     */
    public GUIClickContext(InventoryClickEvent event) {
        this.event = event;
    }

    // === BASIC EVENT INFORMATION ===

    /**
     * Gets the player who performed the click.
     * 
     * @return the clicking player
     */
    public Player getPlayer() {
        return (Player) event.getWhoClicked();
    }

    /**
     * Gets the slot that was clicked (relative to the clicked inventory).
     * 
     * @return the clicked slot number
     */
    public int getSlot() {
        return event.getSlot();
    }

    /**
     * Gets the raw slot number (absolute position in the combined inventory view).
     * 
     * @return the raw slot number
     */
    public int getRawSlot() {
        return event.getRawSlot();
    }

    /**
     * Gets the item that was clicked on.
     * 
     * @return the clicked ItemStack, or null if slot was empty
     */
    public ItemStack getClickedItem() {
        return event.getCurrentItem();
    }

    /**
     * Gets the item currently on the player's cursor.
     * 
     * @return the cursor ItemStack, or null if cursor is empty
     */
    public ItemStack getCursor() {
        return event.getCursor();
    }

    /**
     * Gets the type of click that was performed.
     * 
     * @return the ClickType enum value
     */
    public ClickType getClickType() {
        return event.getClick();
    }

    /**
     * Gets the inventory action that would be performed.
     * 
     * @return the InventoryAction enum value
     */
    public InventoryAction getAction() {
        return event.getAction();
    }

    /**
     * Gets the underlying Bukkit event.
     * Use this when you need access to advanced event features.
     * 
     * @return the original InventoryClickEvent
     */
    public InventoryClickEvent getEvent() {
        return event;
    }

    // === CLICK TYPE DETECTION ===

    /**
     * Checks if this was a left mouse button click.
     * 
     * @return true if left click, false otherwise
     */
    public boolean isLeftClick() {
        return event.isLeftClick();
    }

    /**
     * Checks if this was a right mouse button click.
     * 
     * @return true if right click, false otherwise
     */
    public boolean isRightClick() {
        return event.isRightClick();
    }

    /**
     * Checks if shift was held during the click.
     * 
     * @return true if shift click, false otherwise
     */
    public boolean isShiftClick() {
        return event.isShiftClick();
    }

    /**
     * Checks if this was a middle mouse button click.
     * 
     * @return true if middle click, false otherwise
     */
    public boolean isMiddleClick() {
        return event.getClick() == ClickType.MIDDLE;
    }

    /**
     * Checks if this was a double-click.
     * 
     * @return true if double click, false otherwise
     */
    public boolean isDoubleClick() {
        return event.getClick() == ClickType.DOUBLE_CLICK;
    }

    /**
     * Checks if a number key (1-9) was pressed during the click.
     * 
     * @return true if number key click, false otherwise
     */
    public boolean isNumberKey() {
        return event.getClick().name().contains("NUMBER_KEY");
    }

    /**
     * Checks if the drop key (Q) was pressed.
     * 
     * @return true if drop key, false otherwise
     */
    public boolean isDropKey() {
        return event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP;
    }

    /**
     * Checks if this was a shift + left click.
     * 
     * @return true if shift left click, false otherwise
     */
    public boolean isShiftLeftClick() {
        return event.getClick() == ClickType.SHIFT_LEFT;
    }

    /**
     * Checks if this was a shift + right click.
     * 
     * @return true if shift right click, false otherwise
     */
    public boolean isShiftRightClick() {
        return event.getClick() == ClickType.SHIFT_RIGHT;
    }

    /**
     * Checks if control (Ctrl) was held during the click.
     * 
     * @return true if control click, false otherwise
     */
    public boolean isControlClick() {
        return event.getClick().name().contains("CONTROL");
    }

    // === INVENTORY MANAGEMENT ===

    /**
     * Gets the inventory that contains the clicked slot.
     * 
     * @return the main inventory (usually the GUI inventory)
     */
    public Inventory getInventory() {
        return event.getInventory();
    }

    /**
     * Gets the specific inventory that was clicked.
     * This can be either the top (GUI) or bottom (player) inventory.
     * 
     * @return the clicked inventory, or null if none
     */
    public Inventory getClickedInventory() {
        return event.getClickedInventory();
    }

    /**
     * Checks if the click was in the top inventory (GUI inventory).
     * 
     * @return true if top inventory was clicked, false otherwise
     */
    public boolean isTopInventory() {
        return event.getClickedInventory() != null &&
                event.getClickedInventory().equals(event.getInventory());
    }

    /**
     * Checks if the click was in the bottom inventory (player inventory).
     * 
     * @return true if player inventory was clicked, false otherwise
     */
    public boolean isBottomInventory() {
        return event.getClickedInventory() != null &&
                event.getClickedInventory().equals(getPlayer().getInventory());
    }

    /**
     * Sets the item on the player's cursor.
     * 
     * @param item the ItemStack to place on cursor (null to clear)
     */
    public void setCursor(ItemStack item) {
        event.setCursor(item);
    }

    /**
     * Sets the item in the clicked slot.
     * 
     * @param item the ItemStack to place in the slot (null to clear)
     */
    public void setCurrentItem(ItemStack item) {
        event.setCurrentItem(item);
    }

    /**
     * Cancels the click event to prevent default inventory behavior.
     * This is automatically called when the context is consumed.
     */
    public void cancel() {
        event.setCancelled(true);
    }

    /**
     * Closes the player's currently open inventory.
     */
    public void closeInventory() {
        getPlayer().closeInventory();
    }

    // === ITEM MANAGEMENT ===

    /**
     * Gives an item to the player.
     * If the inventory is full, the item is dropped at the player's location.
     * 
     * @param item the ItemStack to give to the player
     */
    public void giveItem(ItemStack item) {
        if (item == null) return;
        
        if (getPlayer().getInventory().firstEmpty() != -1) {
            getPlayer().getInventory().addItem(item);
        } else {
            getPlayer().getWorld().dropItemNaturally(getPlayer().getLocation(), item);
            info("Item dropped at your feet because inventory is full!");
        }
    }

    /**
     * Removes an item from the player's inventory.
     * 
     * @param item the ItemStack to remove (matches type and amount)
     */
    public void takeItem(ItemStack item) {
        if (item != null) {
            getPlayer().getInventory().removeItem(item);
        }
    }

    /**
     * Checks if the player has at least the specified amount of an item.
     * 
     * @param item the ItemStack to check for (matches type and amount)
     * @return true if player has enough of the item, false otherwise
     */
    public boolean hasItem(ItemStack item) {
        return item != null && getPlayer().getInventory().containsAtLeast(item, item.getAmount());
    }

    /**
     * Counts the total amount of a specific material in the player's inventory.
     * 
     * @param material the Material to count
     * @return the total amount of the material
     */
    public int getItemAmount(Material material) {
        int total = 0;
        for (ItemStack item : getPlayer().getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    // === PLAYER COMMUNICATION ===

    /**
     * Sends a plain message to the player.
     * 
     * @param message the message to send
     */
    public void sendMessage(String message) {
        if (message != null) {
            getPlayer().sendMessage(message);
        }
    }

    /**
     * Sends a message with color code support to the player.
     * Color codes use the 'and' character (e.g., "Green text").
     * 
     * @param message the message to send (supports color codes)
     */
    public void sendColoredMessage(String message) {
        if (message != null) {
            getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    /**
     * Sends an action bar message to the player.
     * Note: This is a simplified implementation. For proper action bars,
     * consider using a packet library or newer Bukkit methods.
     * 
     * @param message the action bar message (supports color codes)
     */
    public void sendActionBar(String message) {
        if (message != null) {
            // This is a placeholder - actual action bar implementation would use packets
            sendColoredMessage(message);
        }
    }

    /**
     * Sends a title to the player with default timing.
     * 
     * @param title the main title text (supports color codes)
     * @param subtitle the subtitle text (supports color codes)
     */
    public void sendTitle(String title, String subtitle) {
        sendTitle(title, subtitle, 10, 70, 20);
    }

    /**
     * Sends a title to the player with custom timing.
     * 
     * @param title the main title text (supports color codes)
     * @param subtitle the subtitle text (supports color codes)
     * @param fadeIn fade-in time in ticks
     * @param stay display time in ticks
     * @param fadeOut fade-out time in ticks
     */
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        String coloredTitle = title != null ? ChatColor.translateAlternateColorCodes('&', title) : "";
        String coloredSubtitle = subtitle != null ? ChatColor.translateAlternateColorCodes('&', subtitle) : "";
        
        getPlayer().sendTitle(coloredTitle, coloredSubtitle, fadeIn, stay, fadeOut);
    }

    // === SOUND EFFECTS ===

    /**
     * Plays a sound to the player with default volume and pitch.
     * 
     * @param sound the Sound to play
     */
    public void playSound(Sound sound) {
        playSound(sound, 1.0f, 1.0f);
    }

    /**
     * Plays a sound to the player with custom volume and pitch.
     * 
     * @param sound the Sound to play
     * @param volume the volume level (0.0-1.0)
     * @param pitch the pitch level (0.5-2.0)
     */
    public void playSound(Sound sound, float volume, float pitch) {
        if (sound != null) {
            getPlayer().playSound(getPlayer().getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Plays a standard UI click sound.
     */
    public void playClickSound() {
        playSound(Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * Plays an error sound to indicate something went wrong.
     */
    public void playErrorSound() {
        playSound(Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }

    /**
     * Plays a success sound to indicate a positive outcome.
     */
    public void playSuccessSound() {
        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }

    /**
     * Plays an information sound for neutral notifications.
     */
    public void playInfoSound() {
        playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
    }

    /**
     * Plays a warning sound to indicate caution.
     */
    public void playWarningSound() {
        playSound(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
    }

    // === FEEDBACK METHODS ===

    /**
     * Displays a success message with appropriate styling and sound.
     * 
     * @param message the success message to display
     */
    public void success(String message) {
        if (message != null) {
            sendColoredMessage("&a✓ " + message);
            playSuccessSound();
        }
    }

    /**
     * Displays an error message with appropriate styling and sound.
     * 
     * @param message the error message to display
     */
    public void error(String message) {
        if (message != null) {
            sendColoredMessage("&c✗ " + message);
            playErrorSound();
        }
    }

    /**
     * Displays an informational message with appropriate styling and sound.
     * 
     * @param message the info message to display
     */
    public void info(String message) {
        if (message != null) {
            sendColoredMessage("&e⚠ " + message);
            playInfoSound();
        }
    }

    /**
     * Displays a warning message with appropriate styling and sound.
     * 
     * @param message the warning message to display
     */
    public void warning(String message) {
        if (message != null) {
            sendColoredMessage("&6⚠ " + message);
            playWarningSound();
        }
    }

    // === PERMISSION MANAGEMENT ===

    /**
     * Checks if the player has the specified permission.
     * 
     * @param permission the permission node to check
     * @return true if player has permission, false otherwise
     */
    public boolean hasPermission(String permission) {
        return permission == null || getPlayer().hasPermission(permission);
    }

    /**
     * Requires the player to have a specific permission.
     * If they don't have it, displays a default error message and plays error sound.
     * 
     * @param permission the required permission node
     * @return true if player has permission, false if they don't
     */
    public boolean requirePermission(String permission) {
        return requirePermission(permission, "You don't have permission to do this!");
    }

    /**
     * Requires the player to have a specific permission with a custom error message.
     * If they don't have it, displays the error message and plays error sound.
     * 
     * @param permission the required permission node
     * @param errorMessage the message to show if permission is missing
     * @return true if player has permission, false if they don't
     */
    public boolean requirePermission(String permission, String errorMessage) {
        if (!hasPermission(permission)) {
            error(errorMessage);
            return false;
        }
        return true;
    }

    // === ASYNCHRONOUS EXECUTION ===

    /**
     * Executes a task after a delay on the main server thread.
     * 
     * @param plugin the plugin instance to schedule with
     * @param task the task to execute
     * @param delayTicks the delay in ticks before execution
     */
    public void runLater(Plugin plugin, Runnable task, long delayTicks) {
        if (plugin != null && task != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        task.run();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error in delayed task: " + e.getMessage());
                    }
                }
            }.runTaskLater(plugin, Math.max(0, delayTicks));
        }
    }

    /**
     * Executes a task asynchronously (off the main thread).
     * Warning: Bukkit API calls are not thread-safe in async tasks!
     * 
     * @param plugin the plugin instance to schedule with
     * @param task the task to execute
     */
    public void runAsync(Plugin plugin, Runnable task) {
        if (plugin != null && task != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        task.run();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error in async task: " + e.getMessage());
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    // === CONTEXT CONTROL ===

    /**
     * Consumes this context, preventing further processing of the click event.
     * This automatically cancels the event and sets the consumed flag.
     */
    public void consume() {
        this.consumed = true;
        cancel();
    }

    /**
     * Checks if this context has been consumed.
     * 
     * @return true if consumed, false otherwise
     */
    public boolean isConsumed() {
        return consumed;
    }

    // === DEBUG AND UTILITY ===

    /**
     * Sends debug information about the click to operators.
     * Only visible to players with OP status.
     */
    public void debug() {
        if (getPlayer().isOp()) {
            sendMessage("§7[DEBUG] Slot: " + getSlot() +
                    ", RawSlot: " + getRawSlot() +
                    ", Click: " + getClickType() +
                    ", Action: " + getAction() +
                    ", Item: " + (getClickedItem() != null ? getClickedItem().getType() : "null") +
                    ", Cursor: " + (getCursor() != null ? getCursor().getType() : "null") +
                    ", TopInv: " + isTopInventory() +
                    ", Consumed: " + consumed);
        }
    }

    /**
     * Creates a debug string representation of this context.
     * Useful for logging and troubleshooting.
     * 
     * @return a debug string containing key context information
     */
    @Override
    public String toString() {
        return "GUIClickContext{" +
                "player=" + getPlayer().getName() +
                ", slot=" + getSlot() +
                ", rawSlot=" + getRawSlot() +
                ", clickType=" + getClickType() +
                ", action=" + getAction() +
                ", item=" + (getClickedItem() != null ? getClickedItem().getType() : "null") +
                ", cursor=" + (getCursor() != null ? getCursor().getType() : "null") +
                ", topInventory=" + isTopInventory() +
                ", consumed=" + consumed +
                '}';
    }

    // === CONVENIENCE METHODS ===

    /**
     * Checks if the clicked item is of the specified material.
     * 
     * @param material the Material to check against
     * @return true if clicked item matches the material, false otherwise
     */
    public boolean isClickedItem(Material material) {
        return getClickedItem() != null && getClickedItem().getType() == material;
    }

    /**
     * Checks if the cursor item is of the specified material.
     * 
     * @param material the Material to check against
     * @return true if cursor item matches the material, false otherwise
     */
    public boolean isCursorItem(Material material) {
        return getCursor() != null && getCursor().getType() == material;
    }

    /**
     * Gets the amount of the clicked item.
     * 
     * @return the item amount, or 0 if no item was clicked
     */
    public int getClickedItemAmount() {
        return getClickedItem() != null ? getClickedItem().getAmount() : 0;
    }

    /**
     * Gets the amount of the cursor item.
     * 
     * @return the cursor item amount, or 0 if cursor is empty
     */
    public int getCursorItemAmount() {
        return getCursor() != null ? getCursor().getAmount() : 0;
    }

    /**
     * Checks if the clicked slot is empty.
     * 
     * @return true if no item in the clicked slot, false otherwise
     */
    public boolean isSlotEmpty() {
        ItemStack item = getClickedItem();
        return item == null || item.getType().isAir();
    }

    /**
     * Checks if the cursor is empty.
     * 
     * @return true if cursor has no item, false otherwise
     */
    public boolean isCursorEmpty() {
        ItemStack cursor = getCursor();
        return cursor == null || cursor.getType().isAir();
    }

    /**
     * Performs a quick inventory refresh for the player.
     * Useful after modifying inventory contents.
     */
    public void refreshInventory() {
        getPlayer().updateInventory();
    }
}
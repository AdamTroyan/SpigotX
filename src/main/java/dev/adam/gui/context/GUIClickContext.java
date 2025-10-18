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

public class GUIClickContext {
    private final InventoryClickEvent event;
    private boolean consumed = false;

    public GUIClickContext(InventoryClickEvent event) {
        this.event = event;
    }

    public Player getPlayer() {
        return (Player) event.getWhoClicked();
    }

    public int getSlot() {
        return event.getSlot();
    }

    public ItemStack getClickedItem() {
        return event.getCurrentItem();
    }

    public void cancel() {
        event.setCancelled(true);
    }

    public void sendMessage(String msg) {
        getPlayer().sendMessage(msg);
    }

    public int getRawSlot() {
        return event.getRawSlot();
    }

    public ItemStack getCursor() {
        return event.getCursor();
    }

    public boolean isLeftClick() {
        return event.isLeftClick();
    }

    public boolean isRightClick() {
        return event.isRightClick();
    }

    public boolean isShiftClick() {
        return event.isShiftClick();
    }

    public void closeInventory() {
        getPlayer().closeInventory();
    }

    public InventoryClickEvent getEvent() {
        return event;
    }

    public ClickType getClickType() {
        return event.getClick();
    }
    
    public InventoryAction getAction() {
        return event.getAction();
    }
    
    public boolean isMiddleClick() {
        return event.getClick() == ClickType.MIDDLE;
    }
    
    public boolean isDoubleClick() {
        return event.getClick() == ClickType.DOUBLE_CLICK;
    }
    
    public boolean isNumberKey() {
        return event.getClick().name().contains("NUMBER_KEY");
    }
    
    public boolean isDropKey() {
        return event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP;
    }
    
    public boolean isShiftLeftClick() {
        return event.getClick() == ClickType.SHIFT_LEFT;
    }
    
    public boolean isShiftRightClick() {
        return event.getClick() == ClickType.SHIFT_RIGHT;
    }
    
    public boolean isControlClick() {
        return event.getClick().name().contains("CONTROL");
    }

    public void playSound(Sound sound) {
        playSound(sound, 1.0f, 1.0f);
    }
    
    public void playSound(Sound sound, float volume, float pitch) {
        getPlayer().playSound(getPlayer().getLocation(), sound, volume, pitch);
    }
    
    public void playClickSound() {
        playSound(Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
    
    public void playErrorSound() {
        playSound(Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }
    
    public void playSuccessSound() {
        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }

    public void sendColoredMessage(String msg) {
        getPlayer().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
    }
    
    public void sendActionBar(String msg) {
        getPlayer().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', msg));
    }
    
    public void sendTitle(String title, String subtitle) {
        sendTitle(title, subtitle, 10, 70, 20);
    }
    
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        getPlayer().sendTitle(
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title),
            org.bukkit.ChatColor.translateAlternateColorCodes('&', subtitle),
            fadeIn, stay, fadeOut
        );
    }

    public Inventory getInventory() {
        return event.getInventory();
    }
    
    public Inventory getClickedInventory() {
        return event.getClickedInventory();
    }
    
    public boolean isTopInventory() {
        return event.getClickedInventory() != null && 
               event.getClickedInventory().equals(event.getInventory());
    }
    
    public boolean isBottomInventory() {
        return event.getClickedInventory() != null && 
               event.getClickedInventory().equals(getPlayer().getInventory());
    }
    
    public void setCursor(ItemStack item) {
        event.setCursor(item);
    }
    
    public void setCurrentItem(ItemStack item) {
        event.setCurrentItem(item);
    }

    public void runLater(Plugin plugin, Runnable task, long delayTicks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskLater(plugin, delayTicks);
    }
    
    public void runAsync(Plugin plugin, Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskAsynchronously(plugin);
    }

    public boolean hasPermission(String permission) {
        return getPlayer().hasPermission(permission);
    }
    
    public boolean requirePermission(String permission) {
        return requirePermission(permission, "&cYou don't have permission to do this!");
    }
    
    public boolean requirePermission(String permission, String errorMessage) {
        if (!hasPermission(permission)) {
            sendColoredMessage(errorMessage);
            playErrorSound();
            return false;
        }
        return true;
    }

    public void consume() {
        this.consumed = true;
        cancel();
    }
    
    public boolean isConsumed() {
        return consumed;
    }

    public void giveItem(ItemStack item) {
        if (getPlayer().getInventory().firstEmpty() != -1) {
            getPlayer().getInventory().addItem(item);
        } else {
            getPlayer().getWorld().dropItemNaturally(getPlayer().getLocation(), item);
            sendColoredMessage("&eItem dropped at your feet because inventory is full!");
        }
    }
    
    public void takeItem(ItemStack item) {
        getPlayer().getInventory().removeItem(item);
    }
    
    public boolean hasItem(ItemStack item) {
        return getPlayer().getInventory().containsAtLeast(item, item.getAmount());
    }
    
    public int getItemAmount(org.bukkit.Material material) {
        int total = 0;
        for (ItemStack item : getPlayer().getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    public void success(String message) {
        sendColoredMessage("&a✓ " + message);
        playSuccessSound();
    }
    
    public void error(String message) {
        sendColoredMessage("&c✗ " + message);
        playErrorSound();
    }
    
    public void info(String message) {
        sendColoredMessage("&e⚠ " + message);
        playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
    }
    
    public void warning(String message) {
        sendColoredMessage("&6⚠ " + message);
        playSound(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.8f);
    }

    public void debug() {
        if (getPlayer().isOp()) {
            sendMessage("§7[DEBUG] Slot: " + getSlot() + 
                       ", Click: " + getClickType() + 
                       ", Action: " + getAction() + 
                       ", Item: " + (getClickedItem() != null ? getClickedItem().getType() : "null"));
        }
    }
    
    @Override
    public String toString() {
        return "GUIClickContext{" +
                "player=" + getPlayer().getName() +
                ", slot=" + getSlot() +
                ", clickType=" + getClickType() +
                ", action=" + getAction() +
                ", item=" + (getClickedItem() != null ? getClickedItem().getType() : "null") +
                ", consumed=" + consumed +
                '}';
    }
}
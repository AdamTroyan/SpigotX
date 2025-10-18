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

public class GUI implements GUIBase {
    private final String title;
    private final int rows;
    private final Inventory inventory;
    private final Map<Integer, Consumer<GUIClickContext>> clickHandlers = new HashMap<>();
    private final Map<Integer, String> slotPermissions = new HashMap<>();
    private final Map<Integer, ItemStack> originalItems = new HashMap<>();
    private final Set<Integer> protectedSlots = new HashSet<>();
    private final Map<String, Object> properties = new ConcurrentHashMap<>();
    
    private Consumer<Player> onOpen, onClose;
    private Consumer<GUIClickContext> globalClickHandler;
    private Predicate<Player> openCondition;
    
    private boolean autoRefresh = false;
    private long refreshInterval = 20L; // ticks
    private boolean allowPlayerInventoryClick = false;
    private String closeSound = "UI_BUTTON_CLICK";
    private ItemStack backgroundItem;
    private final Map<Integer, Long> slotCooldowns = new HashMap<>();
    private final Map<Integer, String> slotTooltips = new HashMap<>();
    
    private final Map<Integer, ItemStack[]> animations = new HashMap<>();
    private final Map<Integer, Integer> animationFrames = new HashMap<>();
    private final Map<Integer, Long> animationSpeeds = new HashMap<>();

    public GUI(String title, int rows) {
        this.title = title;
        this.rows = rows;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        
        this.backgroundItem = createDefaultBackground();
    }

    public void setItem(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        
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

    public void removeItem(int slot) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        inventory.setItem(slot, backgroundItem);
        clickHandlers.remove(slot);
        slotPermissions.remove(slot);
        originalItems.remove(slot);
        protectedSlots.remove(slot);
        slotCooldowns.remove(slot);
        slotTooltips.remove(slot);
    }

    public void setItem(int slot, ItemStack item, String permission, long cooldownMs, Consumer<GUIClickContext> onClick) {
        setItem(slot, item, onClick);
        if (permission != null && !permission.isEmpty()) {
            slotPermissions.put(slot, permission);
        }
        if (cooldownMs > 0) {
            slotCooldowns.put(slot, cooldownMs);
        }
    }

    public void setItemWithTooltip(int slot, ItemStack item, String tooltip, Consumer<GUIClickContext> onClick) {
        setItem(slot, item, onClick);
        if (tooltip != null) {
            slotTooltips.put(slot, tooltip);
        }
    }

    public void setProtectedItem(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        setItem(slot, item, onClick);
        protectedSlots.add(slot);
    }

    public void protectSlot(int slot) {
        protectedSlots.add(slot);
    }

    public void unprotectSlot(int slot) {
        protectedSlots.remove(slot);
    }

    public boolean isProtected(int slot) {
        return protectedSlots.contains(slot);
    }

    public void setAnimatedItem(int slot, ItemStack[] frames, long speedTicks, Consumer<GUIClickContext> onClick) {
        if (frames == null || frames.length == 0) return;
        
        animations.put(slot, frames);
        animationFrames.put(slot, 0);
        animationSpeeds.put(slot, speedTicks);
        setItem(slot, frames[0], onClick);
    }

    public void updateAnimations() {
        for (Map.Entry<Integer, ItemStack[]> entry : animations.entrySet()) {
            int slot = entry.getKey();
            ItemStack[] frames = entry.getValue();
            
            int currentFrame = animationFrames.getOrDefault(slot, 0);
            int nextFrame = (currentFrame + 1) % frames.length;
            
            animationFrames.put(slot, nextFrame);
            inventory.setItem(slot, frames[nextFrame]);
        }
    }

    public void stopAnimation(int slot) {
        animations.remove(slot);
        animationFrames.remove(slot);
        animationSpeeds.remove(slot);
    }

    public void setItems(Map<Integer, ItemStack> items, Consumer<GUIClickContext> onClick) {
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            setItem(entry.getKey(), entry.getValue(), onClick);
        }
    }

    public void setItemsWithHandlers(Map<Integer, ItemStack> items, Map<Integer, Consumer<GUIClickContext>> handlers) {
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            int slot = entry.getKey();
            setItem(slot, entry.getValue(), handlers.get(slot));
        }
    }

    public void setPattern(String[] pattern, Map<Character, ItemStack> mapping, Map<Character, Consumer<GUIClickContext>> handlers) {
        int slot = 0;
        for (String row : pattern) {
            for (char c : row.toCharArray()) {
                if (slot >= inventory.getSize()) break;
                
                ItemStack item = mapping.get(c);
                Consumer<GUIClickContext> handler = handlers != null ? handlers.get(c) : null;
                
                if (item != null) {
                    setItem(slot, item, handler);
                } else if (c == ' ') {
                    setItem(slot, backgroundItem, null);
                }
                slot++;
            }
        }
    }

    public void fillArea(int startSlot, int endSlot, ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int i = startSlot; i <= endSlot && i < inventory.getSize(); i++) {
            setItem(i, item, onClick);
        }
    }

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

    public void setItemIf(int slot, ItemStack item, Predicate<GUI> condition, Consumer<GUIClickContext> onClick) {
        if (condition.test(this)) {
            setItem(slot, item, onClick);
        }
    }

    public void fillRowIf(int row, ItemStack item, Predicate<Integer> slotCondition, Consumer<GUIClickContext> onClick) {
        int start = (row - 1) * 9;
        int end = start + 9;
        for (int i = start; i < end && i < inventory.getSize(); i++) {
            if (slotCondition.test(i)) {
                setItem(i, item, onClick);
            }
        }
    }

    public void swapItems(int slot1, int slot2) {
        ItemStack item1 = inventory.getItem(slot1);
        ItemStack item2 = inventory.getItem(slot2);
        Consumer<GUIClickContext> handler1 = clickHandlers.get(slot1);
        Consumer<GUIClickContext> handler2 = clickHandlers.get(slot2);
        
        setItem(slot1, item2, handler2);
        setItem(slot2, item1, handler1);
    }

    public void moveItem(int fromSlot, int toSlot) {
        ItemStack item = inventory.getItem(fromSlot);
        Consumer<GUIClickContext> handler = clickHandlers.get(fromSlot);
        
        removeItem(fromSlot);
        setItem(toSlot, item, handler);
    }

    public ItemStack cloneItem(int slot) {
        ItemStack item = inventory.getItem(slot);
        return item != null ? item.clone() : null;
    }

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

    public int getFirstSlotWithItem(Material material) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material) {
                return i;
            }
        }
        return -1;
    }

    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public <T> T getProperty(String key, T defaultValue, Class<T> type) {
        T value = getProperty(key, type);
        return value != null ? value : defaultValue;
    }

    public void removeProperty(String key) {
        properties.remove(key);
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public void resetSlot(int slot) {
        ItemStack original = originalItems.get(slot);
        if (original != null) {
            setItem(slot, original.clone(), clickHandlers.get(slot));
        }
    }

    public void resetAllSlots() {
        for (int slot : originalItems.keySet()) {
            resetSlot(slot);
        }
    }

    public void clearAllSlots() {
        for (int i = 0; i < inventory.getSize(); i++) {
            removeItem(i);
        }
    }

    public void setOpenCondition(Predicate<Player> condition) {
        this.openCondition = condition;
    }

    public void setGlobalClickHandler(Consumer<GUIClickContext> handler) {
        this.globalClickHandler = handler;
    }

    public void setAutoRefresh(boolean autoRefresh, long intervalTicks) {
        this.autoRefresh = autoRefresh;
        this.refreshInterval = intervalTicks;
    }

    public void setAllowPlayerInventoryClick(boolean allow) {
        this.allowPlayerInventoryClick = allow;
    }

    public void setCloseSound(String sound) {
        this.closeSound = sound;
    }

    public void setBackgroundItem(ItemStack item) {
        this.backgroundItem = item;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, item);
            }
        }
    }

    public int getSlotCount() {
        return rows * 9;
    }

    public int getFilledSlotCount() {
        int count = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) != null && !inventory.getItem(i).getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    public int getEmptySlotCount() {
        return getSlotCount() - getFilledSlotCount();
    }

    public boolean isFull() {
        return getEmptySlotCount() == 0;
    }

    public boolean isEmpty() {
        return getFilledSlotCount() == 0;
    }

    private ItemStack createDefaultBackground() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

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

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        GUIClickContext context = new GUIClickContext(event);
        
        String permission = slotPermissions.get(slot);
        if (permission != null && !player.hasPermission(permission)) {
            context.error("You don't have permission to use this!");
            return;
        }
        
        Long cooldown = slotCooldowns.get(slot);
        if (cooldown != null) {
            
        }
        
        String tooltip = slotTooltips.get(slot);
        if (tooltip != null) {
            context.sendActionBar(tooltip);
        }
        
        if (globalClickHandler != null) {
            try {
                globalClickHandler.accept(context);
                if (context.isConsumed()) return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        Consumer<GUIClickContext> handler = clickHandlers.get(slot);
        if (handler != null) {
            event.setCancelled(true);
            try {
                handler.accept(context);
            } catch (Exception e) {
                context.error("An error occurred while processing your click!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (onClose != null && event.getInventory().equals(inventory)) {
            Player player = (Player) event.getPlayer();
            try {
                onClose.accept(player);
                
                if (closeSound != null) {
                    try {
                        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(closeSound);
                        player.playSound(player.getLocation(), sound, 0.5f, 1.0f);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void open(Player player) {
        if (player == null) return;
        
        if (openCondition != null && !openCondition.test(player)) {
            player.sendMessage("§cYou cannot open this GUI right now!");
            return;
        }
        
        player.openInventory(inventory);
        if (onOpen != null) {
            try {
                onOpen.accept(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public String getTitle() { return title; }
    public int getRows() { return rows; }
    public Consumer<Player> getOnOpen() { return onOpen; }
    public Consumer<Player> getOnClose() { return onClose; }
    public boolean isAutoRefresh() { return autoRefresh; }
    public long getRefreshInterval() { return refreshInterval; }
    public boolean isAllowPlayerInventoryClick() { return allowPlayerInventoryClick; }
    public String getCloseSound() { return closeSound; }
    public ItemStack getBackgroundItem() { return backgroundItem; }
    
    public void setOnOpen(Consumer<Player> onOpen) { this.onOpen = onOpen; }
    public void setOnClose(Consumer<Player> onClose) { this.onClose = onClose; }

    @Override
    public Inventory getInventory() { return inventory; }

    @Override
    public boolean hasHandler(int slot) {
        return clickHandlers.containsKey(slot);
    }

    public Consumer<GUIClickContext> getHandler(int slot) {
        return clickHandlers.get(slot);
    }

    public void removeHandler(int slot) {
        clickHandlers.remove(slot);
    }
        
    public void fillBorder(ItemStack item, Consumer<GUIClickContext> onClick) {
        int size = rows * 9;
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                setItem(i, item, onClick);
            }
        }
    }

    public void fillRowIfEmpty(int row, ItemStack item, Consumer<GUIClickContext> onClick) {
        int start = (row - 1) * 9;
        int end = start + 9;
        for (int i = start; i < end && i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) setItem(i, item, onClick);
        }
    }

    public void fillColumnIfEmpty(int col, ItemStack item, Consumer<GUIClickContext> onClick) {
        if (col < 0 || col >= 9) return;
        for (int i = col; i < inventory.getSize(); i += 9) {
            if (inventory.getItem(i) == null) setItem(i, item, onClick);
        }
    }

    public void clearRow(int row) {
        int start = (row - 1) * 9;
        for (int i = start; i < start + 9 && i < inventory.getSize(); i++) {
            removeItem(i);
        }
    }

    public void clearColumn(int col) {
        if (col < 0 || col >= 9) return;
        for (int i = col; i < inventory.getSize(); i += 9) removeItem(i);
    }

    public void setItemsBulk(int[] slots, ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                setItem(slot, item, onClick);
            }
        }
    }

    public void fillBorderIfEmpty(ItemStack item, Consumer<GUIClickContext> onClick) {
        int size = inventory.getSize();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) {
                if (inventory.getItem(i) == null) setItem(i, item, onClick);
            }
        }
    }

    public void replaceItem(Material from, ItemStack to, Consumer<GUIClickContext> onClick) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack current = inventory.getItem(i);
            if (current != null && current.getType() == from) setItem(i, to, onClick);
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
            setItem(slot, item, onClick);
        }
    }

    public void setRow(int row, ItemStack[] items, Consumer<GUIClickContext>[] handlers) {
        int start = (row - 1) * 9;
        for (int i = 0; i < 9 && i < items.length && (start + i) < inventory.getSize(); i++) {
            setItem(start + i, items[i], handlers != null && i < handlers.length ? handlers[i] : null);
        }
    }

    public void setBackground(ItemStack item, Consumer<GUIClickContext> onClick) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) setItem(i, item, onClick);
        }
    }

    public void close(Player player) {
        if (player == null) return;
        player.closeInventory();
    }
}
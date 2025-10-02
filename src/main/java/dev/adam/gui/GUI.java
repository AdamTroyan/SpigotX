package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GUI implements InventoryHolder {

    private final Plugin plugin;
    private final Inventory inventory;
    private final Map<Integer, Consumer<ClickContext>> slotHandlers = new ConcurrentHashMap<>();
    private final Map<Integer, GUIButton> legacyButtons = new ConcurrentHashMap<>();
    private final Map<Integer, Animation> animations = new ConcurrentHashMap<>();

    private Consumer<ClickContext> globalClickHandler;
    private Consumer<Player> onOpen;
    private Consumer<Player> onClose;

    private boolean defaultCancel = true;
    private boolean allowShiftClick = false;
    private boolean allowDrag = false;
    private boolean autoUnregisterWhenEmpty = true;

    public GUI(Plugin plugin, String title, int rows) {
        this(plugin, title, rows * 9, null);
    }

    public GUI(Plugin plugin, String title, int size, InventoryHolder holder) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(holder, size, title);
        GUIManager.get(plugin).register(this);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    // ---------------- API ----------------

    public GUI setItem(int slot, ItemStack item, Consumer<ClickContext> action) {
        if (item != null) item = item.clone();
        inventory.setItem(slot, item);

        if (action != null) slotHandlers.put(slot, action);
        else slotHandlers.remove(slot);

        legacyButtons.remove(slot);

        return this;
    }

    public GUI setItemForPlayer(int slot, ItemStack item, Consumer<Player> action) {
        if (item != null) item = item.clone();
        inventory.setItem(slot, item);

        if (action != null) {
            slotHandlers.put(slot, ctx -> {
                if (ctx.getPlayer() != null) action.accept(ctx.getPlayer());
            });
        } else {
            slotHandlers.remove(slot);
        }
        legacyButtons.remove(slot);

        return this;
    }

    public boolean addItem(ItemStack item, Consumer<ClickContext> action) {
        int empty = inventory.firstEmpty();
        if (empty == -1) return false;
        setItem(empty, item, action);
        return true;
    }

    public void removeItem(int slot) {
        inventory.clear(slot);
        slotHandlers.remove(slot);
        legacyButtons.remove(slot);
        animations.remove(slot);
    }

    public void clear() {
        inventory.clear();
        slotHandlers.clear();
        legacyButtons.clear();
        animations.clear();
    }

    public void open(Player player) {
        applyPlaceholdersToPlayer(player);
        player.openInventory(inventory);
    }

    public void openAll(Collection<? extends Player> players) {
        for (Player p : players) open(p);
    }

    public void closeAll() {
        for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) viewer.closeInventory();
    }

    public void fillRow(int row, ItemStack item, java.util.function.Consumer<ClickContext> action) {
        int cols = 9;
        int start = row * cols;
        for (int i = 0; i < cols; i++) setItem(start + i, item, action);
    }

    public void fillBorder(ItemStack item, java.util.function.Consumer<ClickContext> action) {
        int size = inventory.getSize();
        int rows = size / 9;
        if (rows <= 0) return;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) {
                int slot = r * 9 + c;
                if (r == 0 || r == rows - 1 || c == 0 || c == 8) setItem(slot, item, action);
            }
        }
    }

    public void setAnimation(int slot, Animation animation) {
        animations.put(slot, animation);

        GUIUpdater.scheduleRepeating(plugin, this, animation.periodTicks(), g -> {
            Animation an = animations.get(slot);
            if (an != null) {
                an.advance();
                ItemStack frame = an.current();
                inventory.setItem(slot, frame);
            }
        });
    }

    public void removeAnimation(int slot) {
        animations.remove(slot);
    }

    // config setters
    public void setDefaultCancel(boolean defaultCancel) { this.defaultCancel = defaultCancel; }
    public void setAllowShiftClick(boolean allow) { this.allowShiftClick = allow; }
    public void setAllowDrag(boolean allow) { this.allowDrag = allow; }
    public void setAutoUnregisterWhenEmpty(boolean autoUnregisterWhenEmpty) { this.autoUnregisterWhenEmpty = autoUnregisterWhenEmpty; }

    // handlers
    public void setGlobalClickHandler(Consumer<ClickContext> handler) { this.globalClickHandler = handler; }
    public void setOnOpen(Consumer<Player> onOpen) { this.onOpen = onOpen; }
    public void setOnClose(Consumer<Player> onClose) { this.onClose = onClose; }

    // placeholders
    private void applyPlaceholdersToPlayer(Player p) {
        PlaceholderManager pm = PlaceholderManager.get();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack it = inventory.getItem(i);
            if (it == null) continue;
            ItemStack clone = it.clone();

            if (clone.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = clone.getItemMeta();
                if (meta.hasDisplayName()) meta.setDisplayName(pm.apply(p, meta.getDisplayName()));
                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    List<String> out = new ArrayList<>();
                    for (String s : lore) out.add(pm.apply(p, s));
                    meta.setLore(out);
                }
                clone.setItemMeta(meta);
            }
            inventory.setItem(i, clone);
        }
    }

    // ---------------- Internal event handling ----------------

    void handleClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        int raw = e.getRawSlot();
        int size = inventory.getSize();
        boolean top = raw >= 0 && raw < size;
        int slotInView = e.getSlot();
        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        ClickContext ctx = new ClickContext(
                player,
                clicked == null ? new ItemStack(Material.AIR) : clicked.clone(),
                cursor == null ? new ItemStack(Material.AIR) : cursor.clone(),
                slotInView,
                raw,
                top,
                e.getClick(),
                e.getAction(),
                e.getHotbarButton(),
                e.isShiftClick(),
                e,
                defaultCancel
        );

        if (top) {
            Consumer<ClickContext> slotHandler = slotHandlers.get(raw);
            if (slotHandler != null) {
                try { slotHandler.accept(ctx); } catch (Exception ex) { ex.printStackTrace(); }
            } else {
                GUIButton legacy = legacyButtons.get(raw);
                if (legacy != null) {
                    try { legacy.click(player); } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
        }

        // global handler
        if (globalClickHandler != null) {
            try { globalClickHandler.accept(ctx); } catch (Exception ex) { ex.printStackTrace(); }
        }

        // shift / drag rules
        if (!allowShiftClick && e.isShiftClick() && top) ctx.setCancelled(true);
        if (!allowDrag && (e.getAction() == InventoryAction.COLLECT_TO_CURSOR ||
                e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)) ctx.setCancelled(true);

        e.setCancelled(ctx.isCancelled());
    }

    void handleDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        for (int slot : e.getRawSlots()) {
            if (slot < inventory.getSize()) {
                if (!allowDrag) { e.setCancelled(true); return; }
            }
        }
    }

    void handleOpen(org.bukkit.event.inventory.InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;

        applyPlaceholdersToPlayer(player);
        if (onOpen != null) {
            try { onOpen.accept(player); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    void handleClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (onClose != null) {
            try { onClose.accept(player); } catch (Exception ex) { ex.printStackTrace(); }
        }
        if (autoUnregisterWhenEmpty && inventory.getViewers().isEmpty()) GUIManager.get(plugin).unregister(this);
    }
}

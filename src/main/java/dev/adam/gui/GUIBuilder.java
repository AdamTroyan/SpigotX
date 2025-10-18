package dev.adam.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import dev.adam.gui.context.GUIClickContext;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GUIBuilder implements GUIBase {
    private final GUI gui;
    private final Map<Integer, String> slotPermissions = new HashMap<>();
    private final Map<String, Consumer<GUIClickContext>> namedHandlers = new HashMap<>();
    private final List<BuildStep> buildSteps = new ArrayList<>();
    private final Set<String> appliedTemplates = new HashSet<>();

    private boolean validateItems = true;
    private boolean autoFillBackground = false;
    private ItemStack defaultBackground;
    private String builderName = "UnnamedGUI";

    @FunctionalInterface
    public interface BuildStep {
        void apply(GUIBuilder builder);
    }

    public static class GUITemplate {
        private final String name;
        private final Map<Integer, ItemStack> items = new HashMap<>();
        private final Map<Integer, Consumer<GUIClickContext>> handlers = new HashMap<>();
        private final Map<String, Object> properties = new HashMap<>();
        private Consumer<GUIBuilder> postApply;

        public GUITemplate(String name) {
            this.name = name;
        }

        public GUITemplate setItem(int slot, ItemStack item, Consumer<GUIClickContext> handler) {
            items.put(slot, item);
            if (handler != null) handlers.put(slot, handler);
            return this;
        }

        public GUITemplate setProperty(String key, Object value) {
            properties.put(key, value);
            return this;
        }

        public GUITemplate setPostApply(Consumer<GUIBuilder> postApply) {
            this.postApply = postApply;
            return this;
        }

        public String getName() {
            return name;
        }

        public Map<Integer, ItemStack> getItems() {
            return items;
        }

        public Map<Integer, Consumer<GUIClickContext>> getHandlers() {
            return handlers;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public Consumer<GUIBuilder> getPostApply() {
            return postApply;
        }
    }

    public GUIBuilder(String title, int rows) {
        this.gui = new GUI(title, rows);
        this.builderName = title;
        this.defaultBackground = GUI.createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public GUIBuilder setItem(int slot, ItemStack item, Consumer<GUIClickContext> handler) {
        return setItem(slot, item, null, handler);
    }

    public GUIBuilder setItem(int slot, ItemStack item, String permission, Consumer<GUIClickContext> handler) {
        if (validateItems && (item == null || item.getItemMeta() == null || item.getItemMeta().getDisplayName() == null)) {
            throw new IllegalArgumentException("Item at slot " + slot + " must have a display name when validation is enabled!");
        }

        gui.setItem(slot, item, handler);
        if (permission != null && !permission.isEmpty()) {
            slotPermissions.put(slot, permission);
        }

        return this;
    }

    public GUIBuilder setItemIf(boolean condition, int slot, ItemStack item, Consumer<GUIClickContext> handler) {
        if (condition) {
            setItem(slot, item, handler);
        }
        return this;
    }

    public GUIBuilder setItemIf(Predicate<GUIBuilder> condition, int slot, ItemStack item, Consumer<GUIClickContext> handler) {
        if (condition.test(this)) {
            setItem(slot, item, handler);
        }
        return this;
    }

    public GUIBuilder registerHandler(String name, Consumer<GUIClickContext> handler) {
        namedHandlers.put(name, handler);
        return this;
    }

    public GUIBuilder setItemWithNamedHandler(int slot, ItemStack item, String handlerName) {
        Consumer<GUIClickContext> handler = namedHandlers.get(handlerName);
        if (handler == null) {
            throw new IllegalArgumentException("Handler '" + handlerName + "' not found!");
        }
        return setItem(slot, item, handler);
    }

    public GUIBuilder setItems(int[] slots, ItemStack item, Consumer<GUIClickContext> handler) {
        for (int slot : slots) {
            setItem(slot, item, handler);
        }
        return this;
    }

    public GUIBuilder setItemsWithDifferentHandlers(Map<Integer, ItemStack> items, Map<Integer, Consumer<GUIClickContext>> handlers) {
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            int slot = entry.getKey();
            setItem(slot, entry.getValue(), handlers.get(slot));
        }
        return this;
    }

    public GUIBuilder applyPattern(String[] pattern, Map<Character, ItemStack> items, Map<Character, Consumer<GUIClickContext>> handlers) {
        int slot = 0;
        for (String row : pattern) {
            for (char c : row.toCharArray()) {
                if (slot >= gui.getSlotCount()) break;

                ItemStack item = items.get(c);
                Consumer<GUIClickContext> handler = handlers != null ? handlers.get(c) : null;

                if (item != null) {
                    setItem(slot, item, handler);
                } else if (c == ' ' && autoFillBackground) {
                    setItem(slot, defaultBackground, null);
                }
                slot++;
            }
        }
        return this;
    }

    public GUIBuilder applyTemplate(GUITemplate template) {
        if (appliedTemplates.contains(template.getName())) {
            return this;
        }

        for (Map.Entry<Integer, ItemStack> entry : template.getItems().entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();
            Consumer<GUIClickContext> handler = template.getHandlers().get(slot);
            setItem(slot, item, handler);
        }

        for (Map.Entry<String, Object> entry : template.getProperties().entrySet()) {
            gui.setProperty(entry.getKey(), entry.getValue());
        }

        if (template.getPostApply() != null) {
            template.getPostApply().accept(this);
        }

        appliedTemplates.add(template.getName());
        return this;
    }

    public GUIBuilder addBuildStep(BuildStep step) {
        buildSteps.add(step);
        return this;
    }

    public GUIBuilder executeBuildSteps() {
        for (BuildStep step : buildSteps) {
            step.apply(this);
        }
        buildSteps.clear();
        return this;
    }

    public GUIBuilder fillArea(int topLeft, int bottomRight, ItemStack item, Consumer<GUIClickContext> handler) {
        int startRow = topLeft / 9;
        int startCol = topLeft % 9;
        int endRow = bottomRight / 9;
        int endCol = bottomRight % 9;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int slot = row * 9 + col;
                if (slot < gui.getSlotCount()) {
                    setItem(slot, item, handler);
                }
            }
        }
        return this;
    }

    public GUIBuilder fillCircle(int center, int radius, ItemStack item, Consumer<GUIClickContext> handler) {
        int centerRow = center / 9;
        int centerCol = center % 9;

        for (int slot = 0; slot < gui.getSlotCount(); slot++) {
            int row = slot / 9;
            int col = slot % 9;

            double distance = Math.sqrt(Math.pow(row - centerRow, 2) + Math.pow(col - centerCol, 2));
            if (distance <= radius) {
                setItem(slot, item, handler);
            }
        }
        return this;
    }

    public GUIBuilder fillDiamond(int center, int radius, ItemStack item, Consumer<GUIClickContext> handler) {
        int centerRow = center / 9;
        int centerCol = center % 9;

        for (int slot = 0; slot < gui.getSlotCount(); slot++) {
            int row = slot / 9;
            int col = slot % 9;

            int distance = Math.abs(row - centerRow) + Math.abs(col - centerCol);
            if (distance <= radius) {
                setItem(slot, item, handler);
            }
        }
        return this;
    }

    public GUIBuilder fillCheckered(ItemStack item1, ItemStack item2, Consumer<GUIClickContext> handler1, Consumer<GUIClickContext> handler2) {
        for (int slot = 0; slot < gui.getSlotCount(); slot++) {
            int row = slot / 9;
            int col = slot % 9;

            if ((row + col) % 2 == 0) {
                setItem(slot, item1, handler1);
            } else {
                setItem(slot, item2, handler2);
            }
        }
        return this;
    }

    public GUIBuilder fillSpiral(ItemStack item, Consumer<GUIClickContext> handler) {
        int rows = gui.getRows();
        int cols = 9;
        int top = 0, bottom = rows - 1, left = 0, right = cols - 1;

        while (top <= bottom && left <= right) {
            for (int col = left; col <= right; col++) {
                setItem(top * 9 + col, item, handler);
            }
            top++;

            for (int row = top; row <= bottom; row++) {
                setItem(row * 9 + right, item, handler);
            }
            right--;

            if (top <= bottom) {
                for (int col = right; col >= left; col--) {
                    setItem(bottom * 9 + col, item, handler);
                }
                bottom--;
            }

            if (left <= right) {
                for (int row = bottom; row >= top; row--) {
                    setItem(row * 9 + left, item, handler);
                }
                left++;
            }
        }
        return this;
    }

    public GUIBuilder setValidateItems(boolean validate) {
        this.validateItems = validate;
        return this;
    }

    public GUIBuilder setAutoFillBackground(boolean autoFill) {
        this.autoFillBackground = autoFill;
        return this;
    }

    public GUIBuilder setDefaultBackground(ItemStack background) {
        this.defaultBackground = background;
        return this;
    }

    public GUIBuilder setBuilderName(String name) {
        this.builderName = name;
        return this;
    }

    public GUIBuilder setOpenCondition(Predicate<Player> condition) {
        gui.setOpenCondition(condition);
        return this;
    }

    public GUIBuilder setGlobalClickHandler(Consumer<GUIClickContext> handler) {
        gui.setGlobalClickHandler(handler);
        return this;
    }

    public GUIBuilder setAutoRefresh(boolean autoRefresh, long intervalTicks) {
        gui.setAutoRefresh(autoRefresh, intervalTicks);
        return this;
    }

    public GUIBuilder setCloseSound(String sound) {
        gui.setCloseSound(sound);
        return this;
    }

    public GUIBuilder setBackgroundItem(ItemStack item) {
        gui.setBackgroundItem(item);
        return this;
    }

    public GUIBuilder setProperty(String key, Object value) {
        gui.setProperty(key, value);
        return this;
    }

    public <T> T getProperty(String key, Class<T> type) {
        return gui.getProperty(key, type);
    }

    public GUIBuilder quickItem(int slot, Material material, String name, Consumer<GUIClickContext> handler, String... lore) {
        ItemStack item = GUI.createItem(material, name, lore);
        return setItem(slot, item, handler);
    }

    public GUIBuilder quickButton(int slot, Material material, String name, Runnable action, String... lore) {
        return quickItem(slot, material, name, ctx -> {
            ctx.playClickSound();
            action.run();
        }, lore);
    }

    public GUIBuilder quickCloseButton(int slot, Material material, String name, String... lore) {
        return quickButton(slot, material, name, () -> {
        }, lore);
    }

    public boolean validate() {
        if (!validateItems) return true;

        for (int slot = 0; slot < gui.getSlotCount(); slot++) {
            ItemStack item = gui.getInventory().getItem(slot);
            if (item != null && !item.getType().isAir()) {
                if (item.getItemMeta() == null || item.getItemMeta().getDisplayName() == null) {
                    System.err.println("Warning: Item at slot " + slot + " in GUI '" + builderName + "' has no display name!");
                    return false;
                }
            }
        }

        return true;
    }


    public void debug() {
        System.out.println("=== GUI Builder Debug: " + builderName + " ===");
        System.out.println("Size: " + gui.getRows() + " rows (" + gui.getSlotCount() + " slots)");
        System.out.println("Filled slots: " + gui.getFilledSlotCount());
        System.out.println("Empty slots: " + gui.getEmptySlotCount());
        System.out.println("Named handlers: " + namedHandlers.size());
        System.out.println("Applied templates: " + appliedTemplates);
        System.out.println("Pending build steps: " + buildSteps.size());
        System.out.println("Properties: " + (gui.hasProperty("debug") ? "Has debug properties" : "No debug properties"));
        System.out.println("=====================================");
    }

    public GUIBuilder fillRowIfEmpty(int row, ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.fillRowIfEmpty(row, item, onClick);
        return this;
    }

    public GUIBuilder fillColumnIfEmpty(int col, ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.fillColumnIfEmpty(col, item, onClick);
        return this;
    }

    public GUIBuilder clearRow(int row) {
        gui.clearRow(row);
        return this;
    }

    public GUIBuilder clearColumn(int col) {
        gui.clearColumn(col);
        return this;
    }

    public GUIBuilder setItemsBulk(int[] slots, ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.setItemsBulk(slots, item, onClick);
        return this;
    }

    public GUIBuilder fillBorderIfEmpty(ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.fillBorderIfEmpty(item, onClick);
        return this;
    }

    public GUIBuilder replaceItem(Material from, ItemStack to, Consumer<GUIClickContext> onClick) {
        gui.replaceItem(from, to, onClick);
        return this;
    }

    public int getFirstEmptySlotInRow(int row) {
        return gui.getFirstEmptySlotInRow(row);
    }

    public GUIBuilder setItemIfEmpty(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.setItemIfEmpty(slot, item, onClick);
        return this;
    }

    public GUIBuilder setRow(int row, ItemStack[] items, Consumer<GUIClickContext>[] handlers) {
        gui.setRow(row, items, handlers);
        return this;
    }

    public GUIBuilder setBackground(ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.setBackground(item, onClick);
        return this;
    }

    public void removeItem(int slot) {
        gui.removeItem(slot);
        slotPermissions.remove(slot);
    }

    public GUIBuilder fillBorder(ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.fillBorder(item, onClick);
        return this;
    }

    public GUIBuilder onOpen(Consumer<Player> onOpen) {
        gui.setOnOpen(onOpen);
        return this;
    }

    public GUIBuilder onClose(Consumer<Player> onClose) {
        gui.setOnClose(onClose);
        return this;
    }

    public void open(Player player) {
        if (autoFillBackground) {
            gui.setBackground(defaultBackground, null);
        }
        executeBuildSteps();
        validate();
        gui.open(player);
    }

    public GUI build() {
        if (autoFillBackground) {
            gui.setBackground(defaultBackground, null);
        }
        executeBuildSteps();
        validate();
        return gui;
    }

    public static GUITemplate createBasicTemplate() {
        return new GUITemplate("basic")
                .setItem(0, GUI.createItem(Material.GRAY_STAINED_GLASS_PANE, " "), null)
                .setProperty("template_type", "basic");
    }

    public static GUITemplate createNavigationTemplate() {
        GUITemplate template = new GUITemplate("navigation");

        template.setItem(45, GUI.createItem(Material.ARROW, "&cPrevious Page"), ctx -> {
            ctx.sendMessage("Previous page clicked!");
        });

        template.setItem(53, GUI.createItem(Material.ARROW, "&aNext Page"), ctx -> {
            ctx.sendMessage("Next page clicked!");
        });

        template.setItem(49, GUI.createItem(Material.BARRIER, "&cClose"), ctx -> {
            ctx.closeInventory();
        });

        return template;
    }

    @Override
    public Inventory getInventory() {
        return gui.getInventory();
    }

    @Override
    public boolean hasHandler(int slot) {
        return gui.hasHandler(slot);
    }

    @Override
    public void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();

        if (slotPermissions.containsKey(slot) && !player.hasPermission(slotPermissions.get(slot))) {
            player.sendMessage("§cYou don't have permission to use this button.");
            return;
        }

        gui.handleClick(event);
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        gui.handleClose(event);
    }

    public String getBuilderName() {
        return builderName;
    }

    public boolean isValidateItems() {
        return validateItems;
    }

    public boolean isAutoFillBackground() {
        return autoFillBackground;
    }

    public ItemStack getDefaultBackground() {
        return defaultBackground;
    }

    public Set<String> getAppliedTemplates() {
        return new HashSet<>(appliedTemplates);
    }

    public GUI getGUI() {
        return gui;
    }
}
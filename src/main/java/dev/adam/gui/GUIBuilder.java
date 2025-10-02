package dev.adam.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GUIBuilder {

    private final Plugin plugin;
    private final String title;
    private final int rows;
    private final List<Runnable> ops = new ArrayList<>();
    private Consumer<ClickContext> globalHandler;
    private Consumer<Player> onOpen;
    private Consumer<Player> onClose;

    private GUIBuilder(Plugin plugin, String title, int rows) {
        this.plugin = plugin; this.title = title; this.rows = rows;
    }

    public static GUIBuilder create(Plugin plugin, String title, int rows) {
        return new GUIBuilder(plugin, title, rows);
    }

    public GUIBuilder item(int slot, ItemStack item, Consumer<ClickContext> action) {
        ops.add(() -> builderGui().setItem(slot, item, action));
        return this;
    }

    public GUIBuilder itemLegacy(int slot, ItemStack item, Consumer<Player> action) {
        ops.add(() -> builderGui().setItemForPlayer(slot, item, action));
        return this;
    }

    public GUIBuilder fillBorder(ItemStack item, Consumer<ClickContext> action) {
        ops.add(() -> builderGui().fillBorder(item, action));
        return this;
    }

    public GUIBuilder onOpen(Consumer<Player> c) { this.onOpen = c; return this; }
    public GUIBuilder onClose(Consumer<Player> c) { this.onClose = c; return this; }
    public GUIBuilder globalHandler(Consumer<ClickContext> handler) { this.globalHandler = handler; return this; }

    private GUI _built;
    private GUI builderGui() {
        if (_built == null) {
            _built = new GUI(plugin, title, rows);
            if (globalHandler != null) _built.setGlobalClickHandler(globalHandler);
            if (onOpen != null) _built.setOnOpen(onOpen);
            if (onClose != null) _built.setOnClose(onClose);
        }
        return _built;
    }

    public GUI build() {
        GUI g = builderGui();
        for (Runnable op : ops) try { op.run(); } catch (Exception ex) { ex.printStackTrace(); }
        return g;
    }
}

package dev.adam.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.adam.gui.context.GUIClickContext;
import java.util.function.Consumer;

public class GUIBuilder {
    private final GUI gui;

    public GUIBuilder(String title, int rows) {
        this.gui = new GUI(title, rows);
    }

    public GUIBuilder setItem(int slot, ItemStack item, Consumer<GUIClickContext> onClick) {
        gui.setItem(slot, item, onClick);
        return this;
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

    public GUI build() {
        return gui;
    }

    public void open(Player player) {
        GUIListener.openGui(player, gui);
    }
}
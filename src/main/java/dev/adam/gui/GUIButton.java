package dev.adam.gui;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class GUIButton {
    private final int slot;
    private final Consumer<Player> action;

    public GUIButton(int slot, Consumer<Player> action) {
        this.slot = slot;
        this.action = action;
    }

    public int getSlot() {
        return slot;
    }

    public void click(Player player) {
        action.accept(player);
    }
}

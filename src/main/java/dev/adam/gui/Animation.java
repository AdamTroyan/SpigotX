package dev.adam.gui;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Animation {
    private final List<ItemStack> frames;
    private final long periodTicks;
    private int index = 0;

    public Animation(List<ItemStack> frames, long periodTicks) {
        this.frames = frames;
        this.periodTicks = periodTicks;
    }

    public ItemStack current() {
        return frames.get(index % frames.size());
    }

    public void advance() { index = (index + 1) % frames.size(); }

    public long periodTicks() { return periodTicks; }
}

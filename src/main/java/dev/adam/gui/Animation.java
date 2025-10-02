package dev.adam.gui;

import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class Animation {

    private final List<ItemStack> frames;
    private final long periodTicks;
    private int index = 0;

    public Animation(List<ItemStack> frames, long periodTicks) {
        if (frames == null || frames.isEmpty()) throw new IllegalArgumentException("Frames cannot be null or empty");
        this.frames = Collections.unmodifiableList(frames);
        this.periodTicks = periodTicks;
    }

    public synchronized ItemStack current() {
        return frames.get(index % frames.size()).clone();
    }

    public synchronized void advance() { index = (index + 1) % frames.size(); }

    public long periodTicks() { return periodTicks; }
}

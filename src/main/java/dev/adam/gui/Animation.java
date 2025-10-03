package dev.adam.gui;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public class Animation {
    private final List<ItemStack> frames;
    private final long ticksPerFrame;
    private int currentFrame = 0;

    public Animation(List<ItemStack> frames, long ticksPerFrame) {
        this.frames = frames;
        this.ticksPerFrame = ticksPerFrame;
    }

    public ItemStack nextFrame() {
        ItemStack frame = frames.get(currentFrame);
        currentFrame = (currentFrame + 1) % frames.size();
        return frame;
    }

    public long getTicksPerFrame() {
        return ticksPerFrame;
    }
}
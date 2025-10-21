package dev.adam.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Animation system for GUI items with frame-based progression.
 * 
 * This class provides a comprehensive animation framework for creating dynamic
 * and interactive GUI elements. It supports various animation types including
 * frame sequences, color transitions, material cycling, and procedural animations.
 * The system is designed to be lightweight and efficient while offering extensive
 * customization options for creating engaging user interfaces.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Frame-based animation with customizable timing</li>
 *   <li>Multiple animation types (sequence, ping-pong, random, procedural)</li>
 *   <li>Color transition support with smooth gradients</li>
 *   <li>Material cycling for dynamic item type changes</li>
 *   <li>Animation state management (play, pause, stop, reset)</li>
 *   <li>Loop control with count limits and infinite loops</li>
 *   <li>Event callbacks for animation lifecycle</li>
 *   <li>Performance optimization with frame caching</li>
 *   <li>Builder pattern for easy animation creation</li>
 *   <li>Predefined animation templates for common use cases</li>
 * </ul>
 * 
 * <p>Animation types:</p>
 * <ul>
 *   <li><strong>SEQUENCE</strong> - Plays frames in order, then restarts</li>
 *   <li><strong>PING_PONG</strong> - Plays forward then backward repeatedly</li>
 *   <li><strong>RANDOM</strong> - Selects random frames each time</li>
 *   <li><strong>PROCEDURAL</strong> - Uses a supplier function to generate frames</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a simple loading animation
 * Animation loading = Animation.builder()
 *     .addFrame(GUI.createItem(Material.GRAY_STAINED_GLASS_PANE, "&7Loading."))
 *     .addFrame(GUI.createItem(Material.GRAY_STAINED_GLASS_PANE, "&7Loading.."))
 *     .addFrame(GUI.createItem(Material.GRAY_STAINED_GLASS_PANE, "&7Loading..."))
 *     .setTicksPerFrame(10)
 *     .setLoops(-1) // Infinite
 *     .build();
 * 
 * // Create a color transition
 * Animation rainbow = Animation.createRainbowAnimation(20, 5);
 * 
 * // Create a material cycle
 * Animation gems = Animation.createMaterialCycle(
 *     Arrays.asList(Material.DIAMOND, Material.EMERALD, Material.RUBY), 15);
 * }</pre>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class Animation {
    
    /**
     * Enumeration of available animation types.
     */
    public enum AnimationType {
        /** Plays frames in sequence, then restarts from beginning */
        SEQUENCE,
        /** Plays frames forward, then backward, then repeats */
        PING_PONG,
        /** Selects a random frame each time */
        RANDOM,
        /** Uses a procedural function to generate frames dynamically */
        PROCEDURAL
    }

    /**
     * Enumeration of animation states.
     */
    public enum AnimationState {
        /** Animation is currently playing */
        PLAYING,
        /** Animation is paused (can be resumed) */
        PAUSED,
        /** Animation is stopped (must be reset to play again) */
        STOPPED,
        /** Animation has completed all loops */
        FINISHED
    }

    // === CORE PROPERTIES ===
    
    /** List of animation frames */
    private final List<ItemStack> frames;
    /** Time between frame changes in ticks */
    private final long ticksPerFrame;
    /** Type of animation playback */
    private final AnimationType type;
    /** Maximum number of loops (-1 for infinite) */
    private final int maxLoops;
    /** Procedural frame supplier for PROCEDURAL type */
    private final Supplier<ItemStack> frameSupplier;

    // === RUNTIME STATE ===
    
    /** Current frame index */
    private int currentFrame = 0;
    /** Current animation state */
    private AnimationState state = AnimationState.STOPPED;
    /** Current loop count */
    private int currentLoop = 0;
    /** Direction for ping-pong animations (1 = forward, -1 = backward) */
    private int direction = 1;
    /** Random instance for random animations */
    private final Random random = new Random();
    /** Timestamp of last frame change */
    private long lastFrameTime = 0;

    // === EVENT CALLBACKS ===
    
    /** Callback executed when animation starts */
    private Consumer<Animation> onStart;
    /** Callback executed when animation pauses */
    private Consumer<Animation> onPause;
    /** Callback executed when animation resumes */
    private Consumer<Animation> onResume;
    /** Callback executed when animation stops */
    private Consumer<Animation> onStop;
    /** Callback executed when animation finishes all loops */
    private Consumer<Animation> onFinish;
    /** Callback executed on each frame change */
    private Consumer<ItemStack> onFrameChange;
    /** Callback executed when a loop completes */
    private Consumer<Integer> onLoopComplete;

    // === CONSTRUCTORS ===

    /**
     * Creates a new animation with the specified frames and timing.
     * Uses SEQUENCE animation type with infinite loops.
     * 
     * @param frames the list of animation frames
     * @param ticksPerFrame the time between frame changes in ticks
     */
    public Animation(List<ItemStack> frames, long ticksPerFrame) {
        this(frames, ticksPerFrame, AnimationType.SEQUENCE, -1, null);
    }

    /**
     * Creates a new animation with full configuration.
     * 
     * @param frames the list of animation frames (can be null for PROCEDURAL type)
     * @param ticksPerFrame the time between frame changes in ticks
     * @param type the animation playback type
     * @param maxLoops the maximum number of loops (-1 for infinite)
     * @param frameSupplier the frame supplier for PROCEDURAL type (can be null for other types)
     */
    public Animation(List<ItemStack> frames, long ticksPerFrame, AnimationType type, 
                    int maxLoops, Supplier<ItemStack> frameSupplier) {
        this.frames = frames != null ? new ArrayList<>(frames) : new ArrayList<>();
        this.ticksPerFrame = Math.max(1, ticksPerFrame);
        this.type = type;
        this.maxLoops = maxLoops;
        this.frameSupplier = frameSupplier;

        validateConfiguration();
    }

    /**
     * Validates the animation configuration for consistency.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateConfiguration() {
        if (type == AnimationType.PROCEDURAL) {
            if (frameSupplier == null) {
                throw new IllegalArgumentException("PROCEDURAL animations require a frame supplier!");
            }
        } else {
            if (frames.isEmpty()) {
                throw new IllegalArgumentException("Non-procedural animations require at least one frame!");
            }
        }
    }

    // === FRAME MANAGEMENT ===

    /**
     * Gets the current frame of the animation.
     * This method handles the timing logic and automatically advances frames.
     * 
     * @return the current frame ItemStack
     */
    public ItemStack getCurrentFrame() {
        if (state != AnimationState.PLAYING) {
            return getCurrentFrameStatic();
        }

        long currentTime = System.currentTimeMillis() / 50; // Convert to ticks (rough approximation)
        if (currentTime - lastFrameTime >= ticksPerFrame) {
            advanceFrame();
            lastFrameTime = currentTime;
        }

        return getCurrentFrameStatic();
    }

    /**
     * Gets the current frame without advancing the animation.
     * 
     * @return the current frame ItemStack
     */
    public ItemStack getCurrentFrameStatic() {
        if (type == AnimationType.PROCEDURAL && frameSupplier != null) {
            try {
                return frameSupplier.get();
            } catch (Exception e) {
                System.err.println("Error in procedural frame supplier: " + e.getMessage());
                return createErrorFrame();
            }
        }

        if (frames.isEmpty()) {
            return createErrorFrame();
        }

        int index = Math.max(0, Math.min(currentFrame, frames.size() - 1));
        ItemStack frame = frames.get(index);
        return frame != null ? frame.clone() : createErrorFrame();
    }

    /**
     * Manually advances to the next frame.
     * Respects animation type and loop settings.
     * 
     * @return the next frame ItemStack
     */
    public ItemStack nextFrame() {
        if (state == AnimationState.FINISHED) {
            return getCurrentFrameStatic();
        }

        advanceFrame();

        if (onFrameChange != null) {
            try {
                onFrameChange.accept(getCurrentFrameStatic());
            } catch (Exception e) {
                System.err.println("Error in frame change callback: " + e.getMessage());
            }
        }

        return getCurrentFrameStatic();
    }

    /**
     * Internal method to advance the frame index based on animation type.
     */
    private void advanceFrame() {
        if (frames.isEmpty() && type != AnimationType.PROCEDURAL) return;

        switch (type) {
            case SEQUENCE:
                advanceSequential();
                break;
            case PING_PONG:
                advancePingPong();
                break;
            case RANDOM:
                advanceRandom();
                break;
            case PROCEDURAL:
                // Procedural animations don't advance frame index
                checkLoopCompletion();
                break;
        }
    }

    /**
     * Advances frame for sequential animation type.
     */
    private void advanceSequential() {
        currentFrame++;
        if (currentFrame >= frames.size()) {
            currentFrame = 0;
            completeLoop();
        }
    }

    /**
     * Advances frame for ping-pong animation type.
     */
    private void advancePingPong() {
        currentFrame += direction;

        if (currentFrame >= frames.size() - 1) {
            direction = -1;
            currentFrame = frames.size() - 1;
        } else if (currentFrame <= 0) {
            direction = 1;
            currentFrame = 0;
            completeLoop();
        }
    }

    /**
     * Advances frame for random animation type.
     */
    private void advanceRandom() {
        int oldFrame = currentFrame;
        // Ensure we don't pick the same frame twice in a row (if possible)
        if (frames.size() > 1) {
            do {
                currentFrame = random.nextInt(frames.size());
            } while (currentFrame == oldFrame);
        }
        
        // For random animations, we consider each frame change a "loop"
        completeLoop();
    }

    /**
     * Handles loop completion logic.
     */
    private void completeLoop() {
        currentLoop++;
        
        if (onLoopComplete != null) {
            try {
                onLoopComplete.accept(currentLoop);
            } catch (Exception e) {
                System.err.println("Error in loop complete callback: " + e.getMessage());
            }
        }

        if (maxLoops > 0 && currentLoop >= maxLoops) {
            state = AnimationState.FINISHED;
            if (onFinish != null) {
                try {
                    onFinish.accept(this);
                } catch (Exception e) {
                    System.err.println("Error in finish callback: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles loop completion for procedural animations.
     */
    private void checkLoopCompletion() {
        // For procedural animations, we complete a loop every frame
        completeLoop();
    }

    // === ANIMATION CONTROL ===

    /**
     * Starts or resumes the animation.
     */
    public void play() {
        if (state == AnimationState.FINISHED) {
            reset();
        }

        AnimationState oldState = state;
        state = AnimationState.PLAYING;
        lastFrameTime = System.currentTimeMillis() / 50;

        if (oldState == AnimationState.STOPPED && onStart != null) {
            try {
                onStart.accept(this);
            } catch (Exception e) {
                System.err.println("Error in start callback: " + e.getMessage());
            }
        } else if (oldState == AnimationState.PAUSED && onResume != null) {
            try {
                onResume.accept(this);
            } catch (Exception e) {
                System.err.println("Error in resume callback: " + e.getMessage());
            }
        }
    }

    /**
     * Pauses the animation without resetting progress.
     */
    public void pause() {
        if (state == AnimationState.PLAYING) {
            state = AnimationState.PAUSED;
            if (onPause != null) {
                try {
                    onPause.accept(this);
                } catch (Exception e) {
                    System.err.println("Error in pause callback: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Stops the animation and resets to the beginning.
     */
    public void stop() {
        state = AnimationState.STOPPED;
        reset();
        if (onStop != null) {
            try {
                onStop.accept(this);
            } catch (Exception e) {
                System.err.println("Error in stop callback: " + e.getMessage());
            }
        }
    }

    /**
     * Resets the animation to its initial state.
     */
    public void reset() {
        currentFrame = 0;
        currentLoop = 0;
        direction = 1;
        if (state != AnimationState.PLAYING) {
            state = AnimationState.STOPPED;
        }
    }

    /**
     * Skips to a specific frame.
     * 
     * @param frameIndex the frame index to skip to
     */
    public void skipToFrame(int frameIndex) {
        if (type != AnimationType.PROCEDURAL && frameIndex >= 0 && frameIndex < frames.size()) {
            currentFrame = frameIndex;
        }
    }

    // === EVENT HANDLERS ===

    /**
     * Sets the callback to execute when animation starts.
     * 
     * @param callback the start callback
     */
    public void setOnStart(Consumer<Animation> callback) {
        this.onStart = callback;
    }

    /**
     * Sets the callback to execute when animation pauses.
     * 
     * @param callback the pause callback
     */
    public void setOnPause(Consumer<Animation> callback) {
        this.onPause = callback;
    }

    /**
     * Sets the callback to execute when animation resumes.
     * 
     * @param callback the resume callback
     */
    public void setOnResume(Consumer<Animation> callback) {
        this.onResume = callback;
    }

    /**
     * Sets the callback to execute when animation stops.
     * 
     * @param callback the stop callback
     */
    public void setOnStop(Consumer<Animation> callback) {
        this.onStop = callback;
    }

    /**
     * Sets the callback to execute when animation finishes all loops.
     * 
     * @param callback the finish callback
     */
    public void setOnFinish(Consumer<Animation> callback) {
        this.onFinish = callback;
    }

    /**
     * Sets the callback to execute on each frame change.
     * 
     * @param callback the frame change callback
     */
    public void setOnFrameChange(Consumer<ItemStack> callback) {
        this.onFrameChange = callback;
    }

    /**
     * Sets the callback to execute when a loop completes.
     * 
     * @param callback the loop complete callback
     */
    public void setOnLoopComplete(Consumer<Integer> callback) {
        this.onLoopComplete = callback;
    }

    // === GETTERS ===

    /**
     * Gets the time between frames in ticks.
     * 
     * @return the ticks per frame
     */
    public long getTicksPerFrame() {
        return ticksPerFrame;
    }

    /**
     * Gets the animation type.
     * 
     * @return the animation type
     */
    public AnimationType getType() {
        return type;
    }

    /**
     * Gets the current animation state.
     * 
     * @return the animation state
     */
    public AnimationState getState() {
        return state;
    }

    /**
     * Gets the maximum number of loops.
     * 
     * @return the max loops (-1 for infinite)
     */
    public int getMaxLoops() {
        return maxLoops;
    }

    /**
     * Gets the current loop count.
     * 
     * @return the current loop number
     */
    public int getCurrentLoop() {
        return currentLoop;
    }

    /**
     * Gets the current frame index.
     * 
     * @return the current frame index
     */
    public int getCurrentFrameIndex() {
        return currentFrame;
    }

    /**
     * Gets the total number of frames.
     * 
     * @return the frame count
     */
    public int getFrameCount() {
        return frames.size();
    }

    /**
     * Checks if the animation is currently playing.
     * 
     * @return true if playing, false otherwise
     */
    public boolean isPlaying() {
        return state == AnimationState.PLAYING;
    }

    /**
     * Checks if the animation is paused.
     * 
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return state == AnimationState.PAUSED;
    }

    /**
     * Checks if the animation is stopped.
     * 
     * @return true if stopped, false otherwise
     */
    public boolean isStopped() {
        return state == AnimationState.STOPPED;
    }

    /**
     * Checks if the animation has finished all loops.
     * 
     * @return true if finished, false otherwise
     */
    public boolean isFinished() {
        return state == AnimationState.FINISHED;
    }

    /**
     * Checks if the animation loops infinitely.
     * 
     * @return true if infinite loops, false otherwise
     */
    public boolean isInfinite() {
        return maxLoops == -1;
    }

    // === UTILITY METHODS ===

    /**
     * Creates an error frame to display when something goes wrong.
     * 
     * @return an error ItemStack
     */
    private ItemStack createErrorFrame() {
        ItemStack error = new ItemStack(Material.BARRIER);
        ItemMeta meta = error.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cAnimation Error");
            error.setItemMeta(meta);
        }
        return error;
    }

    /**
     * Creates a clone of this animation with the same configuration.
     * The clone starts in STOPPED state regardless of the original's state.
     * 
     * @return a cloned Animation instance
     */
    public Animation clone() {
        List<ItemStack> clonedFrames = new ArrayList<>();
        for (ItemStack frame : frames) {
            clonedFrames.add(frame != null ? frame.clone() : null);
        }

        Animation clone = new Animation(clonedFrames, ticksPerFrame, type, maxLoops, frameSupplier);
        
        // Copy callbacks
        clone.setOnStart(onStart);
        clone.setOnPause(onPause);
        clone.setOnResume(onResume);
        clone.setOnStop(onStop);
        clone.setOnFinish(onFinish);
        clone.setOnFrameChange(onFrameChange);
        clone.setOnLoopComplete(onLoopComplete);

        return clone;
    }

    // === STATIC FACTORY METHODS ===

    /**
     * Creates a builder for constructing animations fluently.
     * 
     * @return a new AnimationBuilder instance
     */
    public static AnimationBuilder builder() {
        return new AnimationBuilder();
    }

    /**
     * Creates a simple loading animation with dots.
     * 
     * @param material the material to use for all frames
     * @param ticksPerFrame the time between frame changes
     * @return a loading animation
     */
    public static Animation createLoadingAnimation(Material material, long ticksPerFrame) {
        return builder()
                .addFrame(GUI.createItem(material, "&7Loading."))
                .addFrame(GUI.createItem(material, "&7Loading.."))
                .addFrame(GUI.createItem(material, "&7Loading..."))
                .setTicksPerFrame(ticksPerFrame)
                .setLoops(-1)
                .build();
    }

    /**
     * Creates a rainbow color animation using stained glass.
     * 
     * @param ticksPerFrame the time between frame changes
     * @param cycles the number of color cycles (-1 for infinite)
     * @return a rainbow animation
     */
    public static Animation createRainbowAnimation(long ticksPerFrame, int cycles) {
        Material[] colors = {
                Material.RED_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.LIME_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE,
                Material.PURPLE_STAINED_GLASS_PANE,
                Material.MAGENTA_STAINED_GLASS_PANE
        };

        AnimationBuilder builder = builder().setTicksPerFrame(ticksPerFrame).setLoops(cycles);

        for (Material color : colors) {
            builder.addFrame(GUI.createItem(color, "&f⬢"));
        }

        return builder.build();
    }

    /**
     * Creates a material cycling animation.
     * 
     * @param materials the materials to cycle through
     * @param ticksPerFrame the time between frame changes
     * @return a material cycle animation
     */
    public static Animation createMaterialCycle(List<Material> materials, long ticksPerFrame) {
        AnimationBuilder builder = builder().setTicksPerFrame(ticksPerFrame).setLoops(-1);

        for (Material material : materials) {
            builder.addFrame(new ItemStack(material));
        }

        return builder.build();
    }

    /**
     * Creates a procedural animation using a supplier function.
     * 
     * @param supplier the function to generate frames
     * @param ticksPerFrame the time between frame changes
     * @param maxLoops the maximum number of loops
     * @return a procedural animation
     */
    public static Animation createProcedural(Supplier<ItemStack> supplier, long ticksPerFrame, int maxLoops) {
        return new Animation(null, ticksPerFrame, AnimationType.PROCEDURAL, maxLoops, supplier);
    }

    // === BUILDER CLASS ===

    /**
     * Builder class for creating animations with a fluent API.
     */
    public static class AnimationBuilder {
        private final List<ItemStack> frames = new ArrayList<>();
        private long ticksPerFrame = 20;
        private AnimationType type = AnimationType.SEQUENCE;
        private int maxLoops = -1;
        private Supplier<ItemStack> frameSupplier;

        /**
         * Adds a frame to the animation.
         * 
         * @param frame the ItemStack frame to add
         * @return this builder for method chaining
         */
        public AnimationBuilder addFrame(ItemStack frame) {
            frames.add(frame);
            return this;
        }

        /**
         * Adds multiple frames to the animation.
         * 
         * @param frames the ItemStack frames to add
         * @return this builder for method chaining
         */
        public AnimationBuilder addFrames(ItemStack... frames) {
            this.frames.addAll(Arrays.asList(frames));
            return this;
        }

        /**
         * Adds multiple frames from a collection.
         * 
         * @param frames the collection of frames to add
         * @return this builder for method chaining
         */
        public AnimationBuilder addFrames(Collection<ItemStack> frames) {
            this.frames.addAll(frames);
            return this;
        }

        /**
         * Sets the time between frame changes.
         * 
         * @param ticks the ticks per frame
         * @return this builder for method chaining
         */
        public AnimationBuilder setTicksPerFrame(long ticks) {
            this.ticksPerFrame = Math.max(1, ticks);
            return this;
        }

        /**
         * Sets the animation type.
         * 
         * @param type the animation type
         * @return this builder for method chaining
         */
        public AnimationBuilder setType(AnimationType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the maximum number of loops.
         * 
         * @param loops the max loops (-1 for infinite)
         * @return this builder for method chaining
         */
        public AnimationBuilder setLoops(int loops) {
            this.maxLoops = loops;
            return this;
        }

        /**
         * Sets the frame supplier for procedural animations.
         * 
         * @param supplier the frame supplier function
         * @return this builder for method chaining
         */
        public AnimationBuilder setFrameSupplier(Supplier<ItemStack> supplier) {
            this.frameSupplier = supplier;
            return this;
        }

        /**
         * Builds the animation with the configured parameters.
         * 
         * @return the constructed Animation instance
         */
        public Animation build() {
            return new Animation(frames, ticksPerFrame, type, maxLoops, frameSupplier);
        }
    }
}
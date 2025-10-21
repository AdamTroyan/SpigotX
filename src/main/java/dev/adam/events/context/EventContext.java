package dev.adam.events.context;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.World;
import org.bukkit.Location;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Context wrapper for Bukkit events that provides easy access to common event properties.
 * 
 * This class wraps any Bukkit event and provides a unified API for accessing commonly
 * needed objects like players, entities, blocks, worlds, and locations. It uses both
 * direct type checking and reflection to extract information from various event types,
 * making it easier to write generic event handlers.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Unified access to players, entities, blocks, worlds, and locations</li>
 *   <li>Reflection-based property access for custom event types</li>
 *   <li>Event cancellation management</li>
 *   <li>Metadata storage for custom data</li>
 *   <li>Type checking utilities</li>
 *   <li>Optional-based safe access methods</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * EventContext<PlayerJoinEvent> ctx = new EventContext<>(event);
 * 
 * // Access player safely
 * if (ctx.hasPlayer()) {
 *     Player player = ctx.getPlayer();
 *     player.sendMessage("Welcome!");
 * }
 * 
 * // Use optional for safe access
 * ctx.getPlayerOptional().ifPresent(player -> {
 *     player.teleport(ctx.getWorld().getSpawnLocation());
 * });
 * 
 * // Store custom metadata
 * ctx.setMetadata("handled", true);
 * ctx.setMetadata("handler", "welcome-system");
 * 
 * // Cancel events if needed
 * if (ctx.isCancellable()) {
 *     ctx.cancel();
 * }
 * }</pre>
 * 
 * @param <T> the specific event type this context wraps
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class EventContext<T extends Event> {
    private final T event;
    private final Map<String, Object> metadata = new HashMap<>();
    private final long timestamp = System.currentTimeMillis();

    /**
     * Creates a new event context wrapping the specified event.
     * 
     * @param event the event to wrap
     */
    public EventContext(T event) {
        this.event = event;
    }

    // === BASIC EVENT ACCESS ===

    /**
     * Gets the wrapped event instance.
     * 
     * @return the original event object
     */
    public T getEvent() {
        return event;
    }

    /**
     * Gets the timestamp when this context was created.
     * 
     * @return the creation timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the class type of the wrapped event.
     * 
     * @return the event class
     */
    public Class<? extends Event> getEventType() {
        return event.getClass();
    }

    /**
     * Gets the simple name of the event class.
     * 
     * @return the event class simple name
     */
    public String getEventName() {
        return event.getClass().getSimpleName();
    }

    // === PLAYER ACCESS ===

    /**
     * Attempts to extract a player from the event using multiple strategies.
     * First checks for PlayerEvent, then EntityEvent with player entity,
     * then uses reflection to find getPlayer() or getEntity() methods.
     * 
     * @return the player involved in the event, or null if none found
     */
    public Player getPlayer() {
        if (event instanceof PlayerEvent) {
            return ((PlayerEvent) event).getPlayer();
        }

        if (event instanceof EntityEvent) {
            Entity entity = ((EntityEvent) event).getEntity();
            if (entity instanceof Player) {
                return (Player) entity;
            }
        }

        try {
            Method getPlayerMethod = event.getClass().getMethod("getPlayer");
            Object result = getPlayerMethod.invoke(event);
            if (result instanceof Player) {
                return (Player) result;
            }
        } catch (Exception ignored) {
        }

        try {
            Method getEntityMethod = event.getClass().getMethod("getEntity");
            Object result = getEntityMethod.invoke(event);
            if (result instanceof Player) {
                return (Player) result;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Gets the player wrapped in an Optional for safe access.
     * 
     * @return an Optional containing the player, or empty if no player found
     */
    public Optional<Player> getPlayerOptional() {
        return Optional.ofNullable(getPlayer());
    }

    /**
     * Checks if this event involves a player.
     * 
     * @return true if a player is involved, false otherwise
     */
    public boolean hasPlayer() {
        return getPlayer() != null;
    }

    // === ENTITY ACCESS ===

    /**
     * Attempts to extract an entity from the event using multiple strategies.
     * First checks for EntityEvent, then uses reflection to find getEntity() method.
     * 
     * @return the entity involved in the event, or null if none found
     */
    public Entity getEntity() {
        if (event instanceof EntityEvent) {
            return ((EntityEvent) event).getEntity();
        }

        try {
            Method getEntityMethod = event.getClass().getMethod("getEntity");
            Object result = getEntityMethod.invoke(event);
            if (result instanceof Entity) {
                return (Entity) result;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Gets the entity wrapped in an Optional for safe access.
     * 
     * @return an Optional containing the entity, or empty if no entity found
     */
    public Optional<Entity> getEntityOptional() {
        return Optional.ofNullable(getEntity());
    }

    /**
     * Checks if this event involves an entity.
     * 
     * @return true if an entity is involved, false otherwise
     */
    public boolean hasEntity() {
        return getEntity() != null;
    }

    // === BLOCK ACCESS ===

    /**
     * Attempts to extract a block from the event using multiple strategies.
     * First checks for BlockEvent, then uses reflection to find getBlock() method.
     * 
     * @return the block involved in the event, or null if none found
     */
    public Block getBlock() {
        if (event instanceof BlockEvent) {
            return ((BlockEvent) event).getBlock();
        }

        try {
            Method getBlockMethod = event.getClass().getMethod("getBlock");
            Object result = getBlockMethod.invoke(event);
            if (result instanceof Block) {
                return (Block) result;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Gets the block wrapped in an Optional for safe access.
     * 
     * @return an Optional containing the block, or empty if no block found
     */
    public Optional<Block> getBlockOptional() {
        return Optional.ofNullable(getBlock());
    }

    /**
     * Checks if this event involves a block.
     * 
     * @return true if a block is involved, false otherwise
     */
    public boolean hasBlock() {
        return getBlock() != null;
    }

    // === INVENTORY ACCESS ===

    /**
     * Attempts to extract an inventory from the event using multiple strategies.
     * First checks for InventoryEvent, then uses reflection to find getInventory() method.
     * 
     * @return the inventory involved in the event, or null if none found
     */
    public Inventory getInventory() {
        if (event instanceof InventoryEvent) {
            return ((InventoryEvent) event).getInventory();
        }

        try {
            Method getInventoryMethod = event.getClass().getMethod("getInventory");
            Object result = getInventoryMethod.invoke(event);
            if (result instanceof Inventory) {
                return (Inventory) result;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Gets the inventory wrapped in an Optional for safe access.
     * 
     * @return an Optional containing the inventory, or empty if no inventory found
     */
    public Optional<Inventory> getInventoryOptional() {
        return Optional.ofNullable(getInventory());
    }

    /**
     * Checks if this event involves an inventory.
     * 
     * @return true if an inventory is involved, false otherwise
     */
    public boolean hasInventory() {
        return getInventory() != null;
    }

    // === WORLD ACCESS ===

    /**
     * Attempts to extract a world from the event using multiple strategies.
     * Checks WorldEvent, then derives world from player, entity, or block,
     * finally uses reflection to find getWorld() method.
     * 
     * @return the world involved in the event, or null if none found
     */
    public World getWorld() {
        if (event instanceof WorldEvent) {
            return ((WorldEvent) event).getWorld();
        }

        Player player = getPlayer();
        if (player != null) {
            return player.getWorld();
        }

        Entity entity = getEntity();
        if (entity != null) {
            return entity.getWorld();
        }

        Block block = getBlock();
        if (block != null) {
            return block.getWorld();
        }

        try {
            Method getWorldMethod = event.getClass().getMethod("getWorld");
            Object result = getWorldMethod.invoke(event);
            if (result instanceof World) {
                return (World) result;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Gets the world wrapped in an Optional for safe access.
     * 
     * @return an Optional containing the world, or empty if no world found
     */
    public Optional<World> getWorldOptional() {
        return Optional.ofNullable(getWorld());
    }

    /**
     * Checks if this event involves a world.
     * 
     * @return true if a world is involved, false otherwise
     */
    public boolean hasWorld() {
        return getWorld() != null;
    }

    // === LOCATION ACCESS ===

    /**
     * Attempts to extract a location from the event using multiple strategies.
     * Derives location from player, entity, or block, then uses reflection
     * to find getLocation() method.
     * 
     * @return the location involved in the event, or null if none found
     */
    public Location getLocation() {
        Player player = getPlayer();
        if (player != null) {
            return player.getLocation();
        }

        Entity entity = getEntity();
        if (entity != null) {
            return entity.getLocation();
        }

        Block block = getBlock();
        if (block != null) {
            return block.getLocation();
        }

        try {
            Method getLocationMethod = event.getClass().getMethod("getLocation");
            Object result = getLocationMethod.invoke(event);
            if (result instanceof Location) {
                return (Location) result;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Gets the location wrapped in an Optional for safe access.
     * 
     * @return an Optional containing the location, or empty if no location found
     */
    public Optional<Location> getLocationOptional() {
        return Optional.ofNullable(getLocation());
    }

    /**
     * Checks if this event has an associated location.
     * 
     * @return true if a location is available, false otherwise
     */
    public boolean hasLocation() {
        return getLocation() != null;
    }

    // === EVENT CANCELLATION ===

    /**
     * Checks if the wrapped event implements Cancellable.
     * 
     * @return true if the event can be cancelled, false otherwise
     */
    public boolean isCancellable() {
        return event instanceof org.bukkit.event.Cancellable;
    }

    /**
     * Checks if the event is currently cancelled.
     * For non-cancellable events, always returns false.
     * 
     * @return true if the event is cancelled, false otherwise
     */
    public boolean isCancelled() {
        if (isCancellable()) {
            return ((org.bukkit.event.Cancellable) event).isCancelled();
        }
        return false;
    }

    /**
     * Sets the cancelled state of the event.
     * Has no effect on non-cancellable events.
     * 
     * @param cancelled true to cancel the event, false to uncancel
     */
    public void setCancelled(boolean cancelled) {
        if (isCancellable()) {
            ((org.bukkit.event.Cancellable) event).setCancelled(cancelled);
        }
    }

    /**
     * Cancels the event (sets cancelled to true).
     * Has no effect on non-cancellable events.
     */
    public void cancel() {
        setCancelled(true);
    }

    /**
     * Uncancels the event (sets cancelled to false).
     * Has no effect on non-cancellable events.
     */
    public void uncancel() {
        setCancelled(false);
    }

    // === METADATA MANAGEMENT ===

    /**
     * Stores custom metadata with this context.
     * 
     * @param key the metadata key
     * @param value the metadata value
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Retrieves typed metadata from this context.
     * 
     * @param <V> the expected value type
     * @param key the metadata key
     * @param type the expected value class
     * @return the metadata value cast to the specified type, or null if not found or wrong type
     */
    public <V> V getMetadata(String key, Class<V> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * Retrieves raw metadata from this context.
     * 
     * @param key the metadata key
     * @return the metadata value, or null if not found
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * Checks if metadata exists for the given key.
     * 
     * @param key the metadata key to check
     * @return true if metadata exists, false otherwise
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Removes metadata for the given key.
     * 
     * @param key the metadata key to remove
     */
    public void removeMetadata(String key) {
        metadata.remove(key);
    }

    /**
     * Clears all metadata from this context.
     */
    public void clearMetadata() {
        metadata.clear();
    }

    /**
     * Gets a copy of all metadata stored in this context.
     * 
     * @return a map containing all metadata key-value pairs
     */
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }

    // === TYPE CHECKING UTILITIES ===

    /**
     * Convenience method to check if this event involves a player.
     * 
     * @return true if a player is involved, false otherwise
     */
    public boolean isPlayerEvent() {
        return getPlayer() != null;
    }

    /**
     * Convenience method to check if this event involves an entity.
     * 
     * @return true if an entity is involved, false otherwise
     */
    public boolean isEntityEvent() {
        return getEntity() != null;
    }

    /**
     * Convenience method to check if this event involves a block.
     * 
     * @return true if a block is involved, false otherwise
     */
    public boolean isBlockEvent() {
        return getBlock() != null;
    }

    /**
     * Convenience method to check if this is a world event.
     * 
     * @return true if this is a WorldEvent, false otherwise
     */
    public boolean isWorldEvent() {
        return event instanceof WorldEvent;
    }

    /**
     * Convenience method to check if this event involves an inventory.
     * 
     * @return true if an inventory is involved, false otherwise
     */
    public boolean isInventoryEvent() {
        return getInventory() != null;
    }

    /**
     * Checks if the wrapped event is of the specified type.
     * 
     * @param eventType the event type to check against
     * @return true if the event is of the specified type, false otherwise
     */
    public boolean isEventType(Class<? extends Event> eventType) {
        return eventType.isInstance(event);
    }

    /**
     * Checks if the wrapped event is any of the specified types.
     * 
     * @param eventTypes the event types to check against
     * @return true if the event is any of the specified types, false otherwise
     */
    @SafeVarargs
    public final boolean isAnyEventType(Class<? extends Event>... eventTypes) {
        for (Class<? extends Event> type : eventTypes) {
            if (type.isInstance(event)) {
                return true;
            }
        }
        return false;
    }

    // === REFLECTION-BASED PROPERTY ACCESS ===

    /**
     * Attempts to get a property from the event using reflection.
     * Tries both "get" and "is" prefixes for the property name.
     * 
     * @param propertyName the property name (without get/is prefix)
     * @return the property value, or null if not found
     */
    public Object getProperty(String propertyName) {
        try {
            Method method = event.getClass().getMethod("get" + capitalize(propertyName));
            return method.invoke(event);
        } catch (Exception e) {
            try {
                Method method = event.getClass().getMethod("is" + capitalize(propertyName));
                return method.invoke(event);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Attempts to get a typed property from the event using reflection.
     * 
     * @param <V> the expected property type
     * @param propertyName the property name (without get/is prefix)
     * @param type the expected property class
     * @return the property value cast to the specified type, or null if not found or wrong type
     */
    public <V> V getProperty(String propertyName, Class<V> type) {
        Object value = getProperty(propertyName);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * Checks if the event has the specified property.
     * 
     * @param propertyName the property name to check
     * @return true if the property exists, false otherwise
     */
    public boolean hasProperty(String propertyName) {
        return getProperty(propertyName) != null;
    }

    /**
     * Capitalizes the first letter of a string.
     * 
     * @param str the string to capitalize
     * @return the capitalized string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // === DEBUG AND UTILITY ===

    /**
     * Prints comprehensive debug information about this context to the console.
     * Useful for development and troubleshooting.
     */
    public void printDebugInfo() {
        System.out.println("=== Event Context Debug ===");
        System.out.println("Event: " + getEventName());
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Player: " + getPlayer());
        System.out.println("Entity: " + getEntity());
        System.out.println("Block: " + getBlock());
        System.out.println("World: " + getWorld());
        System.out.println("Location: " + getLocation());
        System.out.println("Cancellable: " + isCancellable());
        System.out.println("Cancelled: " + isCancelled());
        System.out.println("Metadata: " + metadata);
    }

    /**
     * Returns a string representation of this context.
     * 
     * @return a string containing basic context information
     */
    @Override
    public String toString() {
        return String.format("EventContext{event=%s, player=%s, timestamp=%d}",
                getEventName(), getPlayer(), timestamp);
    }
}
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

public class EventContext<T extends Event> {
    private final T event;
    private final Map<String, Object> metadata = new HashMap<>();
    private final long timestamp = System.currentTimeMillis();
    
    public EventContext(T event) {
        this.event = event;
    }
        
    public T getEvent() { 
        return event; 
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Class<? extends Event> getEventType() {
        return event.getClass();
    }
    
    public String getEventName() {
        return event.getClass().getSimpleName();
    }
        
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
        } catch (Exception ignored) {}
        
        try {
            Method getEntityMethod = event.getClass().getMethod("getEntity");
            Object result = getEntityMethod.invoke(event);
            if (result instanceof Player) {
                return (Player) result;
            }
        } catch (Exception ignored) {}
        
        return null;
    }
    
    public Optional<Player> getPlayerOptional() {
        return Optional.ofNullable(getPlayer());
    }
    
    public boolean hasPlayer() {
        return getPlayer() != null;
    }
        
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
        } catch (Exception ignored) {}
        
        return null;
    }
    
    public Optional<Entity> getEntityOptional() {
        return Optional.ofNullable(getEntity());
    }
    
    public boolean hasEntity() {
        return getEntity() != null;
    }
        
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
        } catch (Exception ignored) {}
        
        return null;
    }
    
    public Optional<Block> getBlockOptional() {
        return Optional.ofNullable(getBlock());
    }
    
    public boolean hasBlock() {
        return getBlock() != null;
    }
        
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
        } catch (Exception ignored) {}
        
        return null;
    }
    
    public Optional<Inventory> getInventoryOptional() {
        return Optional.ofNullable(getInventory());
    }
    
    public boolean hasInventory() {
        return getInventory() != null;
    }
        
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
        } catch (Exception ignored) {}
        
        return null;
    }
    
    public Optional<World> getWorldOptional() {
        return Optional.ofNullable(getWorld());
    }
    
    public boolean hasWorld() {
        return getWorld() != null;
    }
        
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
        } catch (Exception ignored) {}
        
        return null;
    }
    
    public Optional<Location> getLocationOptional() {
        return Optional.ofNullable(getLocation());
    }
    
    public boolean hasLocation() {
        return getLocation() != null;
    }
        
    public boolean isCancellable() {
        return event instanceof org.bukkit.event.Cancellable;
    }
    
    public boolean isCancelled() {
        if (isCancellable()) {
            return ((org.bukkit.event.Cancellable) event).isCancelled();
        }
        return false;
    }
    
    public void setCancelled(boolean cancelled) {
        if (isCancellable()) {
            ((org.bukkit.event.Cancellable) event).setCancelled(cancelled);
        }
    }
    
    public void cancel() {
        setCancelled(true);
    }
    
    public void uncancel() {
        setCancelled(false);
    }
        
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public <V> V getMetadata(String key, Class<V> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    public void removeMetadata(String key) {
        metadata.remove(key);
    }
    
    public void clearMetadata() {
        metadata.clear();
    }
    
    public Map<String, Object> getAllMetadata() {
        return new HashMap<>(metadata);
    }
        
    public boolean isPlayerEvent() {
        return getPlayer() != null;
    }
    
    public boolean isEntityEvent() {
        return getEntity() != null;
    }
    
    public boolean isBlockEvent() {
        return getBlock() != null;
    }
    
    public boolean isWorldEvent() {
        return event instanceof WorldEvent;
    }
    
    public boolean isInventoryEvent() {
        return getInventory() != null;
    }
        
    public boolean isEventType(Class<? extends Event> eventType) {
        return eventType.isInstance(event);
    }
    
    @SafeVarargs
    public final boolean isAnyEventType(Class<? extends Event>... eventTypes) {
        for (Class<? extends Event> type : eventTypes) {
            if (type.isInstance(event)) {
                return true;
            }
        }
        return false;
    }
        
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
    
    public <V> V getProperty(String propertyName, Class<V> type) {
        Object value = getProperty(propertyName);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    
    public boolean hasProperty(String propertyName) {
        return getProperty(propertyName) != null;
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
        
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
    
    @Override
    public String toString() {
        return String.format("EventContext{event=%s, player=%s, timestamp=%d}", 
            getEventName(), getPlayer(), timestamp);
    }
}
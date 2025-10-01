package dev.adam.events;

import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class Events {

    private static Plugin plugin;
    private static final List<RegisteredListener<?>> listeners = new ArrayList<>();

    public static void init(Plugin pl) {
        plugin = pl;
    }

    // ---------------- Generic register methods ----------------

    public static <T extends Event> void register(Class<T> clazz, EventListener<T> listener) {
        register(clazz, listener, e -> true, EventPriority.NORMAL, false, false);
    }

    public static <T extends Event> void register(Class<T> clazz, EventListener<T> listener,
                                                  Predicate<T> filter) {
        register(clazz, listener, filter, EventPriority.NORMAL, false, false);
    }

    public static <T extends Event> void register(Class<T> clazz, EventListener<T> listener,
                                                  Predicate<T> filter, EventPriority priority,
                                                  boolean ignoreCancelled, boolean async) {
        if (plugin == null) throw new IllegalStateException("Events.init(plugin) must be called first!");

        RegisteredListener<T> rl = new RegisteredListener<>(clazz, listener, filter, priority, ignoreCancelled, async);
        listeners.add(rl);
        Bukkit.getPluginManager().registerEvents(rl, plugin);
    }

    // ---------------- Unregister methods ----------------

    public static void unregisterAll() {
        listeners.forEach(RegisteredListener::unregister);
        listeners.clear();
    }

    public static <T extends Event> void unregister(EventListener<T> listener) {
        listeners.removeIf(rl -> {
            if (rl.listener.equals(listener)) {
                rl.unregister();
                return true;
            }
            return false;
        });
    }

    // ---------------- Functional interface ----------------

    @FunctionalInterface
    public interface EventListener<T extends Event> {
        void handle(T event);
    }

    // ---------------- Internal RegisteredListener wrapper ----------------

    private static class RegisteredListener<T extends Event> implements Listener {
        private final Class<T> clazz;
        private final EventListener<T> listener;
        private final Predicate<T> filter;
        private final EventPriority priority;
        private final boolean ignoreCancelled;
        private final boolean async;

        public RegisteredListener(Class<T> clazz, EventListener<T> listener,
                                  Predicate<T> filter, EventPriority priority,
                                  boolean ignoreCancelled, boolean async) {
            this.clazz = clazz;
            this.listener = listener;
            this.filter = filter;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
            this.async = async;
        }

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
        public void onEvent(Event event) {
            if (!clazz.isInstance(event)) return;

            T casted = clazz.cast(event);

            if (ignoreCancelled && casted instanceof Cancellable cancellable && cancellable.isCancelled()) return;
            if (!filter.test(casted)) return;

            if (async) {
                CompletableFuture.runAsync(() -> safeHandle(casted));
            } else {
                safeHandle(casted);
            }
        }

        private void safeHandle(T event) {
            try {
                listener.handle(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void unregister() {
            HandlerList.unregisterAll(this);
        }
    }

    // ---------------- Convenience methods for common events ----------------

    public static void onJoin(EventListener<PlayerJoinEvent> listener) {
        register(PlayerJoinEvent.class, listener);
    }

    public static void onQuit(EventListener<PlayerQuitEvent> listener) {
        register(PlayerQuitEvent.class, listener);
    }

    public static void onChat(EventListener<AsyncPlayerChatEvent> listener, boolean async) {
        register(AsyncPlayerChatEvent.class, listener, e -> true, EventPriority.NORMAL, false, async);
    }

    public static void onMove(EventListener<PlayerMoveEvent> listener) {
        register(PlayerMoveEvent.class, listener);
    }

    public static void onTeleport(EventListener<PlayerTeleportEvent> listener) {
        register(PlayerTeleportEvent.class, listener);
    }

    public static void onRespawn(EventListener<PlayerRespawnEvent> listener) {
        register(PlayerRespawnEvent.class, listener);
    }

    public static void onInteract(EventListener<PlayerInteractEvent> listener) {
        register(PlayerInteractEvent.class, listener);
    }

    public static void onDamage(EventListener<EntityDamageEvent> listener) {
        register(EntityDamageEvent.class, listener);
    }

    public static void onDeath(EventListener<PlayerDeathEvent> listener) {
        register(PlayerDeathEvent.class, listener);
    }

    public static void onFoodLevelChange(EventListener<FoodLevelChangeEvent> listener) {
        register(FoodLevelChangeEvent.class, listener);
    }

    public static void onBlockBreak(EventListener<BlockBreakEvent> listener) {
        register(BlockBreakEvent.class, listener);
    }

    public static void onBlockPlace(EventListener<BlockPlaceEvent> listener) {
        register(BlockPlaceEvent.class, listener);
    }

    public static void onBlockDamage(EventListener<BlockDamageEvent> listener) {
        register(BlockDamageEvent.class, listener);
    }

    public static void onInventoryClick(EventListener<InventoryClickEvent> listener) {
        register(InventoryClickEvent.class, listener);
    }

    public static void onInventoryClose(EventListener<InventoryCloseEvent> listener) {
        register(InventoryCloseEvent.class, listener);
    }

    public static void onVehicleEnter(EventListener<VehicleEnterEvent> listener) {
        register(VehicleEnterEvent.class, listener);
    }

    public static void onVehicleExit(EventListener<VehicleExitEvent> listener) {
        register(VehicleExitEvent.class, listener);
    }

    public static void onWeatherChange(EventListener<WeatherChangeEvent> listener) {
        register(WeatherChangeEvent.class, listener);
    }

    public static void onPickupItem(EventListener<PlayerPickupItemEvent> listener) {
        register(PlayerPickupItemEvent.class, listener);
    }

    public static void onDropItem(EventListener<PlayerDropItemEvent> listener) {
        register(PlayerDropItemEvent.class, listener);
    }

    public static void onSneak(EventListener<PlayerToggleSneakEvent> listener) {
        register(PlayerToggleSneakEvent.class, listener);
    }

    public static void onSprint(EventListener<PlayerToggleSprintEvent> listener) {
        register(PlayerToggleSprintEvent.class, listener);
    }

    public static void onPortal(EventListener<PlayerPortalEvent> listener) {
        register(PlayerPortalEvent.class, listener);
    }

    public static void onCommandPreprocess(EventListener<PlayerCommandPreprocessEvent> listener) {
        register(PlayerCommandPreprocessEvent.class, listener);
    }

    public static void onItemConsume(EventListener<PlayerItemConsumeEvent> listener) {
        register(PlayerItemConsumeEvent.class, listener);
    }

    public static void onItemHeldChange(EventListener<PlayerItemHeldEvent> listener) {
        register(PlayerItemHeldEvent.class, listener);
    }

    public static void onInteractEntity(EventListener<PlayerInteractEntityEvent> listener) {
        register(PlayerInteractEntityEvent.class, listener);
    }
}

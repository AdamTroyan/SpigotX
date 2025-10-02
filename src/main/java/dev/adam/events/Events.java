package dev.adam.events;

import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

        Bukkit.getPluginManager().registerEvent(
                clazz,
                rl,
                priority,
                (l, e) -> {
                    if (l instanceof RegisteredListener<?> registeredListener) {
                        registeredListener.executeEvent(e);
                    }
                },
                plugin,
                ignoreCancelled
        );
    }

    // ---------------- Unregister methods ----------------

    public static void unregisterAll() {
        for (RegisteredListener<?> rl : listeners) {
            rl.unregister();
        }
        listeners.clear();
    }

    public static <T extends Event> void unregister(EventListener<T> listener) {
        Iterator<RegisteredListener<?>> it = listeners.iterator();
        while (it.hasNext()) {
            RegisteredListener<?> rl = it.next();
            if (rl.listenerEquals(listener)) {
                rl.unregister();
                it.remove();
            }
        }
    }

    // ---------------- Internal RegisteredListener wrapper ----------------

    private static class RegisteredListener<T extends Event> implements Listener {
        private final Class<T> clazz;
        private final dev.adam.events.EventListener<T> listener;
        private final Predicate<T> filter;
        private final EventPriority priority;
        private final boolean ignoreCancelled;
        private final boolean async;

        public RegisteredListener(Class<T> clazz, dev.adam.events.EventListener<T> listener,
                                  Predicate<T> filter, EventPriority priority,
                                  boolean ignoreCancelled, boolean async) {
            this.clazz = clazz;
            this.listener = listener;
            this.filter = filter;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
            this.async = async;
        }

        public void executeEvent(Event event) {
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
                listener.handle(new EventContext<>(event));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        public void unregister() {
            try {
                HandlerList handlerList = (HandlerList) clazz.getMethod("getHandlerList").invoke(null);
                if (handlerList != null) {
                    handlerList.unregister(this);
                }
            } catch (Exception ignored) {

            }
        }

        public boolean listenerEquals(dev.adam.events.EventListener<?> other) {
            return listener.equals(other);
        }
    }

    // ---------------- Convenience methods for common events ----------------

    public static <T extends org.bukkit.event.Event> void onEvent(dev.adam.events.EventListener<T> listener, Class<T> clazz) {
        dev.adam.events.Events.register(clazz, listener);
    }

    public static void onJoin(dev.adam.events.EventListener<PlayerJoinEvent> listener) {
        register(PlayerJoinEvent.class, listener);
    }

    public static void onQuit(dev.adam.events.EventListener<PlayerQuitEvent> listener) {
        register(PlayerQuitEvent.class, listener);
    }

    public static void onChat(dev.adam.events.EventListener<AsyncPlayerChatEvent> listener, boolean async) {
        register(AsyncPlayerChatEvent.class, listener, e -> true, EventPriority.NORMAL, false, async);
    }

    public static void onMove(dev.adam.events.EventListener<PlayerMoveEvent> listener) {
        register(PlayerMoveEvent.class, listener);
    }

    public static void onTeleport(dev.adam.events.EventListener<PlayerTeleportEvent> listener) {
        register(PlayerTeleportEvent.class, listener);
    }

    public static void onRespawn(dev.adam.events.EventListener<PlayerRespawnEvent> listener) {
        register(PlayerRespawnEvent.class, listener);
    }

    public static void onInteract(dev.adam.events.EventListener<PlayerInteractEvent> listener) {
        register(PlayerInteractEvent.class, listener);
    }

    public static void onDamage(dev.adam.events.EventListener<EntityDamageEvent> listener) {
        register(EntityDamageEvent.class, listener);
    }

    public static void onDeath(dev.adam.events.EventListener<PlayerDeathEvent> listener) {
        register(PlayerDeathEvent.class, listener);
    }

    public static void onFoodLevelChange(dev.adam.events.EventListener<FoodLevelChangeEvent> listener) {
        register(FoodLevelChangeEvent.class, listener);
    }

    public static void onBlockBreak(dev.adam.events.EventListener<BlockBreakEvent> listener) {
        register(BlockBreakEvent.class, listener);
    }

    public static void onBlockPlace(dev.adam.events.EventListener<BlockPlaceEvent> listener) {
        register(BlockPlaceEvent.class, listener);
    }

    public static void onBlockDamage(dev.adam.events.EventListener<BlockDamageEvent> listener) {
        register(BlockDamageEvent.class, listener);
    }

    public static void onInventoryClick(dev.adam.events.EventListener<InventoryClickEvent> listener) {
        register(InventoryClickEvent.class, listener);
    }

    public static void onInventoryClose(dev.adam.events.EventListener<InventoryCloseEvent> listener) {
        register(InventoryCloseEvent.class, listener);
    }

    public static void onVehicleEnter(dev.adam.events.EventListener<VehicleEnterEvent> listener) {
        register(VehicleEnterEvent.class, listener);
    }

    public static void onVehicleExit(dev.adam.events.EventListener<VehicleExitEvent> listener) {
        register(VehicleExitEvent.class, listener);
    }

    public static void onWeatherChange(dev.adam.events.EventListener<WeatherChangeEvent> listener) {
        register(WeatherChangeEvent.class, listener);
    }

    public static void onPickupItem(dev.adam.events.EventListener<PlayerPickupItemEvent> listener) {
        register(PlayerPickupItemEvent.class, listener);
    }

    public static void onDropItem(dev.adam.events.EventListener<PlayerDropItemEvent> listener) {
        register(PlayerDropItemEvent.class, listener);
    }

    public static void onSneak(dev.adam.events.EventListener<PlayerToggleSneakEvent> listener) {
        register(PlayerToggleSneakEvent.class, listener);
    }

    public static void onSprint(dev.adam.events.EventListener<PlayerToggleSprintEvent> listener) {
        register(PlayerToggleSprintEvent.class, listener);
    }

    public static void onPortal(dev.adam.events.EventListener<PlayerPortalEvent> listener) {
        register(PlayerPortalEvent.class, listener);
    }

    public static void onCommandPreprocess(dev.adam.events.EventListener<PlayerCommandPreprocessEvent> listener) {
        register(PlayerCommandPreprocessEvent.class, listener);
    }

    public static void onItemConsume(dev.adam.events.EventListener<PlayerItemConsumeEvent> listener) {
        register(PlayerItemConsumeEvent.class, listener);
    }

    public static void onItemHeldChange(dev.adam.events.EventListener<PlayerItemHeldEvent> listener) {
        register(PlayerItemHeldEvent.class, listener);
    }

    public static void onInteractEntity(dev.adam.events.EventListener<PlayerInteractEntityEvent> listener) {
        register(PlayerInteractEntityEvent.class, listener);
    }
}
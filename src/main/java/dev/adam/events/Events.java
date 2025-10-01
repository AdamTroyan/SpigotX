package dev.adam.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Events {

    public static void onJoin(Consumer<Player> consumer) {
        register(PlayerJoinEvent.class, event -> consumer.accept(event.getPlayer()));
    }

    public static void onQuit(Consumer<Player> consumer) {
        register(PlayerQuitEvent.class, event -> consumer.accept(event.getPlayer()));
    }

    public static void onChat(BiConsumer<Player, String> consumer) {
        register(AsyncPlayerChatEvent.class, event -> consumer.accept(event.getPlayer(), event.getMessage()));
    }

    public static void onMove(BiConsumer<Player, org.bukkit.event.player.PlayerMoveEvent> consumer) {
        register(org.bukkit.event.player.PlayerMoveEvent.class, event -> consumer.accept(event.getPlayer(), event));
    }

    public static void onTeleport(BiConsumer<Player, PlayerTeleportEvent> consumer) {
        register(PlayerTeleportEvent.class, event -> consumer.accept(event.getPlayer(), event));
    }

    public static void onRespawn(Consumer<Player> consumer) {
        register(PlayerRespawnEvent.class, event -> consumer.accept(event.getPlayer()));
    }

    public static void onInteract(BiConsumer<Player, PlayerInteractEvent> consumer) {
        register(PlayerInteractEvent.class, event -> consumer.accept(event.getPlayer(), event));
    }

    public static void onDamage(BiConsumer<Player, EntityDamageEvent> consumer) {
        register(EntityDamageEvent.class, event -> {
            if(event.getEntity() instanceof Player player) {
                consumer.accept(player, event);
            }
        });
    }

    public static void onDeath(BiConsumer<Player, PlayerDeathEvent> consumer) {
        register(PlayerDeathEvent.class, event -> consumer.accept(event.getEntity(), event));
    }

    public static void onFoodLevelChange(BiConsumer<Player, FoodLevelChangeEvent> consumer) {
        register(FoodLevelChangeEvent.class, event -> {
            if(event.getEntity() instanceof Player player) {
                consumer.accept(player, event);
            }
        });
    }

    public static void onBlockBreak(BiConsumer<Player, BlockBreakEvent> consumer) {
        register(BlockBreakEvent.class, event -> consumer.accept(event.getPlayer(), event));
    }

    public static void onBlockPlace(BiConsumer<Player, BlockPlaceEvent> consumer) {
        register(BlockPlaceEvent.class, event -> consumer.accept(event.getPlayer(), event));
    }

    public static void onBlockDamage(BiConsumer<Player, BlockDamageEvent> consumer) {
        register(BlockDamageEvent.class, event -> consumer.accept(event.getPlayer(), event));
    }

    public static void onInventoryClick(BiConsumer<Player, InventoryClickEvent> consumer) {
        register(InventoryClickEvent.class, event -> {
            if(event.getWhoClicked() instanceof Player player) {
                consumer.accept(player, event);
            }
        });
    }

    public static void onInventoryClose(BiConsumer<Player, InventoryCloseEvent> consumer) {
        register(InventoryCloseEvent.class, event -> {
            if(event.getPlayer() instanceof Player player) {
                consumer.accept(player, event);
            }
        });
    }

    public static void onVehicleEnter(BiConsumer<Player, VehicleEnterEvent> consumer) {
        register(VehicleEnterEvent.class, event -> {
            if(event.getEntered() instanceof Player player) {
                consumer.accept(player, event);
            }
        });
    }

    public static void onVehicleExit(BiConsumer<Player, VehicleExitEvent> consumer) {
        register(VehicleExitEvent.class, event -> {
            if(event.getExited() instanceof Player player) {
                consumer.accept(player, event);
            }
        });
    }

    public static void onWeatherChange(Consumer<WeatherChangeEvent> consumer) {
        register(WeatherChangeEvent.class, consumer::accept);
    }

    private static <T extends Event> void register(Class<T> eventClass, Consumer<T> consumer) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEvent(Event event) {
                if(eventClass.isInstance(event)) {
                    consumer.accept(eventClass.cast(event));
                }
            }
        }, Bukkit.getPluginManager().getPlugins()[0]);
    }
}

package dev.adam.events;

import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Utility class for simplified event handling in Bukkit plugins.
 * 
 * This class provides a fluent API for registering event listeners with various
 * conditions and behaviors, eliminating the need for traditional listener classes
 * in many cases. It also includes tracking and cleanup functionality.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Simple event registration with lambda handlers</li>
 *   <li>Conditional event listeners</li>
 *   <li>Limited execution listeners (once, times, timeout)</li>
 *   <li>Automatic listener tracking and cleanup</li>
 *   <li>Plugin-based listener management</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Listen to player join events
 * EventUtil.listen(plugin, PlayerJoinEvent.class, event -> {
 *     event.getPlayer().sendMessage("Welcome!");
 * });
 * 
 * // Listen only once
 * EventUtil.listenOnce(plugin, ServerLoadEvent.class, event -> {
 *     plugin.getLogger().info("Server loaded!");
 * });
 * 
 * // Conditional listening
 * EventUtil.listenIf(plugin, PlayerChatEvent.class, 
 *     event -> event.getPlayer().sendMessage("Nice message!"),
 *     event -> event.getMessage().contains("hello")
 * );
 * }</pre>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class EventUtil {
    
    /** Map tracking listeners per plugin for cleanup purposes */
    private static final Map<Plugin, Set<Listener>> pluginListeners = new ConcurrentHashMap<>();
    
    /** Map tracking scheduled tasks for timeout listeners */
    private static final Map<Listener, BukkitTask> timeoutTasks = new ConcurrentHashMap<>();

    // === BASIC EVENT REGISTRATION ===

    /**
     * Registers an event listener with normal priority.
     * 
     * @param <T> the event type
     * @param plugin the plugin registering the listener
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @return the registered Listener instance, or null if parameters are invalid
     */
    public static <T extends Event> Listener listen(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        return listen(plugin, eventClass, handler, EventPriority.NORMAL);
    }

    /**
     * Registers an event listener with specified priority.
     * 
     * @param <T> the event type
     * @param plugin the plugin registering the listener
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @param priority the event priority
     * @return the registered Listener instance, or null if parameters are invalid
     */
    public static <T extends Event> Listener listen(Plugin plugin, Class<T> eventClass, Consumer<T> handler, EventPriority priority) {
        if (plugin == null || eventClass == null || handler == null) {
            return null;
        }

        Listener listener = new Listener() {};

        Bukkit.getPluginManager().registerEvent(
                eventClass,
                listener,
                priority,
                (l, event) -> {
                    if (eventClass.isInstance(event)) {
                        try {
                            handler.accept(eventClass.cast(event));
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error handling event " + eventClass.getSimpleName() + ": " + e.getMessage());
                        }
                    }
                },
                plugin
        );

        trackListener(plugin, listener);
        return listener;
    }

    // === CONDITIONAL EVENT REGISTRATION ===

    /**
     * Registers an event listener that only executes if a condition is met.
     * 
     * @param <T> the event type
     * @param plugin the plugin registering the listener
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @param condition the condition that must be true for the handler to execute
     * @return the registered Listener instance
     */
    public static <T extends Event> Listener listenIf(Plugin plugin, Class<T> eventClass, Consumer<T> handler, Predicate<T> condition) {
        return listen(plugin, eventClass, event -> {
            if (condition.test(event)) {
                handler.accept(event);
            }
        });
    }

    /**
     * Registers an event listener that ignores cancelled events.
     * Only executes the handler if the event is not cancelled (for Cancellable events).
     * 
     * @param <T> the event type
     * @param plugin the plugin registering the listener
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @return the registered Listener instance
     */
    public static <T extends Event> Listener listenIgnoreCancelled(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        return listen(plugin, eventClass, event -> {
            if (event instanceof Cancellable) {
                Cancellable cancellable = (Cancellable) event;
                if (!cancellable.isCancelled()) {
                    handler.accept(event);
                }
            } else {
                handler.accept(event);
            }
        });
    }

    // === LIMITED EXECUTION LISTENERS ===

    /**
     * Registers an event listener that executes only once, then unregisters itself.
     * 
     * @param <T> the event type
     * @param plugin the plugin registering the listener
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @return the registered Listener instance
     */
    public static <T extends Event> Listener listenOnce(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        return new OnceListener<>(plugin, eventClass, handler).register();
    }

    /**
     * Registers an event listener that executes a maximum number of times.
     * 
     * @param <T> the event type
     * @param plugin the plugin registering the listener
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @param maxTimes the maximum number of times to execute
     * @return the registered Listener instance, or null if maxTimes is small or equal 0
     */
    public static <T extends Event> Listener listenTimes(Plugin plugin, Class<T> eventClass, Consumer<T> handler, int maxTimes) {
        if (maxTimes <= 0) {
            return null;
        }
        return new TimesListener<>(plugin, eventClass, handler, maxTimes).register();
    }

    /**
     * Registers an event listener that automatically unregisters after a timeout.
     * 
     * @param <T> the event type
     * @param plugin the plugin registering the listener
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @param timeoutTicks the timeout in ticks after which to unregister
     * @return the registered Listener instance, or null if timeoutTicks is small or equal 0
     */
    public static <T extends Event> Listener listenTimeout(Plugin plugin, Class<T> eventClass, Consumer<T> handler, long timeoutTicks) {
        if (timeoutTicks <= 0) {
            return null;
        }
        return new TimeoutListener<>(plugin, eventClass, handler, timeoutTicks).register();
    }

    // === SPECIALIZED LISTENER CLASSES ===

    /**
     * Internal listener implementation for one-time event handling.
     */
    private static class OnceListener<T extends Event> {
        private final Plugin plugin;
        private final Class<T> eventClass;
        private final Consumer<T> handler;
        private Listener listener;

        public OnceListener(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
            this.plugin = plugin;
            this.eventClass = eventClass;
            this.handler = handler;
        }

        public Listener register() {
            listener = new Listener() {};

            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    EventPriority.NORMAL,
                    (l, event) -> {
                        if (eventClass.isInstance(event)) {
                            try {
                                handler.accept(eventClass.cast(event));
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error in once listener: " + e.getMessage());
                            } finally {
                                unregisterListener(listener);
                            }
                        }
                    },
                    plugin
            );

            trackListener(plugin, listener);
            return listener;
        }
    }

    /**
     * Internal listener implementation for limited execution count.
     */
    private static class TimesListener<T extends Event> {
        private final Plugin plugin;
        private final Class<T> eventClass;
        private final Consumer<T> handler;
        private final int maxTimes;
        private int currentCount = 0;
        private Listener listener;

        public TimesListener(Plugin plugin, Class<T> eventClass, Consumer<T> handler, int maxTimes) {
            this.plugin = plugin;
            this.eventClass = eventClass;
            this.handler = handler;
            this.maxTimes = maxTimes;
        }

        public Listener register() {
            listener = new Listener() {};

            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    EventPriority.NORMAL,
                    (l, event) -> {
                        if (eventClass.isInstance(event) && currentCount < maxTimes) {
                            try {
                                handler.accept(eventClass.cast(event));
                                currentCount++;
                                
                                if (currentCount >= maxTimes) {
                                    unregisterListener(listener);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error in times listener: " + e.getMessage());
                            }
                        }
                    },
                    plugin
            );

            trackListener(plugin, listener);
            return listener;
        }
    }

    /**
     * Internal listener implementation for timeout-based unregistration.
     */
    private static class TimeoutListener<T extends Event> {
        private final Plugin plugin;
        private final Class<T> eventClass;
        private final Consumer<T> handler;
        private final long timeoutTicks;
        private Listener listener;

        public TimeoutListener(Plugin plugin, Class<T> eventClass, Consumer<T> handler, long timeoutTicks) {
            this.plugin = plugin;
            this.eventClass = eventClass;
            this.handler = handler;
            this.timeoutTicks = timeoutTicks;
        }

        public Listener register() {
            listener = new Listener() {};

            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    EventPriority.NORMAL,
                    (l, event) -> {
                        if (eventClass.isInstance(event)) {
                            try {
                                handler.accept(eventClass.cast(event));
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error in timeout listener: " + e.getMessage());
                            }
                        }
                    },
                    plugin
            );

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                unregisterListener(listener);
            }, timeoutTicks);
            
            timeoutTasks.put(listener, task);
            trackListener(plugin, listener);
            return listener;
        }
    }

    // === LISTENER MANAGEMENT ===

    /**
     * Tracks a listener for a specific plugin.
     * 
     * @param plugin the plugin that owns the listener
     * @param listener the listener to track
     */
    private static void trackListener(Plugin plugin, Listener listener) {
        pluginListeners.computeIfAbsent(plugin, k -> ConcurrentHashMap.newKeySet()).add(listener);
    }

    /**
     * Unregisters a specific listener and cleans up associated resources.
     * 
     * @param listener the listener to unregister
     */
    public static void unregisterListener(Listener listener) {
        if (listener == null) return;

        HandlerList.unregisterAll(listener);

        BukkitTask task = timeoutTasks.remove(listener);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        for (Set<Listener> listeners : pluginListeners.values()) {
            listeners.remove(listener);
        }
    }

    /**
     * Unregisters all listeners for a specific plugin.
     * 
     * @param plugin the plugin whose listeners should be unregistered
     */
    public static void unregisterAllListeners(Plugin plugin) {
        Set<Listener> listeners = pluginListeners.get(plugin);
        if (listeners != null) {
            for (Listener listener : new HashSet<>(listeners)) {
                unregisterListener(listener);
            }
            listeners.clear();
        }
    }

    // === INFORMATION METHODS ===

    /**
     * Gets the number of listeners registered by a plugin.
     * 
     * @param plugin the plugin to check
     * @return the number of registered listeners
     */
    public static int getListenerCount(Plugin plugin) {
        Set<Listener> listeners = pluginListeners.get(plugin);
        return listeners != null ? listeners.size() : 0;
    }

    /**
     * Checks if a plugin has any registered listeners.
     * 
     * @param plugin the plugin to check
     * @return true if the plugin has registered listeners, false otherwise
     */
    public static boolean hasListeners(Plugin plugin) {
        return getListenerCount(plugin) > 0;
    }

    // === CLEANUP METHODS ===

    /**
     * Performs cleanup for a specific plugin.
     * This should be called when a plugin is disabled.
     * 
     * @param plugin the plugin to clean up
     */
    public static void cleanup(Plugin plugin) {
        unregisterAllListeners(plugin);
        pluginListeners.remove(plugin);
    }

    /**
     * Performs complete cleanup of all tracked listeners and resources.
     * This should only be called when shutting down the entire system.
     */
    public static void cleanupAll() {
        for (BukkitTask task : timeoutTasks.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        
        timeoutTasks.clear();
        pluginListeners.clear();
    }
}
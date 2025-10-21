package dev.adam.events;

import dev.adam.SpigotX;
import dev.adam.events.context.EventContext;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Advanced event management system for Bukkit plugins.
 * 
 * This class provides a comprehensive event handling framework with advanced features
 * like named handlers, middleware support, conditional registration, and performance
 * metrics. It extends beyond basic event handling to offer enterprise-grade event
 * management capabilities.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Named event handlers for easy reference and management</li>
 *   <li>Event middleware system for preprocessing and postprocessing</li>
 *   <li>Conditional event registration with filters</li>
 *   <li>Performance metrics and monitoring</li>
 *   <li>Plugin-scoped handler management</li>
 *   <li>Specialized registration methods (player-only, conditional)</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Register a named handler
 * EventManager.registerNamed("welcome-handler", PlayerJoinEvent.class, ctx -> {
 *     ctx.getPlayer().sendMessage("Welcome!");
 * });
 * 
 * // Register with conditions
 * EventManager.registerConditional(PlayerChatEvent.class, ctx -> {
 *     ctx.getEvent().setMessage("Filtered: " + ctx.getEvent().getMessage());
 * }, ctx -> ctx.getPlayer().hasPermission("chat.filter"));
 * 
 * // Add middleware
 * EventManager.addMiddleware(new EventManager.EventMiddleware() {
 *     public boolean beforeHandle(EventContext<?> context) {
 *         // Preprocessing logic
 *         return true; // Continue processing
 *     }
 * });
 * }</pre>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class EventManager {
    
    private static final Map<String, RegisteredEventHandler> namedHandlers = new ConcurrentHashMap<>();
    private static final Map<Plugin, Set<RegisteredEventHandler>> pluginHandlers = new ConcurrentHashMap<>();
    private static final List<EventMiddleware> middlewares = new ArrayList<>();
    private static final Map<Class<? extends Event>, EventMetrics> eventMetrics = new ConcurrentHashMap<>();

    // === BASIC EVENT REGISTRATION ===

    /**
     * Registers an event handler with full configuration options.
     * 
     * @param <T> the event type
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @param filter the filter predicate to test before handling
     * @param priority the event priority
     * @param ignoreCancelled whether to ignore cancelled events
     * @return the registered handler instance, or null if parameters are invalid
     */
    public static <T extends Event> RegisteredEventHandler register(
            Class<T> eventClass,
            Consumer<EventContext<T>> handler,
            Predicate<EventContext<T>> filter,
            EventPriority priority,
            boolean ignoreCancelled
    ) {
        Plugin plugin = SpigotX.getPlugin();
        if (plugin == null || eventClass == null || handler == null || filter == null) return null;

        Listener listener = new Listener() {};

        @SuppressWarnings({"unchecked", "rawtypes"})
        RegisteredEventHandler registeredHandler = new RegisteredEventHandler(
                eventClass, listener, (Consumer) handler, (Predicate) filter, priority, ignoreCancelled
        );

        plugin.getServer().getPluginManager().registerEvent(
                eventClass,
                listener,
                priority,
                (l, event) -> {
                    if (eventClass.isInstance(event)) {
                        handleEvent(eventClass.cast(event), registeredHandler);
                    }
                },
                plugin,
                ignoreCancelled
        );

        trackHandler(plugin, registeredHandler);
        return registeredHandler;
    }

    // === NAMED HANDLER REGISTRATION ===

    /**
     * Registers a named event handler with default settings.
     * 
     * @param <T> the event type
     * @param name the unique name for this handler
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @return the registered handler instance
     */
    public static <T extends Event> RegisteredEventHandler registerNamed(
            String name,
            Class<T> eventClass,
            Consumer<EventContext<T>> handler
    ) {
        return registerNamed(name, eventClass, handler, ctx -> true, EventPriority.NORMAL, false);
    }

    /**
     * Registers a named event handler with full configuration.
     * If a handler with the same name exists, it will be replaced.
     * 
     * @param <T> the event type
     * @param name the unique name for this handler
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @param filter the filter predicate to test before handling
     * @param priority the event priority
     * @param ignoreCancelled whether to ignore cancelled events
     * @return the registered handler instance
     */
    public static <T extends Event> RegisteredEventHandler registerNamed(
            String name,
            Class<T> eventClass,
            Consumer<EventContext<T>> handler,
            Predicate<EventContext<T>> filter,
            EventPriority priority,
            boolean ignoreCancelled
    ) {
        unregisterNamed(name);

        RegisteredEventHandler registeredHandler = register(eventClass, handler, filter, priority, ignoreCancelled);
        if (registeredHandler != null) {
            registeredHandler.setName(name);
            namedHandlers.put(name, registeredHandler);
        }

        return registeredHandler;
    }

    /**
     * Unregisters a named event handler.
     * 
     * @param name the name of the handler to unregister
     * @return true if a handler was unregistered, false if no handler with that name existed
     */
    public static boolean unregisterNamed(String name) {
        RegisteredEventHandler handler = namedHandlers.remove(name);
        if (handler != null) {
            return unregister(handler);
        }
        return false;
    }

    /**
     * Checks if a named handler is registered and active.
     * 
     * @param name the handler name to check
     * @return true if the named handler exists and is active, false otherwise
     */
    public static boolean isNamedHandlerRegistered(String name) {
        RegisteredEventHandler handler = namedHandlers.get(name);
        return handler != null && handler.isActive();
    }

    // === SPECIALIZED REGISTRATION METHODS ===

    /**
     * Registers an event handler that only executes when a condition is met.
     * 
     * @param <T> the event type
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @param condition the condition that must be true for the handler to execute
     * @return the registered handler instance
     */
    public static <T extends Event> RegisteredEventHandler registerConditional(
            Class<T> eventClass,
            Consumer<EventContext<T>> handler,
            Predicate<EventContext<T>> condition
    ) {
        return register(eventClass, ctx -> {
            if (condition.test(ctx)) {
                handler.accept(ctx);
            }
        }, ctx -> true, EventPriority.NORMAL, false);
    }

    /**
     * Registers an event handler that only executes for events involving players.
     * 
     * @param <T> the event type
     * @param eventClass the event class to listen for
     * @param handler the handler function to execute
     * @return the registered handler instance
     */
    public static <T extends Event> RegisteredEventHandler registerPlayerOnly(
            Class<T> eventClass,
            Consumer<EventContext<T>> handler
    ) {
        return registerConditional(eventClass, handler, ctx -> ctx.getPlayer() != null);
    }

    // === EVENT PROCESSING ===

    /**
     * Internal method to process events through the middleware chain and handlers.
     */
    private static <T extends Event> void handleEvent(T event, RegisteredEventHandler handler) {
        try {
            long startTime = System.nanoTime();

            EventContext<T> ctx = new EventContext<>(event);

            for (EventMiddleware middleware : middlewares) {
                if (!middleware.beforeHandle(ctx)) {
                    return;
                }
            }

            if (handler.getFilter().test(ctx)) {
                handler.getHandler().accept(ctx);

                long duration = System.nanoTime() - startTime;
                updateMetrics(event.getClass(), duration, true);

                for (EventMiddleware middleware : middlewares) {
                    middleware.afterHandle(ctx);
                }
            }

        } catch (Exception e) {
            System.err.println("Error handling event " + event.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();

            updateMetrics(event.getClass(), 0, false);

            EventContext<T> ctx = new EventContext<>(event);
            for (EventMiddleware middleware : middlewares) {
                middleware.onError(ctx, e);
            }
        }
    }

    /**
     * Internal method to track handlers for cleanup purposes.
     */
    private static void trackHandler(Plugin plugin, RegisteredEventHandler handler) {
        pluginHandlers.computeIfAbsent(plugin, k -> ConcurrentHashMap.newKeySet()).add(handler);
    }

    // === HANDLER MANAGEMENT ===

    /**
     * Unregisters a specific event handler.
     * 
     * @param handler the handler to unregister
     * @return true if the handler was unregistered, false if it was already inactive
     */
    public static boolean unregister(RegisteredEventHandler handler) {
        if (handler != null && handler.isActive()) {
            HandlerList.unregisterAll(handler.getListener());
            handler.setActive(false);

            for (Set<RegisteredEventHandler> handlers : pluginHandlers.values()) {
                handlers.remove(handler);
            }

            namedHandlers.entrySet().removeIf(entry -> entry.getValue().equals(handler));

            return true;
        }
        return false;
    }

    /**
     * Unregisters all event handlers for a specific plugin.
     * 
     * @param plugin the plugin whose handlers should be unregistered
     */
    public static void unregisterAll(Plugin plugin) {
        Set<RegisteredEventHandler> handlers = pluginHandlers.get(plugin);
        if (handlers != null) {
            for (RegisteredEventHandler handler : new HashSet<>(handlers)) {
                unregister(handler);
            }
            handlers.clear();
        }
    }

    // === MIDDLEWARE MANAGEMENT ===

    /**
     * Adds an event middleware to the processing chain.
     * Middlewares are executed in the order they were added.
     * 
     * @param middleware the middleware to add
     */
    public static void addMiddleware(EventMiddleware middleware) {
        if (middleware != null && !middlewares.contains(middleware)) {
            middlewares.add(middleware);
        }
    }

    /**
     * Removes an event middleware from the processing chain.
     * 
     * @param middleware the middleware to remove
     */
    public static void removeMiddleware(EventMiddleware middleware) {
        middlewares.remove(middleware);
    }

    /**
     * Clears all registered middlewares.
     */
    public static void clearMiddlewares() {
        middlewares.clear();
    }

    /**
     * Interface for event middleware that can intercept and modify event processing.
     */
    public interface EventMiddleware {
        /**
         * Called before the event handler is executed.
         * 
         * @param context the event context
         * @return true to continue processing, false to stop
         */
        default boolean beforeHandle(EventContext<?> context) {
            return true;
        }

        /**
         * Called after the event handler is executed successfully.
         * 
         * @param context the event context
         */
        default void afterHandle(EventContext<?> context) {
        }

        /**
         * Called when an error occurs during event handling.
         * 
         * @param context the event context
         * @param error the exception that occurred
         */
        default void onError(EventContext<?> context, Exception error) {
        }
    }

    // === METRICS AND MONITORING ===

    /**
     * Internal method to update performance metrics for events.
     */
    private static void updateMetrics(Class<? extends Event> eventClass, long duration, boolean success) {
        eventMetrics.computeIfAbsent(eventClass, k -> new EventMetrics()).update(duration, success);
    }

    /**
     * Internal class for tracking event performance metrics.
     */
    private static class EventMetrics {
        private long totalCalls = 0;
        private long totalDuration = 0;
        private long successfulCalls = 0;
        private long failedCalls = 0;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = 0;

        public synchronized void update(long duration, boolean success) {
            totalCalls++;

            if (success) {
                successfulCalls++;
                totalDuration += duration;
                minDuration = Math.min(minDuration, duration);
                maxDuration = Math.max(maxDuration, duration);
            } else {
                failedCalls++;
            }
        }

        public double getAverageDurationMs() {
            return successfulCalls > 0 ? (totalDuration / 1_000_000.0) / successfulCalls : 0;
        }

        public double getSuccessRate() {
            return totalCalls > 0 ? (successfulCalls * 100.0) / totalCalls : 0;
        }
    }

    /**
     * Prints comprehensive statistics about event handling performance.
     */
    public static void printEventStatistics() {
        System.out.println("=== Event Manager Statistics ===");

        for (var entry : eventMetrics.entrySet()) {
            Class<? extends Event> eventClass = entry.getKey();
            EventMetrics metrics = entry.getValue();

            System.out.println(String.format("%s: %d calls, %.2f%% success, avg %.2fms",
                    eventClass.getSimpleName(),
                    metrics.totalCalls,
                    metrics.getSuccessRate(),
                    metrics.getAverageDurationMs()));
        }

        System.out.println("Total handlers: " + getTotalHandlerCount());
        System.out.println("Named handlers: " + namedHandlers.size());
        System.out.println("Active middlewares: " + middlewares.size());
    }

    // === INFORMATION METHODS ===

    /**
     * Gets the total number of registered handlers across all plugins.
     * 
     * @return the total handler count
     */
    public static int getTotalHandlerCount() {
        return pluginHandlers.values().stream()
                .mapToInt(Set::size)
                .sum();
    }

    /**
     * Gets the number of registered handlers for a specific plugin.
     * 
     * @param plugin the plugin to check
     * @return the handler count for the plugin
     */
    public static int getHandlerCount(Plugin plugin) {
        Set<RegisteredEventHandler> handlers = pluginHandlers.get(plugin);
        return handlers != null ? handlers.size() : 0;
    }

    /**
     * Gets a map of event classes to their total call counts.
     * 
     * @return a map of event call statistics
     */
    public static Map<Class<? extends Event>, Long> getEventCallCounts() {
        Map<Class<? extends Event>, Long> counts = new HashMap<>();
        for (var entry : eventMetrics.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().totalCalls);
        }
        return counts;
    }

    /**
     * Gets a list of all named handler names.
     * 
     * @return a list of named handler names
     */
    public static List<String> getNamedHandlerNames() {
        return new ArrayList<>(namedHandlers.keySet());
    }

    /**
     * Gets a named handler by its name.
     * 
     * @param name the handler name
     * @return the registered handler, or null if not found
     */
    public static RegisteredEventHandler getNamedHandler(String name) {
        return namedHandlers.get(name);
    }

    // === CLEANUP METHODS ===

    /**
     * Clears all performance metrics data.
     */
    public static void clearMetrics() {
        eventMetrics.clear();
    }

    /**
     * Performs cleanup for a specific plugin.
     * This should be called when a plugin is disabled.
     * 
     * @param plugin the plugin to clean up
     */
    public static void cleanup(Plugin plugin) {
        unregisterAll(plugin);
        pluginHandlers.remove(plugin);
    }

    /**
     * Performs complete cleanup of all data and handlers.
     * This should only be called when shutting down the entire system.
     */
    public static void cleanupAll() {
        pluginHandlers.clear();
        namedHandlers.clear();
        middlewares.clear();
        eventMetrics.clear();
    }

    // === REGISTERED EVENT HANDLER CLASS ===

    /**
     * Represents a registered event handler with all its configuration and metadata.
     */
    public static class RegisteredEventHandler {
        private final Class<? extends Event> eventClass;
        private final Listener listener;
        private final Consumer<EventContext<?>> handler;
        private final Predicate<EventContext<?>> filter;
        private final EventPriority priority;
        private final boolean ignoreCancelled;
        private String name;
        private boolean active = true;

        /**
         * Creates a new registered event handler.
         * 
         * @param eventClass the event class this handler listens to
         * @param listener the Bukkit listener instance
         * @param handler the handler function
         * @param filter the filter predicate
         * @param priority the event priority
         * @param ignoreCancelled whether to ignore cancelled events
         */
        @SuppressWarnings("unchecked")
        public RegisteredEventHandler(
                Class<? extends Event> eventClass,
                Listener listener,
                Consumer<EventContext<?>> handler,
                Predicate<EventContext<?>> filter,
                EventPriority priority,
                boolean ignoreCancelled
        ) {
            this.eventClass = eventClass;
            this.listener = listener;
            this.handler = handler;
            this.filter = filter;
            this.priority = priority;
            this.ignoreCancelled = ignoreCancelled;
        }

        /**
         * Gets the event class this handler listens to.
         * 
         * @return the event class
         */
        public Class<? extends Event> getEventClass() {
            return eventClass;
        }

        /**
         * Gets the Bukkit listener instance.
         * 
         * @return the listener
         */
        public Listener getListener() {
            return listener;
        }

        /**
         * Gets the handler function.
         * 
         * @return the handler consumer
         */
        public Consumer<EventContext<?>> getHandler() {
            return handler;
        }

        /**
         * Gets the filter predicate.
         * 
         * @return the filter predicate
         */
        public Predicate<EventContext<?>> getFilter() {
            return filter;
        }

        /**
         * Gets the event priority.
         * 
         * @return the priority
         */
        public EventPriority getPriority() {
            return priority;
        }

        /**
         * Checks if this handler ignores cancelled events.
         * 
         * @return true if cancelled events are ignored, false otherwise
         */
        public boolean isIgnoreCancelled() {
            return ignoreCancelled;
        }

        /**
         * Gets the name of this handler (if it's a named handler).
         * 
         * @return the handler name, or null if unnamed
         */
        public String getName() {
            return name;
        }

        /**
         * Checks if this handler is currently active.
         * 
         * @return true if active, false if unregistered
         */
        public boolean isActive() {
            return active;
        }

        /**
         * Sets the name for this handler.
         * 
         * @param name the handler name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Sets the active state of this handler.
         * 
         * @param active the active state
         */
        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
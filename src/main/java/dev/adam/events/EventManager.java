package dev.adam.events;

import dev.adam.SpigotX;
import dev.adam.events.context.EventContext;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EventManager {
    private static final Map<String, RegisteredEventHandler> namedHandlers = new ConcurrentHashMap<>();
    private static final Map<Plugin, Set<RegisteredEventHandler>> pluginHandlers = new ConcurrentHashMap<>();
    private static final List<EventMiddleware> middlewares = new ArrayList<>();
    
    private static final Map<Class<? extends Event>, EventMetrics> eventMetrics = new ConcurrentHashMap<>();
        
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
        
    public static <T extends Event> RegisteredEventHandler registerNamed(
            String name,
            Class<T> eventClass,
            Consumer<EventContext<T>> handler
    ) {
        return registerNamed(name, eventClass, handler, ctx -> true, EventPriority.NORMAL, false);
    }
    
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
    
    public static boolean unregisterNamed(String name) {
        RegisteredEventHandler handler = namedHandlers.remove(name);
        if (handler != null) {
            return unregister(handler);
        }
        return false;
    }
    
    public static boolean isNamedHandlerRegistered(String name) {
        RegisteredEventHandler handler = namedHandlers.get(name);
        return handler != null && handler.isActive();
    }
        
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
    
    public static <T extends Event> RegisteredEventHandler registerPlayerOnly(
            Class<T> eventClass,
            Consumer<EventContext<T>> handler
    ) {
        return registerConditional(eventClass, handler, ctx -> ctx.getPlayer() != null);
    }
        
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
    
    private static void trackHandler(Plugin plugin, RegisteredEventHandler handler) {
        pluginHandlers.computeIfAbsent(plugin, k -> ConcurrentHashMap.newKeySet()).add(handler);
    }
    
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
    
    public static void unregisterAll(Plugin plugin) {
        Set<RegisteredEventHandler> handlers = pluginHandlers.get(plugin);
        if (handlers != null) {
            for (RegisteredEventHandler handler : new HashSet<>(handlers)) {
                unregister(handler);
            }
            handlers.clear();
        }
    }
        
    public static void addMiddleware(EventMiddleware middleware) {
        if (middleware != null && !middlewares.contains(middleware)) {
            middlewares.add(middleware);
        }
    }
    
    public static void removeMiddleware(EventMiddleware middleware) {
        middlewares.remove(middleware);
    }
    
    public static void clearMiddlewares() {
        middlewares.clear();
    }
    
    public interface EventMiddleware {
        default boolean beforeHandle(EventContext<?> context) { return true; }
        default void afterHandle(EventContext<?> context) {}
        default void onError(EventContext<?> context, Exception error) {}
    }
        
    private static void updateMetrics(Class<? extends Event> eventClass, long duration, boolean success) {
        eventMetrics.computeIfAbsent(eventClass, k -> new EventMetrics()).update(duration, success);
    }
    
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
    
    public static int getTotalHandlerCount() {
        return pluginHandlers.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
    
    public static int getHandlerCount(Plugin plugin) {
        Set<RegisteredEventHandler> handlers = pluginHandlers.get(plugin);
        return handlers != null ? handlers.size() : 0;
    }
    
    public static Map<Class<? extends Event>, Long> getEventCallCounts() {
        Map<Class<? extends Event>, Long> counts = new HashMap<>();
        for (var entry : eventMetrics.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().totalCalls);
        }
        return counts;
    }
        
    public static class RegisteredEventHandler {
        private final Class<? extends Event> eventClass;
        private final Listener listener;
        private final Consumer<EventContext<?>> handler;
        private final Predicate<EventContext<?>> filter;
        private final EventPriority priority;
        private final boolean ignoreCancelled;
        private String name;
        private boolean active = true;
        
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
        
        public Class<? extends Event> getEventClass() { return eventClass; }
        public Listener getListener() { return listener; }
        public Consumer<EventContext<?>> getHandler() { return handler; }
        public Predicate<EventContext<?>> getFilter() { return filter; }
        public EventPriority getPriority() { return priority; }
        public boolean isIgnoreCancelled() { return ignoreCancelled; }
        public String getName() { return name; }
        public boolean isActive() { return active; }
        
        public void setName(String name) { this.name = name; }
        public void setActive(boolean active) { this.active = active; }
    }
        
    public static List<String> getNamedHandlerNames() {
        return new ArrayList<>(namedHandlers.keySet());
    }
    
    public static RegisteredEventHandler getNamedHandler(String name) {
        return namedHandlers.get(name);
    }
    
    public static void clearMetrics() {
        eventMetrics.clear();
    }
        
    public static void cleanup(Plugin plugin) {
        unregisterAll(plugin);
        pluginHandlers.remove(plugin);
    }
    
    public static void cleanupAll() {
        pluginHandlers.clear();
        namedHandlers.clear();
        middlewares.clear();
        eventMetrics.clear();
    }
}
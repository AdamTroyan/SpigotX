package dev.adam.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventUtil {
    private static final Map<Plugin, Set<Listener>> pluginListeners = new ConcurrentHashMap<>();
    private static final Map<Listener, Set<Class<? extends Event>>> listenerEvents = new ConcurrentHashMap<>();

    private static final Map<Class<? extends Event>, Integer> eventCounts = new ConcurrentHashMap<>();
    private static final Map<Class<? extends Event>, Long> eventTimes = new ConcurrentHashMap<>();

    public static <T extends Event> Listener listen(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        return listen(plugin, eventClass, handler, EventPriority.NORMAL);
    }

    public static <T extends Event> Listener listen(Plugin plugin, Class<T> eventClass, Consumer<T> handler, EventPriority priority) {
        if (plugin == null || eventClass == null || handler == null) return null;

        Listener listener = new Listener() {
        };

        Bukkit.getPluginManager().registerEvent(
                eventClass,
                listener,
                priority,
                (l, event) -> {
                    if (eventClass.isInstance(event)) {
                        try {
                            long startTime = System.nanoTime();
                            handler.accept(eventClass.cast(event));

                            long duration = System.nanoTime() - startTime;
                            eventCounts.merge(eventClass, 1, Integer::sum);
                            eventTimes.merge(eventClass, duration, Long::sum);

                        } catch (Exception e) {
                            System.err.println("Error handling event " + eventClass.getSimpleName() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                plugin
        );

        trackListener(plugin, listener, eventClass);

        return listener;
    }

    public static <T extends Event> Listener listenIf(Plugin plugin, Class<T> eventClass, Consumer<T> handler, java.util.function.Predicate<T> condition) {
        return listen(plugin, eventClass, event -> {
            if (condition.test(event)) {
                handler.accept(event);
            }
        });
    }

    public static <T extends Event> Listener listenOnce(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        return new OnceListener<>(plugin, eventClass, handler).register();
    }

    public static <T extends Event> Listener listenTimes(Plugin plugin, Class<T> eventClass, Consumer<T> handler, int maxTimes) {
        return new TimesListener<>(plugin, eventClass, handler, maxTimes).register();
    }

    public static <T extends Event> Listener listenTimeout(Plugin plugin, Class<T> eventClass, Consumer<T> handler, long timeoutTicks) {
        return new TimeoutListener<>(plugin, eventClass, handler, timeoutTicks).register();
    }

    @SafeVarargs
    public static Listener listenMultiple(Plugin plugin, Consumer<Event> handler, Class<? extends Event>... eventClasses) {
        Listener listener = new Listener() {
        };

        for (Class<? extends Event> eventClass : eventClasses) {
            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    EventPriority.NORMAL,
                    (l, event) -> {
                        if (eventClass.isInstance(event)) {
                            try {
                                handler.accept(event);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    plugin
            );
            trackListener(plugin, listener, eventClass);
        }

        return listener;
    }

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
            listener = new Listener() {
            };

            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    EventPriority.NORMAL,
                    (l, event) -> {
                        if (eventClass.isInstance(event)) {
                            try {
                                handler.accept(eventClass.cast(event));
                                unregisterListener(listener);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    plugin
            );

            trackListener(plugin, listener, eventClass);
            return listener;
        }
    }

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
            listener = new Listener() {
            };

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
                                e.printStackTrace();
                            }
                        }
                    },
                    plugin
            );

            trackListener(plugin, listener, eventClass);
            return listener;
        }
    }

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
            listener = new Listener() {
            };

            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    EventPriority.NORMAL,
                    (l, event) -> {
                        if (eventClass.isInstance(event)) {
                            try {
                                handler.accept(eventClass.cast(event));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    plugin
            );

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                unregisterListener(listener);
            }, timeoutTicks);

            trackListener(plugin, listener, eventClass);
            return listener;
        }
    }

    private static void trackListener(Plugin plugin, Listener listener, Class<? extends Event> eventClass) {
        pluginListeners.computeIfAbsent(plugin, k -> ConcurrentHashMap.newKeySet()).add(listener);
        listenerEvents.computeIfAbsent(listener, k -> ConcurrentHashMap.newKeySet()).add(eventClass);
    }

    public static void unregisterListener(Listener listener) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);

            for (Set<Listener> listeners : pluginListeners.values()) {
                listeners.remove(listener);
            }
            listenerEvents.remove(listener);
        }
    }

    public static void unregisterAllListeners(Plugin plugin) {
        Set<Listener> listeners = pluginListeners.get(plugin);
        if (listeners != null) {
            for (Listener listener : new HashSet<>(listeners)) {
                unregisterListener(listener);
            }
            listeners.clear();
        }
    }

    public static int getListenerCount(Plugin plugin) {
        Set<Listener> listeners = pluginListeners.get(plugin);
        return listeners != null ? listeners.size() : 0;
    }

    public static Set<Class<? extends Event>> getListenedEvents(Plugin plugin) {
        Set<Listener> listeners = pluginListeners.get(plugin);
        if (listeners == null) return Collections.emptySet();

        Set<Class<? extends Event>> events = new HashSet<>();
        for (Listener listener : listeners) {
            Set<Class<? extends Event>> listenerEvents = EventUtil.listenerEvents.get(listener);
            if (listenerEvents != null) {
                events.addAll(listenerEvents);
            }
        }
        return events;
    }

    public static void printEventStatistics() {
        System.out.println("=== Event Statistics ===");

        for (var entry : eventCounts.entrySet()) {
            Class<? extends Event> eventClass = entry.getKey();
            int count = entry.getValue();
            long totalTime = eventTimes.getOrDefault(eventClass, 0L);
            double avgTime = count > 0 ? (totalTime / 1_000_000.0) / count : 0;

            System.out.println(String.format("%s: %d calls, avg %.2fms",
                    eventClass.getSimpleName(), count, avgTime));
        }
    }

    public static Map<Class<? extends Event>, Integer> getEventCounts() {
        return new HashMap<>(eventCounts);
    }

    public static void clearStatistics() {
        eventCounts.clear();
        eventTimes.clear();
    }

    public static boolean isEventRegistered(Class<? extends Event> eventClass) {
        return eventCounts.containsKey(eventClass);
    }

    public static List<Plugin> getPluginsListeningTo(Class<? extends Event> eventClass) {
        List<Plugin> plugins = new ArrayList<>();

        for (var entry : pluginListeners.entrySet()) {
            Plugin plugin = entry.getKey();
            Set<Listener> listeners = entry.getValue();

            for (Listener listener : listeners) {
                Set<Class<? extends Event>> events = listenerEvents.get(listener);
                if (events != null && events.contains(eventClass)) {
                    plugins.add(plugin);
                    break;
                }
            }
        }

        return plugins;
    }

    public static void printListenerInfo() {
        System.out.println("=== Listener Information ===");

        for (var entry : pluginListeners.entrySet()) {
            Plugin plugin = entry.getKey();
            Set<Listener> listeners = entry.getValue();

            System.out.println(plugin.getName() + ": " + listeners.size() + " listeners");

            Set<Class<? extends Event>> events = getListenedEvents(plugin);
            for (Class<? extends Event> event : events) {
                System.out.println("  - " + event.getSimpleName());
            }
        }
    }

    public static <T extends Event> Listener listenIgnoreCancelled(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        return listen(plugin, eventClass, event -> {
            if (event instanceof org.bukkit.event.Cancellable) {
                org.bukkit.event.Cancellable cancellable = (org.bukkit.event.Cancellable) event;
                if (!cancellable.isCancelled()) {
                    handler.accept(event);
                }
            } else {
                handler.accept(event);
            }
        });
    }

    public static <T extends Event> Listener listenOnlyCancelled(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        return listen(plugin, eventClass, event -> {
            if (event instanceof org.bukkit.event.Cancellable) {
                org.bukkit.event.Cancellable cancellable = (org.bukkit.event.Cancellable) event;
                if (cancellable.isCancelled()) {
                    handler.accept(event);
                }
            }
        });
    }

    public static <T extends Event> Listener listenSafe(Plugin plugin, Class<T> eventClass, Consumer<T> handler, Consumer<Exception> errorHandler) {
        return listen(plugin, eventClass, event -> {
            try {
                handler.accept(event);
            } catch (Exception e) {
                if (errorHandler != null) {
                    errorHandler.accept(e);
                } else {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void cleanup(Plugin plugin) {
        unregisterAllListeners(plugin);
        pluginListeners.remove(plugin);
    }

    public static void cleanupAll() {
        pluginListeners.clear();
        listenerEvents.clear();
        clearStatistics();
    }
}
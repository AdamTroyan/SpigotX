package dev.adam.events;

import dev.adam.events.context.EventContext;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Cancellable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Fluent builder for creating event listeners with advanced configuration options.
 * 
 * This class provides a comprehensive builder pattern for registering event listeners
 * with various filters, conditions, and execution limits. It offers a more readable
 * and maintainable alternative to traditional event registration methods.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Fluent API for easy configuration</li>
 *   <li>Multiple filter types (context, event, player-based)</li>
 *   <li>Execution limits (once, max count, timeout)</li>
 *   <li>Priority shortcuts for common priorities</li>
 *   <li>Player-specific filters and handlers</li>
 *   <li>Automatic cleanup for limited execution handlers</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple event listener
 * EventBuilder.listen(PlayerJoinEvent.class)
 *     .handleEvent(event -> event.getPlayer().sendMessage("Welcome!"))
 *     .register();
 * 
 * // Advanced conditional listener
 * EventBuilder.listen(PlayerChatEvent.class)
 *     .playersOnly()
 *     .playerHasPermission("chat.filter")
 *     .onlyIfNotCancelled()
 *     .handle(ctx -> {
 *         String message = ctx.getEvent().getMessage();
 *         ctx.getEvent().setMessage("[FILTERED] " + message);
 *     })
 *     .high()
 *     .register();
 * 
 * // One-time listener with timeout
 * EventBuilder.listen(ServerLoadEvent.class)
 *     .once()
 *     .timeout(600) // 30 seconds
 *     .name("server-load-handler")
 *     .handleEvent(event -> plugin.onServerLoaded())
 *     .register();
 * }</pre>
 * 
 * @param <T> the event type this builder is configured for
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class EventBuilder<T extends Event> {
    private final Class<T> eventClass;
    private Consumer<EventContext<T>> handler;
    private final List<Predicate<EventContext<T>>> filters = new ArrayList<>();
    private EventPriority priority = EventPriority.NORMAL;
    private boolean ignoreCancelled = false;
    private String name;
    private long timeout = -1;
    private int maxExecutions = -1;
    private boolean onlyOnce = false;

    /**
     * Creates a new EventBuilder for the specified event class.
     * 
     * @param eventClass the event class to listen for
     */
    public EventBuilder(Class<T> eventClass) {
        this.eventClass = eventClass;
    }

    // === HANDLER CONFIGURATION ===

    /**
     * Sets the handler function that receives the full event context.
     * 
     * @param handler the handler function to execute
     * @return this builder for method chaining
     */
    public EventBuilder<T> handle(Consumer<EventContext<T>> handler) {
        this.handler = handler;
        return this;
    }

    /**
     * Sets the handler function that receives only the event object.
     * 
     * @param handler the handler function to execute
     * @return this builder for method chaining
     */
    public EventBuilder<T> handleEvent(Consumer<T> handler) {
        this.handler = ctx -> handler.accept(ctx.getEvent());
        return this;
    }

    /**
     * Sets the handler function that receives only the player (if available).
     * If the event doesn't involve a player, the handler won't be called.
     * 
     * @param handler the handler function to execute
     * @return this builder for method chaining
     */
    public EventBuilder<T> handlePlayer(Consumer<Player> handler) {
        this.handler = ctx -> {
            Player player = ctx.getPlayer();
            if (player != null) {
                handler.accept(player);
            }
        };
        return this;
    }

    // === FILTER CONFIGURATION ===

    /**
     * Adds a filter that tests the full event context.
     * 
     * @param filter the predicate to test before executing the handler
     * @return this builder for method chaining
     */
    public EventBuilder<T> filter(Predicate<EventContext<T>> filter) {
        if (filter != null) this.filters.add(filter);
        return this;
    }

    /**
     * Adds a filter that tests only the event object.
     * 
     * @param filter the predicate to test the event
     * @return this builder for method chaining
     */
    public EventBuilder<T> filterEvent(Predicate<T> filter) {
        if (filter != null) this.filters.add(ctx -> filter.test(ctx.getEvent()));
        return this;
    }

    /**
     * Adds a filter that tests only the player (if available).
     * If the event doesn't involve a player, the filter fails.
     * 
     * @param filter the predicate to test the player
     * @return this builder for method chaining
     */
    public EventBuilder<T> filterPlayer(Predicate<Player> filter) {
        if (filter != null) this.filters.add(ctx -> {
            Player player = ctx.getPlayer();
            return player != null && filter.test(player);
        });
        return this;
    }

    // === COMMON FILTERS ===

    /**
     * Filters to only handle events that involve players.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> playersOnly() {
        return filter(ctx -> ctx.getPlayer() != null);
    }

    /**
     * Filters to only handle events where the player has the specified permission.
     * 
     * @param permission the required permission
     * @return this builder for method chaining
     */
    public EventBuilder<T> playerHasPermission(String permission) {
        return filterPlayer(player -> player.hasPermission(permission));
    }

    /**
     * Filters to only handle events where the player is in the specified world.
     * 
     * @param worldName the name of the world
     * @return this builder for method chaining
     */
    public EventBuilder<T> playerInWorld(String worldName) {
        return filterPlayer(player -> player.getWorld().getName().equals(worldName));
    }

    /**
     * Filters to only handle events that are not cancelled.
     * For non-cancellable events, this filter always passes.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> onlyIfNotCancelled() {
        return filter(ctx -> {
            T event = ctx.getEvent();
            if (event instanceof Cancellable) {
                return !((Cancellable) event).isCancelled();
            }
            return true;
        });
    }

    /**
     * Filters to only handle events that are cancelled.
     * For non-cancellable events, this filter always fails.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> onlyIfCancelled() {
        return filter(ctx -> {
            T event = ctx.getEvent();
            if (event instanceof Cancellable) {
                return ((Cancellable) event).isCancelled();
            }
            return false;
        });
    }

    // === PRIORITY CONFIGURATION ===

    /**
     * Sets the event priority for this listener.
     * 
     * @param priority the event priority
     * @return this builder for method chaining
     */
    public EventBuilder<T> priority(EventPriority priority) {
        if (priority != null) this.priority = priority;
        return this;
    }

    /**
     * Sets whether to ignore cancelled events.
     * 
     * @param ignore true to ignore cancelled events, false otherwise
     * @return this builder for method chaining
     */
    public EventBuilder<T> ignoreCancelled(boolean ignore) {
        this.ignoreCancelled = ignore;
        return this;
    }

    /**
     * Sets the listener to ignore cancelled events.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> ignoreCancelled() {
        return ignoreCancelled(true);
    }

    // === NAMING AND EXECUTION LIMITS ===

    /**
     * Sets a name for this event handler (creates a named handler).
     * 
     * @param name the unique name for this handler
     * @return this builder for method chaining
     */
    public EventBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets a timeout for this listener (auto-unregisters after the specified time).
     * 
     * @param ticks the timeout in ticks
     * @return this builder for method chaining
     */
    public EventBuilder<T> timeout(long ticks) {
        this.timeout = ticks;
        return this;
    }

    /**
     * Sets the maximum number of times this handler can execute.
     * 
     * @param max the maximum execution count
     * @return this builder for method chaining
     */
    public EventBuilder<T> maxExecutions(int max) {
        this.maxExecutions = max;
        return this;
    }

    /**
     * Configures this listener to execute only once.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> once() {
        this.onlyOnce = true;
        return this;
    }

    // === PRIORITY SHORTCUTS ===

    /**
     * Sets the priority to LOWEST.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> lowest() {
        return priority(EventPriority.LOWEST);
    }

    /**
     * Sets the priority to LOW.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> low() {
        return priority(EventPriority.LOW);
    }

    /**
     * Sets the priority to NORMAL (default).
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> normal() {
        return priority(EventPriority.NORMAL);
    }

    /**
     * Sets the priority to HIGH.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> high() {
        return priority(EventPriority.HIGH);
    }

    /**
     * Sets the priority to HIGHEST.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> highest() {
        return priority(EventPriority.HIGHEST);
    }

    /**
     * Sets the priority to MONITOR.
     * 
     * @return this builder for method chaining
     */
    public EventBuilder<T> monitor() {
        return priority(EventPriority.MONITOR);
    }

    // === REGISTRATION ===

    /**
     * Registers the event listener with the configured settings.
     * 
     * @return the registered event handler instance
     * @throws IllegalStateException if event class or handler is not set
     */
    public EventManager.RegisteredEventHandler register() {
        if (eventClass == null || handler == null) {
            throw new IllegalStateException("Event class and handler must be set");
        }

        Predicate<EventContext<T>> combinedFilter = ctx -> {
            for (Predicate<EventContext<T>> filter : filters) {
                if (!filter.test(ctx)) {
                    return false;
                }
            }
            return true;
        };

        Consumer<EventContext<T>> wrappedHandler = createWrappedHandler();

        if (name != null) {
            return EventManager.registerNamed(name, eventClass, wrappedHandler, combinedFilter, priority, ignoreCancelled);
        } else {
            return EventManager.register(eventClass, wrappedHandler, combinedFilter, priority, ignoreCancelled);
        }
    }

    /**
     * Internal method to create wrapped handlers for execution limits and timeouts.
     */
    private Consumer<EventContext<T>> createWrappedHandler() {
        Consumer<EventContext<T>> wrappedHandler = handler;

        if (maxExecutions > 0 || onlyOnce) {
            final int maxExec = onlyOnce ? 1 : maxExecutions;
            wrappedHandler = new ExecutionCountingHandler(wrappedHandler, maxExec);
        }

        if (timeout > 0) {
            wrappedHandler = new TimeoutHandler(wrappedHandler, timeout);
        }

        return wrappedHandler;
    }

    /**
     * Handler wrapper that limits execution count and auto-unregisters when limit is reached.
     */
    private class ExecutionCountingHandler implements Consumer<EventContext<T>> {
        private final Consumer<EventContext<T>> delegate;
        private final int maxExecutions;
        private int executions = 0;
        private EventManager.RegisteredEventHandler registeredHandler;

        public ExecutionCountingHandler(Consumer<EventContext<T>> delegate, int maxExecutions) {
            this.delegate = delegate;
            this.maxExecutions = maxExecutions;
        }

        @Override
        public void accept(EventContext<T> context) {
            executions++;
            delegate.accept(context);

            if (executions >= maxExecutions && registeredHandler != null) {
                EventManager.unregister(registeredHandler);
            }
        }

        public void setRegisteredHandler(EventManager.RegisteredEventHandler handler) {
            this.registeredHandler = handler;
        }
    }

    /**
     * Handler wrapper that auto-unregisters after a timeout period.
     */
    private class TimeoutHandler implements Consumer<EventContext<T>> {
        private final Consumer<EventContext<T>> delegate;
        private final long timeoutTime;
        private EventManager.RegisteredEventHandler registeredHandler;

        public TimeoutHandler(Consumer<EventContext<T>> delegate, long timeoutTicks) {
            this.delegate = delegate;
            this.timeoutTime = System.currentTimeMillis() + (timeoutTicks * 50);
        }

        @Override
        public void accept(EventContext<T> context) {
            if (System.currentTimeMillis() > timeoutTime) {
                if (registeredHandler != null) {
                    EventManager.unregister(registeredHandler);
                }
                return;
            }

            delegate.accept(context);
        }

        public void setRegisteredHandler(EventManager.RegisteredEventHandler handler) {
            this.registeredHandler = handler;
        }
    }

    // === STATIC FACTORY METHODS ===

    /**
     * Creates a new EventBuilder for the specified event class.
     * 
     * @param <T> the event type
     * @param eventClass the event class to listen for
     * @return a new EventBuilder instance
     */
    public static <T extends Event> EventBuilder<T> listen(Class<T> eventClass) {
        return new EventBuilder<>(eventClass);
    }

    /**
     * Alias for listen() - creates a new EventBuilder for the specified event class.
     * 
     * @param <T> the event type
     * @param eventClass the event class to listen for
     * @return a new EventBuilder instance
     */
    public static <T extends Event> EventBuilder<T> on(Class<T> eventClass) {
        return new EventBuilder<>(eventClass);
    }
}
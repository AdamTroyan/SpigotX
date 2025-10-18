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

    public EventBuilder(Class<T> eventClass) {
        this.eventClass = eventClass;
    }

    public EventBuilder<T> handle(Consumer<EventContext<T>> handler) {
        this.handler = handler;
        return this;
    }

    public EventBuilder<T> handleEvent(Consumer<T> handler) {
        this.handler = ctx -> handler.accept(ctx.getEvent());
        return this;
    }

    public EventBuilder<T> handlePlayer(Consumer<Player> handler) {
        this.handler = ctx -> {
            Player player = ctx.getPlayer();
            if (player != null) {
                handler.accept(player);
            }
        };
        return this;
    }

    public EventBuilder<T> filter(Predicate<EventContext<T>> filter) {
        if (filter != null) this.filters.add(filter);
        return this;
    }

    public EventBuilder<T> filterEvent(Predicate<T> filter) {
        if (filter != null) this.filters.add(ctx -> filter.test(ctx.getEvent()));
        return this;
    }

    public EventBuilder<T> filterPlayer(Predicate<Player> filter) {
        if (filter != null) this.filters.add(ctx -> {
            Player player = ctx.getPlayer();
            return player != null && filter.test(player);
        });
        return this;
    }

    public EventBuilder<T> playersOnly() {
        return filter(ctx -> ctx.getPlayer() != null);
    }

    public EventBuilder<T> playerHasPermission(String permission) {
        return filterPlayer(player -> player.hasPermission(permission));
    }

    public EventBuilder<T> playerInWorld(String worldName) {
        return filterPlayer(player -> player.getWorld().getName().equals(worldName));
    }

    public EventBuilder<T> onlyIfNotCancelled() {
        return filter(ctx -> {
            T event = ctx.getEvent();
            if (event instanceof Cancellable) {
                return !((Cancellable) event).isCancelled();
            }
            return true;
        });
    }

    public EventBuilder<T> onlyIfCancelled() {
        return filter(ctx -> {
            T event = ctx.getEvent();
            if (event instanceof Cancellable) {
                return ((Cancellable) event).isCancelled();
            }
            return false;
        });
    }

    public EventBuilder<T> priority(EventPriority priority) {
        if (priority != null) this.priority = priority;
        return this;
    }

    public EventBuilder<T> ignoreCancelled(boolean ignore) {
        this.ignoreCancelled = ignore;
        return this;
    }

    public EventBuilder<T> ignoreCancelled() {
        return ignoreCancelled(true);
    }

    public EventBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    public EventBuilder<T> timeout(long ticks) {
        this.timeout = ticks;
        return this;
    }

    public EventBuilder<T> maxExecutions(int max) {
        this.maxExecutions = max;
        return this;
    }

    public EventBuilder<T> once() {
        this.onlyOnce = true;
        return this;
    }

    public EventBuilder<T> lowest() {
        return priority(EventPriority.LOWEST);
    }

    public EventBuilder<T> low() {
        return priority(EventPriority.LOW);
    }

    public EventBuilder<T> normal() {
        return priority(EventPriority.NORMAL);
    }

    public EventBuilder<T> high() {
        return priority(EventPriority.HIGH);
    }

    public EventBuilder<T> highest() {
        return priority(EventPriority.HIGHEST);
    }

    public EventBuilder<T> monitor() {
        return priority(EventPriority.MONITOR);
    }

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

    public static <T extends Event> EventBuilder<T> listen(Class<T> eventClass) {
        return new EventBuilder<>(eventClass);
    }

    public static <T extends Event> EventBuilder<T> on(Class<T> eventClass) {
        return new EventBuilder<>(eventClass);
    }
}
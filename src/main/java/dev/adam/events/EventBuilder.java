package dev.adam.events;

import dev.adam.events.context.EventContext;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EventBuilder<T extends Event> {
    private final Class<T> eventClass;
    private Consumer<EventContext<T>> handler;
    private Predicate<EventContext<T>> filter = ctx -> true;
    private EventPriority priority = EventPriority.NORMAL;
    private boolean ignoreCancelled = false;

    public EventBuilder(Class<T> eventClass) {
        this.eventClass = eventClass;
    }

    public EventBuilder<T> handle(Consumer<EventContext<T>> handler) {
        this.handler = handler;
        return this;
    }

    public EventBuilder<T> filter(Predicate<EventContext<T>> filter) {
        this.filter = filter;
        return this;
    }

    public EventBuilder<T> priority(EventPriority priority) {
        this.priority = priority;
        return this;
    }

    public EventBuilder<T> ignoreCancelled(boolean ignore) {
        this.ignoreCancelled = ignore;
        return this;
    }

    public void register() {
        EventManager.register(eventClass, handler, filter, priority, ignoreCancelled);
    }
}
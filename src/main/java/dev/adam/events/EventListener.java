package dev.adam.events;

@FunctionalInterface
public interface EventListener<T extends org.bukkit.event.Event> {
    void handle(EventContext<T> ctx);
}
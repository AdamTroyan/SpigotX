package dev.adam.events.context;

import org.bukkit.event.Event;
import org.bukkit.entity.Player;

public class EventContext<T extends Event> {
    private final T event;

    public EventContext(T event) {
        this.event = event;
    }

    public T getEvent() { return event; }

    public Player getPlayer() {
        try {
            Object obj = event.getClass().getMethod("getPlayer").invoke(event);
            return (obj instanceof Player) ? (Player) obj : null;
        } catch (Exception ignored) { return null; }
    }
}
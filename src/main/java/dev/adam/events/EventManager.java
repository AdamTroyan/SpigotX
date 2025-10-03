package dev.adam.events;

import dev.adam.SpigotX;
import dev.adam.events.context.EventContext;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class EventManager {
    public static <T extends Event> void register(
            Class<T> eventClass,
            Consumer<EventContext<T>> handler,
            Predicate<EventContext<T>> filter,
            EventPriority priority,
            boolean ignoreCancelled
    ) {
        Plugin plugin = SpigotX.getPlugin();
        Listener listener = new Listener() {};
        plugin.getServer().getPluginManager().registerEvent(
                eventClass,
                listener,
                priority,
                (l, event) -> {
                    if (eventClass.isInstance(event)) {
                        EventContext<T> ctx = new EventContext<>(eventClass.cast(event));
                        if (filter.test(ctx)) handler.accept(ctx);
                    }
                },
                plugin,
                ignoreCancelled
        );
    }
}
package dev.adam.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public class EventUtil {
    public static <T extends Event> void listen(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        if (plugin == null || eventClass == null || handler == null) return;
        Bukkit.getPluginManager().registerEvent(
                eventClass,
                new Listener() {},
                EventPriority.NORMAL,
                (listener, event) -> {
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
    }
}
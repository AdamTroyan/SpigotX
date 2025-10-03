package dev.adam.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public class EventUtil {
    public static <T extends Event> void listen(Plugin plugin, Class<T> eventClass, Consumer<T> handler) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @org.bukkit.event.EventHandler(priority = EventPriority.NORMAL)

            public void onEvent(Event event) {
                if (eventClass.isInstance(event)) {
                    handler.accept(eventClass.cast(event));
                }
            }
            
        }, plugin);
    }
}
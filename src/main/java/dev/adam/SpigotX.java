package dev.adam;

import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import dev.adam.commands.CommandManager;
import dev.adam.events.EventBuilder;
import dev.adam.events.context.EventContext;
import dev.adam.gui.GUIListener;
import java.util.function.Consumer;

public class SpigotX {
    private static Plugin plugin;
    private static boolean guiListenerRegistered = false;

    public static void init(Plugin pl) {
        plugin = pl;
        dev.adam.logging.Logger.info("SpigotX initialized!");
        
        if (!guiListenerRegistered) {
            pl.getServer().getPluginManager().registerEvents(new GUIListener(), pl);
            guiListenerRegistered = true;
        }
    }

    public static Plugin getPlugin() {
        if (plugin == null) throw new IllegalStateException("SpigotX.init(plugin) must be called first!");
        return plugin;
    }

    public static void registerCommand(Object commandsInstance) {
        if (plugin == null)
            throw new IllegalStateException("SpigotX.init(plugin) must be called before register!");
        new CommandManager(plugin, commandsInstance);
    }

    public static <T extends Event> void on(Class<T> eventClass, Consumer<EventContext<T>> handler) {
        new EventBuilder<>(eventClass)
            .handle(handler)
            .register();
    }
}
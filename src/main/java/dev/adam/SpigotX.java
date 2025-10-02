package dev.adam;

import org.bukkit.plugin.Plugin;

public class SpigotX {

    private static final String VERSION = "1.0.4";
    private static Plugin pluginInstance;

    private SpigotX() {

    }

    public static void init(Plugin plugin) {
        pluginInstance = plugin;
        dev.adam.events.Events.init(plugin);
    }

    public static String getVersion() {
        return VERSION;
    }

    public static Plugin getPlugin() {
        if (pluginInstance == null) {
            throw new IllegalStateException("SpigotX.init(plugin) must be called first!");
        }

        return pluginInstance;
    }

    public static void registerCommands(Object executor) {
        if (pluginInstance == null) {
            throw new IllegalStateException("SpigotX.init(plugin) must be called first!");
        }

        new dev.adam.commands.CommandManager((org.bukkit.plugin.java.JavaPlugin) pluginInstance, executor);
    }
}

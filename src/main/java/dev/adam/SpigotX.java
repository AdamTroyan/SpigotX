package dev.adam;

import org.bukkit.plugin.Plugin;

import dev.adam.commands.CommandManager;

public class SpigotX {
    private static Plugin plugin;

    public static void init(Plugin pl) {
        plugin = pl;
        dev.adam.logging.Logger.info("SpigotX initialized!");
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
}
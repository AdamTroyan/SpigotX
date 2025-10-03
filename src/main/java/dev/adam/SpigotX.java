package dev.adam;

import org.bukkit.plugin.Plugin;

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
}
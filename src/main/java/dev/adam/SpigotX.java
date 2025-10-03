package dev.adam;

import org.bukkit.plugin.Plugin;

import dev.adam.commands.CommandManager;
import dev.adam.gui.GUIListener;

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
}
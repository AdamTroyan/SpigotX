package dev.adam.logging;

import org.bukkit.Bukkit;

public class Logger {
    public static void info(String msg) {
        Bukkit.getLogger().info("[SpigotX] " + msg);
    }
    public static void warn(String msg) {
        Bukkit.getLogger().warning("[SpigotX] " + msg);
    }
    public static void error(String msg) {
        Bukkit.getLogger().severe("[SpigotX] " + msg);
    }
    public static void debug(String msg) {
        if (System.getProperty("spigotx.debug", "false").equals("true"))
            Bukkit.getLogger().info("[SpigotX][DEBUG] " + msg);
    }
}
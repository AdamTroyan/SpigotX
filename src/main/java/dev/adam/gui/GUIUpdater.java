package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GUIUpdater {
    private static final Map<GUI, BukkitTask> tasks = new HashMap<>();

    public static void scheduleRepeating(Plugin plugin, GUI gui, long period, Consumer<GUI> update) {
        cancel(gui);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> update.accept(gui), period, period);
        tasks.put(gui, task);
    }

    public static void cancel(GUI gui) {
        BukkitTask task = tasks.remove(gui);
        if (task != null) task.cancel();
    }
}
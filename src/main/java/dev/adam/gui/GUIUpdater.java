package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GUIUpdater {
    private static final Map<GUI, BukkitTask> tasks = new ConcurrentHashMap<>();

    public static void scheduleRepeating(Plugin plugin, GUI gui, long period, Consumer<GUI> update) {
        cancel(gui);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (gui.getInventory().getViewers().isEmpty()) {
                cancel(gui);
                return;
            }
            update.accept(gui);
        }, period, period);

        tasks.put(gui, task);
    }

    public static void cancel(GUI gui) {
        BukkitTask task = tasks.remove(gui);
        if (task != null) task.cancel();
    }

    public static void cancelAll() {
        for (BukkitTask task : tasks.values()) task.cancel();
        
        tasks.clear();
    }
}
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
        if (plugin == null || gui == null || update == null) return;
        cancel(gui);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (gui.getInventory().getViewers().isEmpty()) {
                cancel(gui);
                return;
            }
            try {
                update.accept(gui);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
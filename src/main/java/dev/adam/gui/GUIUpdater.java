package dev.adam.gui;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class GUIUpdater {

    private static final Map<GUI, Integer> tasks = new ConcurrentHashMap<>();

    public static void scheduleRepeating(Plugin plugin, GUI gui, long periodTicks, Consumer<GUI> task) {
        if (gui == null || plugin == null || task == null) return;

        cancel(gui);

        int id = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                if (!gui.getInventory().getViewers().isEmpty()) {
                    task.accept(gui);
                } else {
                    cancel(gui);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, 0L, periodTicks).getTaskId();

        tasks.put(gui, id);
    }

    public static void cancel(GUI gui) {
        if (gui == null) return;
        Integer id = tasks.remove(gui);
        if (id != null) Bukkit.getScheduler().cancelTask(id);
    }

    public static void cancelAll() {
        tasks.keySet().forEach(GUIUpdater::cancel);
    }
}
